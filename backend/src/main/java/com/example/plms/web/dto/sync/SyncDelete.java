package com.example.plms.web.dto.sync;

import java.time.OffsetDateTime;

public record SyncDelete(
    String entityType,
    String entityKey,
    OffsetDateTime deletedAt
) {
}
