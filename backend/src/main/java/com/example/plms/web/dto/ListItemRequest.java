package com.example.plms.web.dto;

import jakarta.validation.constraints.NotNull;

public record ListItemRequest(
    @NotNull Long itemId,
    Integer priority
) {
}
