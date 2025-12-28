package com.example.plms.external;

import java.math.BigDecimal;
import java.util.List;

public record ExternalMetadata(
    String externalId,
    String title,
    List<String> authors,
    String publisher,
    Integer pageCount,
    Integer year,
    String description,
    BigDecimal rating,
    String url
) {
}
