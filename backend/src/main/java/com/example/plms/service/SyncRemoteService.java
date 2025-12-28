package com.example.plms.service;

import com.example.plms.config.SyncProperties;
import com.example.plms.web.dto.sync.SyncDelete;
import com.example.plms.web.dto.sync.SyncExternalLink;
import com.example.plms.web.dto.sync.SyncItem;
import com.example.plms.web.dto.sync.SyncList;
import com.example.plms.web.dto.sync.SyncListItem;
import com.example.plms.web.dto.sync.SyncLoan;
import com.example.plms.web.dto.sync.SyncPayload;
import com.example.plms.web.dto.sync.SyncProgressLog;
import com.example.plms.web.dto.sync.SyncRequest;
import com.example.plms.web.dto.sync.SyncResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SyncRemoteService {
    private static final int STORE_ID = 1;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SyncProperties properties;

    public SyncRemoteService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, SyncProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public SyncResponse handle(SyncRequest request, String apiKey) {
        validateApiKey(apiKey);
        SyncPayload store = loadStore();
        Map<Long, SyncItem> items = toItemMap(store.items());
        Map<Long, SyncList> lists = toListMap(store.lists());
        Map<String, SyncListItem> listItems = toListItemMap(store.listItems());
        Map<Long, SyncProgressLog> progressLogs = toProgressMap(store.progressLogs());
        Map<Long, SyncLoan> loans = toLoanMap(store.loans());
        Map<Long, SyncExternalLink> externalLinks = toExternalLinkMap(store.externalLinks());
        Map<String, SyncDelete> tombstones = toDeleteMap(store.deletes());

        int conflicts = applyChanges(request.changes(), items, lists, listItems, progressLogs, loans, externalLinks, tombstones);

        SyncPayload updatedStore = new SyncPayload(
            false,
            new ArrayList<>(items.values()),
            new ArrayList<>(lists.values()),
            new ArrayList<>(listItems.values()),
            new ArrayList<>(progressLogs.values()),
            new ArrayList<>(loans.values()),
            new ArrayList<>(externalLinks.values()),
            new ArrayList<>(tombstones.values())
        );
        saveStore(updatedStore);

        SyncPayload delta = buildDelta(updatedStore, request.lastSyncAt());
        return new SyncResponse(OffsetDateTime.now(ZoneOffset.UTC), delta, conflicts);
    }

    private void validateApiKey(String apiKey) {
        String expected = properties.getApiKey();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sync API key not configured");
        }
        if (apiKey == null || !expected.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid sync API key");
        }
    }

    private SyncPayload loadStore() {
        try {
            String json = jdbcTemplate.queryForObject(
                "SELECT payload::text FROM sync_remote_store WHERE id = ?",
                String.class,
                STORE_ID
            );
            if (json == null || json.isBlank()) {
                return emptyPayload();
            }
            return objectMapper.readValue(json, SyncPayload.class);
        } catch (EmptyResultDataAccessException ex) {
            SyncPayload empty = emptyPayload();
            saveStore(empty);
            return empty;
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sync store corrupted");
        }
    }

    private void saveStore(SyncPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            jdbcTemplate.update("""
                INSERT INTO sync_remote_store (id, payload, updated_at)
                VALUES (?, ?::jsonb, ?)
                ON CONFLICT (id) DO UPDATE SET payload = EXCLUDED.payload, updated_at = EXCLUDED.updated_at
                """,
                STORE_ID,
                json,
                OffsetDateTime.now(ZoneOffset.UTC)
            );
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sync store write failed");
        }
    }

    private int applyChanges(SyncPayload incoming,
                             Map<Long, SyncItem> items,
                             Map<Long, SyncList> lists,
                             Map<String, SyncListItem> listItems,
                             Map<Long, SyncProgressLog> progressLogs,
                             Map<Long, SyncLoan> loans,
                             Map<Long, SyncExternalLink> externalLinks,
                             Map<String, SyncDelete> tombstones) {
        if (incoming == null) {
            return 0;
        }
        int conflicts = 0;
        conflicts += mergeItems(items, tombstones, incoming.items());
        conflicts += mergeLists(lists, tombstones, incoming.lists());
        conflicts += mergeListItems(listItems, tombstones, incoming.listItems());
        conflicts += mergeProgress(progressLogs, tombstones, incoming.progressLogs());
        conflicts += mergeLoans(loans, tombstones, incoming.loans());
        conflicts += mergeExternalLinks(externalLinks, tombstones, incoming.externalLinks());
        conflicts += applyDeletes(incoming.deletes(), lists, listItems, tombstones);
        return conflicts;
    }

    private int mergeItems(Map<Long, SyncItem> items, Map<String, SyncDelete> tombstones, List<SyncItem> incoming) {
        if (incoming == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncItem item : incoming) {
            if (item == null || item.id() == null || item.updatedAt() == null) {
                continue;
            }
            if (isTombstoned(tombstones, "ITEM", item.id().toString(), item.updatedAt())) {
                conflicts++;
                continue;
            }
            SyncItem existing = items.get(item.id());
            if (existing == null || item.updatedAt().isAfter(existing.updatedAt())) {
                items.put(item.id(), item);
            } else {
                conflicts++;
            }
        }
        return conflicts;
    }

    private int mergeLists(Map<Long, SyncList> lists, Map<String, SyncDelete> tombstones, List<SyncList> incoming) {
        if (incoming == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncList list : incoming) {
            if (list == null || list.id() == null || list.updatedAt() == null) {
                continue;
            }
            if (isTombstoned(tombstones, "LIST", list.id().toString(), list.updatedAt())) {
                conflicts++;
                continue;
            }
            SyncList existing = lists.get(list.id());
            if (existing == null || list.updatedAt().isAfter(existing.updatedAt())) {
                lists.put(list.id(), list);
            } else {
                conflicts++;
            }
        }
        return conflicts;
    }

    private int mergeListItems(Map<String, SyncListItem> listItems, Map<String, SyncDelete> tombstones, List<SyncListItem> incoming) {
        if (incoming == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncListItem listItem : incoming) {
            if (listItem == null || listItem.listId() == null || listItem.itemId() == null || listItem.updatedAt() == null) {
                continue;
            }
            String key = listItem.listId() + ":" + listItem.itemId();
            if (isTombstoned(tombstones, "LIST_ITEM", key, listItem.updatedAt())) {
                conflicts++;
                continue;
            }
            SyncListItem existing = listItems.get(key);
            if (existing == null || listItem.updatedAt().isAfter(existing.updatedAt())) {
                listItems.put(key, listItem);
            } else {
                conflicts++;
            }
        }
        return conflicts;
    }

    private int mergeProgress(Map<Long, SyncProgressLog> logs, Map<String, SyncDelete> tombstones, List<SyncProgressLog> incoming) {
        if (incoming == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncProgressLog log : incoming) {
            if (log == null || log.id() == null || log.updatedAt() == null) {
                continue;
            }
            if (isTombstoned(tombstones, "PROGRESS_LOG", log.id().toString(), log.updatedAt())) {
                conflicts++;
                continue;
            }
            SyncProgressLog existing = logs.get(log.id());
            if (existing == null || log.updatedAt().isAfter(existing.updatedAt())) {
                logs.put(log.id(), log);
            } else {
                conflicts++;
            }
        }
        return conflicts;
    }

    private int mergeLoans(Map<Long, SyncLoan> loans, Map<String, SyncDelete> tombstones, List<SyncLoan> incoming) {
        if (incoming == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncLoan loan : incoming) {
            if (loan == null || loan.id() == null || loan.updatedAt() == null) {
                continue;
            }
            if (isTombstoned(tombstones, "LOAN", loan.id().toString(), loan.updatedAt())) {
                conflicts++;
                continue;
            }
            SyncLoan existing = loans.get(loan.id());
            if (existing == null || loan.updatedAt().isAfter(existing.updatedAt())) {
                loans.put(loan.id(), loan);
            } else {
                conflicts++;
            }
        }
        return conflicts;
    }

    private int mergeExternalLinks(Map<Long, SyncExternalLink> links, Map<String, SyncDelete> tombstones, List<SyncExternalLink> incoming) {
        if (incoming == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncExternalLink link : incoming) {
            if (link == null || link.id() == null || link.updatedAt() == null) {
                continue;
            }
            if (isTombstoned(tombstones, "EXTERNAL_LINK", link.id().toString(), link.updatedAt())) {
                conflicts++;
                continue;
            }
            SyncExternalLink existing = links.get(link.id());
            if (existing == null || link.updatedAt().isAfter(existing.updatedAt())) {
                links.put(link.id(), link);
            } else {
                conflicts++;
            }
        }
        return conflicts;
    }

    private int applyDeletes(List<SyncDelete> deletes,
                             Map<Long, SyncList> lists,
                             Map<String, SyncListItem> listItems,
                             Map<String, SyncDelete> tombstones) {
        if (deletes == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncDelete delete : deletes) {
            if (delete == null || delete.entityType() == null || delete.entityKey() == null || delete.deletedAt() == null) {
                continue;
            }
            String tombstoneKey = tombstoneKey(delete.entityType(), delete.entityKey());
            SyncDelete existingTombstone = tombstones.get(tombstoneKey);
            if (existingTombstone != null && existingTombstone.deletedAt().isAfter(delete.deletedAt())) {
                continue;
            }
            if ("LIST".equalsIgnoreCase(delete.entityType())) {
                SyncList list = lists.get(Long.valueOf(delete.entityKey()));
                if (list != null && list.updatedAt() != null && list.updatedAt().isAfter(delete.deletedAt())) {
                    conflicts++;
                    continue;
                }
                lists.remove(Long.valueOf(delete.entityKey()));
                removeListItems(listItems, delete.entityKey(), delete.deletedAt(), tombstones);
            }
            if ("LIST_ITEM".equalsIgnoreCase(delete.entityType())) {
                SyncListItem item = listItems.get(delete.entityKey());
                if (item != null && item.updatedAt() != null && item.updatedAt().isAfter(delete.deletedAt())) {
                    conflicts++;
                    continue;
                }
                listItems.remove(delete.entityKey());
            }
            tombstones.put(tombstoneKey, delete);
        }
        return conflicts;
    }

    private void removeListItems(Map<String, SyncListItem> listItems, String listId, OffsetDateTime deletedAt,
                                 Map<String, SyncDelete> tombstones) {
        List<String> keys = new ArrayList<>();
        for (String key : listItems.keySet()) {
            if (key.startsWith(listId + ":")) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            listItems.remove(key);
            tombstones.put(tombstoneKey("LIST_ITEM", key), new SyncDelete("LIST_ITEM", key, deletedAt));
        }
    }

    private boolean isTombstoned(Map<String, SyncDelete> tombstones, String entityType, String entityKey, OffsetDateTime updatedAt) {
        SyncDelete tombstone = tombstones.get(tombstoneKey(entityType, entityKey));
        return tombstone != null && tombstone.deletedAt() != null && tombstone.deletedAt().isAfter(updatedAt);
    }

    private String tombstoneKey(String entityType, String entityKey) {
        return entityType + ":" + entityKey;
    }

    private SyncPayload buildDelta(SyncPayload store, OffsetDateTime since) {
        if (since == null) {
            return store;
        }
        return new SyncPayload(
            false,
            filterByUpdatedAt(store.items(), since),
            filterByUpdatedAt(store.lists(), since),
            filterByUpdatedAt(store.listItems(), since),
            filterByUpdatedAt(store.progressLogs(), since),
            filterByUpdatedAt(store.loans(), since),
            filterByUpdatedAt(store.externalLinks(), since),
            filterDeletes(store.deletes(), since)
        );
    }

    private <T> List<T> filterByUpdatedAt(List<T> items, OffsetDateTime since) {
        if (items == null) {
            return List.of();
        }
        List<T> results = new ArrayList<>();
        for (T item : items) {
            OffsetDateTime updated = extractUpdatedAt(item);
            if (updated != null && updated.isAfter(since)) {
                results.add(item);
            }
        }
        return results;
    }

    private OffsetDateTime extractUpdatedAt(Object item) {
        if (item instanceof SyncItem sync) {
            return sync.updatedAt();
        }
        if (item instanceof SyncList sync) {
            return sync.updatedAt();
        }
        if (item instanceof SyncListItem sync) {
            return sync.updatedAt();
        }
        if (item instanceof SyncProgressLog sync) {
            return sync.updatedAt();
        }
        if (item instanceof SyncLoan sync) {
            return sync.updatedAt();
        }
        if (item instanceof SyncExternalLink sync) {
            return sync.updatedAt();
        }
        return null;
    }

    private List<SyncDelete> filterDeletes(List<SyncDelete> deletes, OffsetDateTime since) {
        if (deletes == null) {
            return List.of();
        }
        List<SyncDelete> results = new ArrayList<>();
        for (SyncDelete delete : deletes) {
            if (delete.deletedAt() != null && delete.deletedAt().isAfter(since)) {
                results.add(delete);
            }
        }
        return results;
    }

    private SyncPayload emptyPayload() {
        return new SyncPayload(false, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private Map<Long, SyncItem> toItemMap(List<SyncItem> items) {
        Map<Long, SyncItem> map = new HashMap<>();
        if (items != null) {
            for (SyncItem item : items) {
                if (item != null && item.id() != null) {
                    map.put(item.id(), item);
                }
            }
        }
        return map;
    }

    private Map<Long, SyncList> toListMap(List<SyncList> lists) {
        Map<Long, SyncList> map = new HashMap<>();
        if (lists != null) {
            for (SyncList list : lists) {
                if (list != null && list.id() != null) {
                    map.put(list.id(), list);
                }
            }
        }
        return map;
    }

    private Map<String, SyncListItem> toListItemMap(List<SyncListItem> listItems) {
        Map<String, SyncListItem> map = new HashMap<>();
        if (listItems != null) {
            for (SyncListItem item : listItems) {
                if (item != null && item.listId() != null && item.itemId() != null) {
                    map.put(item.listId() + ":" + item.itemId(), item);
                }
            }
        }
        return map;
    }

    private Map<Long, SyncProgressLog> toProgressMap(List<SyncProgressLog> logs) {
        Map<Long, SyncProgressLog> map = new HashMap<>();
        if (logs != null) {
            for (SyncProgressLog log : logs) {
                if (log != null && log.id() != null) {
                    map.put(log.id(), log);
                }
            }
        }
        return map;
    }

    private Map<Long, SyncLoan> toLoanMap(List<SyncLoan> loans) {
        Map<Long, SyncLoan> map = new HashMap<>();
        if (loans != null) {
            for (SyncLoan loan : loans) {
                if (loan != null && loan.id() != null) {
                    map.put(loan.id(), loan);
                }
            }
        }
        return map;
    }

    private Map<Long, SyncExternalLink> toExternalLinkMap(List<SyncExternalLink> links) {
        Map<Long, SyncExternalLink> map = new HashMap<>();
        if (links != null) {
            for (SyncExternalLink link : links) {
                if (link != null && link.id() != null) {
                    map.put(link.id(), link);
                }
            }
        }
        return map;
    }

    private Map<String, SyncDelete> toDeleteMap(List<SyncDelete> deletes) {
        Map<String, SyncDelete> map = new HashMap<>();
        if (deletes != null) {
            for (SyncDelete delete : deletes) {
                if (delete != null && delete.entityType() != null && delete.entityKey() != null) {
                    map.put(tombstoneKey(delete.entityType(), delete.entityKey()), delete);
                }
            }
        }
        return map;
    }
}
