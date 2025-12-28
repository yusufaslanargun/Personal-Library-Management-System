package com.example.plms.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record IsbnConfirmRequest(
    @NotBlank String provider,
    @NotBlank String externalId,
    @NotBlank String title,
    List<String> authors,
    String publisher,
    Integer pageCount,
    Integer year,
    String description,
    String infoLink,
    BigDecimal averageRating,
    @NotBlank String isbn
) {
}
