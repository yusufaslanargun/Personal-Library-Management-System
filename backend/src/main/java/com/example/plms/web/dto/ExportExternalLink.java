package com.example.plms.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExportExternalLink(
    Long id,
    Long itemId,
    String provider,
    String externalId,
    String url,
    BigDecimal rating,
    String summary,
    OffsetDateTime lastSyncAt
) {
}
