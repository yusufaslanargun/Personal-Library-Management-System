package com.example.plms.web.dto.sync;

import java.time.OffsetDateTime;

public record SyncResponse(
    OffsetDateTime serverTime,
    SyncPayload changes,
    int conflictCount
) {
}
