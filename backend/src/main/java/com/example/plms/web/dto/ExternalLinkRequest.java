package com.example.plms.web.dto;

import java.math.BigDecimal;

public record ExternalLinkRequest(
    String provider,
    String externalId,
    String url,
    BigDecimal rating,
    String summary
) {
}
