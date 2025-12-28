package com.example.plms.web.dto.sync;

import java.time.OffsetDateTime;

public record SyncList(
    Long id,
    String name,
    OffsetDateTime updatedAt
) {
}
