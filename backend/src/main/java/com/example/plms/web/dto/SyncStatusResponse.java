package com.example.plms.web.dto;

import java.time.OffsetDateTime;

public record SyncStatusResponse(
    boolean enabled,
    OffsetDateTime lastSyncAt,
    String lastStatus,
    Integer lastConflictCount
) {
}
