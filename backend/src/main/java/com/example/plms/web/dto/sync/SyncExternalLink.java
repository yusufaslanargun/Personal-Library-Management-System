package com.example.plms.web.dto.sync;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SyncExternalLink(
    Long id,
    Long itemId,
    String provider,
    String externalId,
    String url,
    BigDecimal rating,
    String summary,
    OffsetDateTime lastSyncAt,
    OffsetDateTime updatedAt
) {
}
