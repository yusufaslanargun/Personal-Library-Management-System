package com.example.plms.service;

import com.example.plms.config.SyncProperties;
import com.example.plms.domain.MediaItem;
import com.example.plms.domain.SyncState;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.repository.SyncStateRepository;
import com.example.plms.web.dto.SyncStatusResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final RestTemplate restTemplate;

    public SyncService(SyncProperties properties, SyncStateRepository syncStateRepository,
                       MediaItemRepository itemRepository, RestTemplate restTemplate) {
        this.properties = properties;
        this.syncStateRepository = syncStateRepository;
        this.itemRepository = itemRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public SyncStatusResponse enable(boolean enabled) {
        properties.setEnabled(enabled);
        SyncState state = getOrCreateState();
        state.setLastStatus(enabled ? "enabled" : "disabled");
        syncStateRepository.save(state);
        return status();
    }

    @Transactional
    public SyncStatusResponse runSync() {
        SyncState state = getOrCreateState();
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

        OffsetDateTime since = state.getLastSyncAt();
        List<MediaItem> items = itemRepository.findAll();
        List<Map<String, Object>> payload = new ArrayList<>();
        for (MediaItem item : items) {
            if (since == null || item.getUpdatedAt().isAfter(since)) {
                payload.add(Map.of(
                    "id", item.getId(),
                    "type", item.getType().name(),
                    "title", item.getTitle(),
                    "year", item.getYear(),
                    "updatedAt", item.getUpdatedAt().toString()
                ));
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.set("X-API-Key", properties.getApiKey());
        }

        try {
            ResponseEntity<List> response = restTemplate.postForEntity(properties.getEndpoint(), new HttpEntity<>(payload, headers), List.class);
            int conflicts = applyRemote(response.getBody());
            state.setLastConflictCount(conflicts);
            state.setLastStatus("success");
            state.setLastSyncAt(OffsetDateTime.now(ZoneOffset.UTC));
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
    public SyncStatusResponse status() {
        SyncState state = getOrCreateState();
        return toResponse(state);
    }

    private int applyRemote(List<?> payload) {
        if (payload == null) {
            return 0;
        }
        int conflicts = 0;
        for (Object obj : payload) {
            if (!(obj instanceof Map<?, ?> map)) {
                continue;
            }
            Object idValue = map.get("id");
            if (idValue == null) {
                continue;
            }
            Long id = Long.valueOf(idValue.toString());
            MediaItem local = itemRepository.findById(id).orElse(null);
            if (local == null) {
                continue;
            }
            OffsetDateTime remoteUpdated = OffsetDateTime.parse(String.valueOf(map.get("updatedAt")));
            if (remoteUpdated.isAfter(local.getUpdatedAt())) {
                local.setTitle(String.valueOf(map.get("title")));
                local.setYear(Integer.valueOf(String.valueOf(map.get("year"))));
                itemRepository.save(local);
            } else {
                conflicts++;
            }
        }
        return conflicts;
    }

    private SyncState getOrCreateState() {
        return syncStateRepository.findById(1).orElseGet(() -> syncStateRepository.save(new SyncState()));
    }

    private SyncStatusResponse toResponse(SyncState state) {
        return new SyncStatusResponse(properties.isEnabled(), state.getLastSyncAt(), state.getLastStatus(), state.getLastConflictCount());
    }
}
