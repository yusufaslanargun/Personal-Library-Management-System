package com.example.plms.web.dto;

public record ExternalDiffField(
    String field,
    String currentValue,
    String newValue
) {
}
