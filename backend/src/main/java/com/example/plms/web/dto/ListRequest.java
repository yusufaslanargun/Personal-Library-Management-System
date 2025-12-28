package com.example.plms.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ListRequest(
    @NotBlank String name
) {
}
