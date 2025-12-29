package com.example.plms.service;

import com.example.plms.config.SyncProperties;
import com.example.plms.domain.BookInfo;
import com.example.plms.domain.DvdInfo;
import com.example.plms.domain.ExternalLink;
import com.example.plms.domain.ListItem;
import com.example.plms.domain.Loan;
import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaList;
import com.example.plms.domain.ProgressLog;
import com.example.plms.domain.SyncOutboxEntry;
import com.example.plms.domain.SyncState;
import com.example.plms.domain.Tag;
import com.example.plms.repository.ExternalLinkRepository;
import com.example.plms.repository.ListItemRepository;
import com.example.plms.repository.LoanRepository;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.repository.MediaListRepository;
import com.example.plms.repository.ProgressLogRepository;
import com.example.plms.repository.SyncOutboxRepository;
import com.example.plms.repository.SyncStateRepository;
import com.example.plms.web.dto.SyncStatusResponse;
import com.example.plms.web.dto.sync.SyncBookInfo;
import com.example.plms.web.dto.sync.SyncDelete;
import com.example.plms.web.dto.sync.SyncDvdInfo;
import com.example.plms.web.dto.sync.SyncExternalLink;
import com.example.plms.web.dto.sync.SyncItem;
import com.example.plms.web.dto.sync.SyncList;
import com.example.plms.web.dto.sync.SyncListItem;
import com.example.plms.web.dto.sync.SyncLoan;
import com.example.plms.web.dto.sync.SyncPayload;
import com.example.plms.web.dto.sync.SyncProgressLog;
import com.example.plms.web.dto.sync.SyncRequest;
import com.example.plms.web.dto.sync.SyncResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class SyncService {
    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final SyncProperties properties;
    private final SyncStateRepository syncStateRepository;
    private final MediaItemRepository itemRepository;
    private final MediaListRepository listRepository;
    private final ListItemRepository listItemRepository;
    private final ProgressLogRepository progressLogRepository;
    private final LoanRepository loanRepository;
    private final ExternalLinkRepository externalLinkRepository;
    private final SyncOutboxRepository syncOutboxRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    public SyncService(SyncProperties properties, SyncStateRepository syncStateRepository,
                       MediaItemRepository itemRepository,
                       MediaListRepository listRepository,
                       ListItemRepository listItemRepository,
                       ProgressLogRepository progressLogRepository,
                       LoanRepository loanRepository,
                       ExternalLinkRepository externalLinkRepository,
                       SyncOutboxRepository syncOutboxRepository,
                       JdbcTemplate jdbcTemplate,
                       RestTemplate restTemplate) {
        this.properties = properties;
        this.syncStateRepository = syncStateRepository;
        this.itemRepository = itemRepository;
        this.listRepository = listRepository;
        this.listItemRepository = listItemRepository;
        this.progressLogRepository = progressLogRepository;
        this.loanRepository = loanRepository;
        this.externalLinkRepository = externalLinkRepository;
        this.syncOutboxRepository = syncOutboxRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public SyncStatusResponse enable(Long userId, boolean enabled) {
        properties.setEnabled(enabled);
        SyncState state = getOrCreateState(userId);
        state.setLastStatus(enabled ? "enabled" : "disabled");
        syncStateRepository.save(state);
        return status(userId);
    }

    @Transactional
    public SyncStatusResponse runSync(Long userId) {
        SyncState state = getOrCreateState(userId);
        if (!properties.isEnabled()) {
            state.setLastStatus("disabled");
            syncStateRepository.save(state);
            return toResponse(state);
        }
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            state.setLastStatus("missing-endpoint");
            syncStateRepository.save(state);
            return toResponse(state);
        }
        if (state.getClientId() == null || state.getClientId().isBlank()) {
            state.setClientId(UUID.randomUUID().toString());
        }
        boolean fullSync = state.getLastSyncAt() == null || state.isNeedsFullSync();
        OffsetDateTime since = fullSync ? null : state.getLastSyncAt();
        SyncPayload payload = fullSync ? buildFullPayload(userId) : buildIncrementalPayload(userId, since);
        SyncRequest request = new SyncRequest(state.getClientId(), since, payload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.set("X-API-Key", properties.getApiKey());
        }

        try {
            ResponseEntity<SyncResponse> response = restTemplate.postForEntity(
                properties.getEndpoint(),
                new HttpEntity<>(request, headers),
                SyncResponse.class
            );
            SyncResponse syncResponse = response.getBody();
            int remoteConflicts = syncResponse == null ? 0 : syncResponse.conflictCount();
            int localConflicts = syncResponse == null ? 0 : applyRemoteChanges(userId, syncResponse.changes());
            if (syncResponse != null && syncResponse.changes() != null) {
                updateSequences();
            }
            state.setLastConflictCount(remoteConflicts + localConflicts);
            state.setLastStatus(state.getLastConflictCount() > 0 ? "conflicts" : "success");
            state.setLastSyncAt(syncResponse != null && syncResponse.serverTime() != null
                ? syncResponse.serverTime()
                : OffsetDateTime.now(ZoneOffset.UTC));
            state.setNeedsFullSync(false);
            clearOutbox(userId, fullSync, state.getLastSyncAt());
            syncStateRepository.save(state);
            return toResponse(state);
        } catch (RestClientException ex) {
            log.warn("Sync failed: {}", ex.getMessage());
            state.setLastStatus("error");
            syncStateRepository.save(state);
            return toResponse(state);
        }
    }

    @Transactional(readOnly = true)
    public SyncStatusResponse status(Long userId) {
        SyncState state = getOrCreateState(userId);
        return toResponse(state);
    }

    public void flushAllUsers() {
        if (!properties.isEnabled()) {
            return;
        }
        List<SyncState> states = syncStateRepository.findByUserIdIsNotNull();
        for (SyncState state : states) {
            try {
                runSync(state.getUserId());
            } catch (Exception ex) {
                log.warn("Sync flush failed for user {}: {}", state.getUserId(), ex.getMessage());
            }
        }
    }

    private int applyRemoteChanges(Long userId, SyncPayload payload) {
        if (payload == null) {
            return 0;
        }
        int conflicts = 0;
        conflicts += applyItems(userId, payload.items());
        conflicts += applyLists(userId, payload.lists());
        conflicts += applyListItems(userId, payload.listItems());
        conflicts += applyProgress(userId, payload.progressLogs());
        conflicts += applyLoans(userId, payload.loans());
        conflicts += applyExternalLinks(userId, payload.externalLinks());
        conflicts += applyDeletes(userId, payload.deletes());
        return conflicts;
    }

    private SyncState getOrCreateState(Long userId) {
        return syncStateRepository.findByUserId(userId)
            .orElseGet(() -> {
                SyncState created = new SyncState();
                created.setUserId(userId);
                return syncStateRepository.save(created);
            });
    }

    private SyncStatusResponse toResponse(SyncState state) {
        return new SyncStatusResponse(properties.isEnabled(), state.getLastSyncAt(), state.getLastStatus(), state.getLastConflictCount());
    }

    private SyncPayload buildFullPayload(Long userId) {
        List<MediaItem> ownedItems = itemRepository.findAllByOwner_Id(userId);
        List<Long> itemIds = ownedItems.stream().map(MediaItem::getId).toList();
        List<MediaList> ownedLists = listRepository.findAllByOwner_Id(userId);
        List<Long> listIds = ownedLists.stream().map(MediaList::getId).toList();
        return new SyncPayload(
            true,
            ownedItems.stream().map(this::toSyncItem).toList(),
            ownedLists.stream().map(this::toSyncList).toList(),
            listIds.isEmpty() ? List.of() : listItemRepository.findByIdListIdIn(listIds).stream().map(this::toSyncListItem).toList(),
            itemIds.isEmpty() ? List.of() : progressLogRepository.findByItemIdIn(itemIds).stream().map(this::toSyncProgressLog).toList(),
            itemIds.isEmpty() ? List.of() : loanRepository.findByItemIdIn(itemIds).stream().map(this::toSyncLoan).toList(),
            itemIds.isEmpty() ? List.of() : externalLinkRepository.findByItemIdIn(itemIds).stream().map(this::toSyncExternalLink).toList(),
            List.of()
        );
    }

    private SyncPayload buildIncrementalPayload(Long userId, OffsetDateTime since) {
        List<MediaItem> ownedItems = itemRepository.findAllByOwner_Id(userId);
        List<Long> itemIds = ownedItems.stream().map(MediaItem::getId).toList();
        List<MediaList> ownedLists = listRepository.findAllByOwner_Id(userId);
        List<Long> listIds = ownedLists.stream().map(MediaList::getId).toList();
        return new SyncPayload(
            false,
            filterByUpdatedAt(ownedItems.stream().map(this::toSyncItem).toList(), since),
            filterByUpdatedAt(ownedLists.stream().map(this::toSyncList).toList(), since),
            filterByUpdatedAt(listIds.isEmpty() ? List.of() : listItemRepository.findByIdListIdIn(listIds).stream()
                .map(this::toSyncListItem).toList(), since),
            filterByUpdatedAt(itemIds.isEmpty() ? List.of() : progressLogRepository.findByItemIdIn(itemIds).stream()
                .map(this::toSyncProgressLog).toList(), since),
            filterByUpdatedAt(itemIds.isEmpty() ? List.of() : loanRepository.findByItemIdIn(itemIds).stream()
                .map(this::toSyncLoan).toList(), since),
            filterByUpdatedAt(itemIds.isEmpty() ? List.of() : externalLinkRepository.findByItemIdIn(itemIds).stream()
                .map(this::toSyncExternalLink).toList(), since),
            loadDeletes(userId, since)
        );
    }

    private List<SyncDelete> loadDeletes(Long userId, OffsetDateTime since) {
        if (since == null) {
            return List.of();
        }
        List<SyncOutboxEntry> entries = syncOutboxRepository.findByUserIdAndQueuedAtAfterOrderByQueuedAtAsc(userId, since);
        Map<String, SyncDelete> deduped = new LinkedHashMap<>();
        for (SyncOutboxEntry entry : entries) {
            String key = entry.getEntityType() + ":" + entry.getEntityKey();
            SyncDelete existing = deduped.get(key);
            if (existing == null || entry.getQueuedAt().isAfter(existing.deletedAt())) {
                deduped.put(key, new SyncDelete(entry.getEntityType(), entry.getEntityKey(), entry.getQueuedAt()));
            }
        }
        return new ArrayList<>(deduped.values());
    }

    private void clearOutbox(Long userId, boolean fullSync, OffsetDateTime syncedAt) {
        if (fullSync) {
            syncOutboxRepository.deleteAll(syncOutboxRepository.findByUserId(userId));
            return;
        }
        List<SyncOutboxEntry> entries = syncOutboxRepository.findByUserId(userId);
        List<SyncOutboxEntry> toDelete = new ArrayList<>();
        for (SyncOutboxEntry entry : entries) {
            if (entry.getQueuedAt().isBefore(syncedAt) || entry.getQueuedAt().isEqual(syncedAt)) {
                toDelete.add(entry);
            }
        }
        syncOutboxRepository.deleteAll(toDelete);
    }

    private SyncItem toSyncItem(MediaItem item) {
        SyncBookInfo bookInfo = null;
        if (item.getBookInfo() != null) {
            BookInfo info = item.getBookInfo();
            bookInfo = new SyncBookInfo(info.getIsbn(), info.getPages(), info.getPublisher(),
                info.getAuthors() == null ? List.of() : List.copyOf(info.getAuthors()));
        }
        SyncDvdInfo dvdInfo = null;
        if (item.getDvdInfo() != null) {
            DvdInfo info = item.getDvdInfo();
            dvdInfo = new SyncDvdInfo(info.getRuntime(), info.getDirector(),
                info.getCast() == null ? List.of() : List.copyOf(info.getCast()));
        }
        List<String> tags = item.getTags().stream().map(Tag::getName).sorted().toList();
        return new SyncItem(
            item.getId(),
            item.getType(),
            item.getTitle(),
            item.getYear(),
            item.getCondition(),
            item.getLocation(),
            item.getStatus(),
            item.getDeletedAt(),
            item.getCreatedAt(),
            item.getUpdatedAt(),
            item.getProgressPercent(),
            item.getProgressValue(),
            item.getTotalValue(),
            tags,
            bookInfo,
            dvdInfo
        );
    }

    private SyncList toSyncList(MediaList list) {
        return new SyncList(list.getId(), list.getName(), list.getUpdatedAt());
    }

    private SyncListItem toSyncListItem(ListItem item) {
        return new SyncListItem(
            item.getId().getListId(),
            item.getId().getItemId(),
            item.getPosition(),
            item.getPriority(),
            item.getUpdatedAt()
        );
    }

    private SyncProgressLog toSyncProgressLog(ProgressLog log) {
        return new SyncProgressLog(
            log.getId(),
            log.getItem().getId(),
            log.getLogDate(),
            log.getDurationMinutes(),
            log.getPageOrMinute(),
            log.getPercent(),
            log.getReaderName(),
            log.getUpdatedAt()
        );
    }

    private SyncLoan toSyncLoan(Loan loan) {
        return new SyncLoan(
            loan.getId(),
            loan.getItem().getId(),
            loan.getToWhom(),
            loan.getStartDate(),
            loan.getDueDate(),
            loan.getReturnedAt(),
            loan.getStatus(),
            loan.getUpdatedAt()
        );
    }

    private SyncExternalLink toSyncExternalLink(ExternalLink link) {
        return new SyncExternalLink(
            link.getId(),
            link.getItem().getId(),
            link.getProvider(),
            link.getExternalId(),
            link.getUrl(),
            link.getRating(),
            link.getSummary(),
            link.getLastSyncAt(),
            link.getUpdatedAt()
        );
    }

    private <T> List<T> filterByUpdatedAt(List<T> items, OffsetDateTime since) {
        if (since == null || items == null) {
            return items == null ? List.of() : items;
        }
        List<T> results = new ArrayList<>();
        for (T item : items) {
            OffsetDateTime updatedAt = extractUpdatedAt(item);
            if (updatedAt != null && updatedAt.isAfter(since)) {
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

    private int applyItems(Long userId, List<SyncItem> items) {
        if (items == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncItem item : items) {
            if (item.id() == null || item.updatedAt() == null) {
                continue;
            }
            if (ownedByOtherUser("media_item", item.id(), userId)) {
                conflicts++;
                continue;
            }
            OffsetDateTime localUpdated = findUpdatedAt("media_item", "id", item.id(), userId);
            if (localUpdated != null && !item.updatedAt().isAfter(localUpdated)) {
                conflicts++;
                continue;
            }
            upsertItem(userId, item);
        }
        return conflicts;
    }

    private int applyLists(Long userId, List<SyncList> lists) {
        if (lists == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncList list : lists) {
            if (list.id() == null || list.updatedAt() == null) {
                continue;
            }
            if (ownedByOtherUser("list", list.id(), userId)) {
                conflicts++;
                continue;
            }
            OffsetDateTime localUpdated = findUpdatedAt("list", "id", list.id(), userId);
            if (localUpdated != null && !list.updatedAt().isAfter(localUpdated)) {
                conflicts++;
                continue;
            }
            jdbcTemplate.update("""
                INSERT INTO list (id, name, user_id, created_at, updated_at)
                VALUES (?, ?, ?, COALESCE((SELECT created_at FROM list WHERE id = ?), ?), ?)
                ON CONFLICT (id) DO UPDATE SET
                  name = EXCLUDED.name,
                  updated_at = EXCLUDED.updated_at
                WHERE list.user_id = EXCLUDED.user_id
                """,
                list.id(),
                list.name(),
                userId,
                list.id(),
                OffsetDateTime.now(ZoneOffset.UTC),
                list.updatedAt()
            );
        }
        return conflicts;
    }

    private int applyListItems(Long userId, List<SyncListItem> listItems) {
        if (listItems == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncListItem item : listItems) {
            if (item.listId() == null || item.itemId() == null || item.updatedAt() == null) {
                continue;
            }
            if (!listOwnedByUser(userId, item.listId()) || !itemOwnedByUser(userId, item.itemId())) {
                conflicts++;
                continue;
            }
            OffsetDateTime localUpdated = findListItemUpdatedAt(userId, item.listId(), item.itemId());
            if (localUpdated != null && !item.updatedAt().isAfter(localUpdated)) {
                conflicts++;
                continue;
            }
            jdbcTemplate.update("""
                INSERT INTO list_item (list_id, item_id, position, priority, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (list_id, item_id) DO UPDATE SET
                  position = EXCLUDED.position,
                  priority = EXCLUDED.priority,
                  updated_at = EXCLUDED.updated_at
                """,
                item.listId(), item.itemId(), item.position(), item.priority(), item.updatedAt());
        }
        return conflicts;
    }

    private int applyProgress(Long userId, List<SyncProgressLog> logs) {
        if (logs == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncProgressLog log : logs) {
            if (log.id() == null || log.updatedAt() == null) {
                continue;
            }
            if (!itemOwnedByUser(userId, log.itemId()) || ownedByOtherUserViaItem("progress_log", log.id(), userId)) {
                conflicts++;
                continue;
            }
            OffsetDateTime localUpdated = findUpdatedAt("progress_log", "id", log.id());
            if (localUpdated != null && !log.updatedAt().isAfter(localUpdated)) {
                conflicts++;
                continue;
            }
            jdbcTemplate.update("""
                INSERT INTO progress_log (id, item_id, log_date, duration_minutes, page_or_minute, percent, reader_name, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                  item_id = EXCLUDED.item_id,
                  log_date = EXCLUDED.log_date,
                  duration_minutes = EXCLUDED.duration_minutes,
                  page_or_minute = EXCLUDED.page_or_minute,
                  percent = EXCLUDED.percent,
                  reader_name = EXCLUDED.reader_name,
                  updated_at = EXCLUDED.updated_at
                """,
                log.id(), log.itemId(), log.date(), log.durationMinutes(), log.pageOrMinute(), log.percent(),
                log.readerName(), log.updatedAt());
        }
        return conflicts;
    }

    private int applyLoans(Long userId, List<SyncLoan> loans) {
        if (loans == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncLoan loan : loans) {
            if (loan.id() == null || loan.updatedAt() == null) {
                continue;
            }
            if (!itemOwnedByUser(userId, loan.itemId()) || ownedByOtherUserViaItem("loan", loan.id(), userId)) {
                conflicts++;
                continue;
            }
            OffsetDateTime localUpdated = findUpdatedAt("loan", "id", loan.id());
            if (localUpdated != null && !loan.updatedAt().isAfter(localUpdated)) {
                conflicts++;
                continue;
            }
            jdbcTemplate.update("""
                INSERT INTO loan (id, item_id, to_whom, start_date, due_date, returned_at, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                  item_id = EXCLUDED.item_id,
                  to_whom = EXCLUDED.to_whom,
                  start_date = EXCLUDED.start_date,
                  due_date = EXCLUDED.due_date,
                  returned_at = EXCLUDED.returned_at,
                  status = EXCLUDED.status,
                  updated_at = EXCLUDED.updated_at
                """,
                loan.id(), loan.itemId(), loan.toWhom(), loan.startDate(), loan.dueDate(), loan.returnedAt(), loan.status().name(), loan.updatedAt());
        }
        return conflicts;
    }

    private int applyExternalLinks(Long userId, List<SyncExternalLink> links) {
        if (links == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncExternalLink link : links) {
            if (link.id() == null || link.updatedAt() == null) {
                continue;
            }
            if (!itemOwnedByUser(userId, link.itemId()) || ownedByOtherUserViaItem("external_link", link.id(), userId)) {
                conflicts++;
                continue;
            }
            OffsetDateTime localUpdated = findUpdatedAt("external_link", "id", link.id());
            if (localUpdated != null && !link.updatedAt().isAfter(localUpdated)) {
                conflicts++;
                continue;
            }
            jdbcTemplate.update("""
                INSERT INTO external_link (id, item_id, provider, external_id, url, rating, summary, last_sync_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                  item_id = EXCLUDED.item_id,
                  provider = EXCLUDED.provider,
                  external_id = EXCLUDED.external_id,
                  url = EXCLUDED.url,
                  rating = EXCLUDED.rating,
                  summary = EXCLUDED.summary,
                  last_sync_at = EXCLUDED.last_sync_at,
                  updated_at = EXCLUDED.updated_at
                """,
                link.id(), link.itemId(), link.provider(), link.externalId(), link.url(), link.rating(), link.summary(), link.lastSyncAt(), link.updatedAt());
        }
        return conflicts;
    }

    private int applyDeletes(Long userId, List<SyncDelete> deletes) {
        if (deletes == null) {
            return 0;
        }
        int conflicts = 0;
        for (SyncDelete delete : deletes) {
            if (delete.entityType() == null || delete.entityKey() == null || delete.deletedAt() == null) {
                continue;
            }
            if ("LIST".equalsIgnoreCase(delete.entityType())) {
                if (!listOwnedByUser(userId, Long.valueOf(delete.entityKey()))) {
                    conflicts++;
                    continue;
                }
                OffsetDateTime localUpdated = findUpdatedAt("list", "id", Long.valueOf(delete.entityKey()), userId);
                if (localUpdated != null && localUpdated.isAfter(delete.deletedAt())) {
                    conflicts++;
                    continue;
                }
                jdbcTemplate.update("DELETE FROM list_item WHERE list_id = ?", Long.valueOf(delete.entityKey()));
                jdbcTemplate.update("DELETE FROM list WHERE id = ?", Long.valueOf(delete.entityKey()));
            }
            if ("LIST_ITEM".equalsIgnoreCase(delete.entityType())) {
                String[] parts = delete.entityKey().split(":");
                if (parts.length != 2) {
                    continue;
                }
                Long listId = Long.valueOf(parts[0]);
                Long itemId = Long.valueOf(parts[1]);
                if (!listOwnedByUser(userId, listId) || !itemOwnedByUser(userId, itemId)) {
                    conflicts++;
                    continue;
                }
                OffsetDateTime localUpdated = findListItemUpdatedAt(userId, listId, itemId);
                if (localUpdated != null && localUpdated.isAfter(delete.deletedAt())) {
                    conflicts++;
                    continue;
                }
                jdbcTemplate.update("DELETE FROM list_item WHERE list_id = ? AND item_id = ?", listId, itemId);
            }
        }
        return conflicts;
    }

    private OffsetDateTime findUpdatedAt(String table, String idColumn, Long id) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM " + table + " WHERE " + idColumn + " = ?",
                OffsetDateTime.class,
                id
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private OffsetDateTime findUpdatedAt(String table, String idColumn, Long id, Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM " + table + " WHERE " + idColumn + " = ? AND user_id = ?",
                OffsetDateTime.class,
                id,
                userId
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private OffsetDateTime findListItemUpdatedAt(Long userId, Long listId, Long itemId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT li.updated_at FROM list_item li JOIN list l ON l.id = li.list_id WHERE li.list_id = ? AND li.item_id = ? AND l.user_id = ?",
                OffsetDateTime.class,
                listId,
                itemId,
                userId
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean itemOwnedByUser(Long userId, Long itemId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media_item WHERE id = ? AND user_id = ?",
                Integer.class,
                itemId,
                userId
            );
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean listOwnedByUser(Long userId, Long listId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM list WHERE id = ? AND user_id = ?",
                Integer.class,
                listId,
                userId
            );
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean ownedByOtherUser(String table, Long id, Long userId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ? AND user_id IS NOT NULL AND user_id <> ?",
                Integer.class,
                id,
                userId
            );
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean ownedByOtherUserViaItem(String table, Long id, Long userId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " t JOIN media_item mi ON mi.id = t.item_id WHERE t.id = ? AND mi.user_id <> ?",
                Integer.class,
                id,
                userId
            );
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private void upsertItem(Long userId, SyncItem item) {
        jdbcTemplate.update("""
            INSERT INTO media_item (id, user_id, type, title, year, condition, location, status, deleted_at, created_at, updated_at,
              progress_percent, progress_value, total_value)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
              user_id = EXCLUDED.user_id,
              type = EXCLUDED.type,
              title = EXCLUDED.title,
              year = EXCLUDED.year,
              condition = EXCLUDED.condition,
              location = EXCLUDED.location,
              status = EXCLUDED.status,
              deleted_at = EXCLUDED.deleted_at,
              updated_at = EXCLUDED.updated_at,
              progress_percent = EXCLUDED.progress_percent,
              progress_value = EXCLUDED.progress_value,
              total_value = EXCLUDED.total_value
            WHERE media_item.user_id = EXCLUDED.user_id
            """,
            item.id(), userId, item.type().name(), item.title(), item.year(), item.condition(), item.location(),
            item.status().name(), item.deletedAt(), item.createdAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : item.createdAt(),
            item.updatedAt(), item.progressPercent(), item.progressValue(), item.totalValue());

        if (item.bookInfo() != null) {
            upsertBookInfo(item.id(), item.bookInfo());
            jdbcTemplate.update("DELETE FROM dvd_info WHERE item_id = ?", item.id());
            jdbcTemplate.update("DELETE FROM dvd_cast WHERE item_id = ?", item.id());
        } else if (item.dvdInfo() != null) {
            upsertDvdInfo(item.id(), item.dvdInfo());
            jdbcTemplate.update("DELETE FROM book_info WHERE item_id = ?", item.id());
            jdbcTemplate.update("DELETE FROM book_author WHERE item_id = ?", item.id());
        }
        upsertTags(item.id(), item.tags());
    }

    private void upsertBookInfo(Long itemId, SyncBookInfo info) {
        jdbcTemplate.update("""
            INSERT INTO book_info (item_id, isbn, pages, publisher, authors_text)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (item_id) DO UPDATE SET
              isbn = EXCLUDED.isbn,
              pages = EXCLUDED.pages,
              publisher = EXCLUDED.publisher,
              authors_text = EXCLUDED.authors_text
            """, itemId, info.isbn(), info.pages(), info.publisher(), join(info.authors()));
        jdbcTemplate.update("DELETE FROM book_author WHERE item_id = ?", itemId);
        if (info.authors() != null) {
            for (String author : info.authors()) {
                jdbcTemplate.update("INSERT INTO book_author (item_id, author) VALUES (?, ?)", itemId, author);
            }
        }
    }

    private void upsertDvdInfo(Long itemId, SyncDvdInfo info) {
        jdbcTemplate.update("""
            INSERT INTO dvd_info (item_id, runtime, director)
            VALUES (?, ?, ?)
            ON CONFLICT (item_id) DO UPDATE SET
              runtime = EXCLUDED.runtime,
              director = EXCLUDED.director
            """, itemId, info.runtime(), info.director());
        jdbcTemplate.update("DELETE FROM dvd_cast WHERE item_id = ?", itemId);
        if (info.cast() != null) {
            for (String member : info.cast()) {
                jdbcTemplate.update("INSERT INTO dvd_cast (item_id, member) VALUES (?, ?)", itemId, member);
            }
        }
    }

    private void upsertTags(Long itemId, List<String> tags) {
        jdbcTemplate.update("DELETE FROM media_item_tag WHERE item_id = ?", itemId);
        if (tags == null) {
            return;
        }
        for (String name : tags) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Long tagId = jdbcTemplate.queryForObject(
                "INSERT INTO tag (name) VALUES (?) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id",
                Long.class, name
            );
            jdbcTemplate.update("""
                INSERT INTO media_item_tag (item_id, tag_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """, itemId, tagId);
        }
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(", ", values);
    }

    private void updateSequences() {
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('media_item','id'), COALESCE(MAX(id), 1)) FROM media_item");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('list','id'), COALESCE(MAX(id), 1)) FROM list");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('progress_log','id'), COALESCE(MAX(id), 1)) FROM progress_log");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('loan','id'), COALESCE(MAX(id), 1)) FROM loan");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('external_link','id'), COALESCE(MAX(id), 1)) FROM external_link");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('tag','id'), COALESCE(MAX(id), 1)) FROM tag");
    }
}
