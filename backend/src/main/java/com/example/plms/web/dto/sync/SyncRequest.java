package com.example.plms.web.dto.sync;

import java.time.OffsetDateTime;

public record SyncRequest(
    String clientId,
    OffsetDateTime lastSyncAt,
    SyncPayload changes
) {
}
