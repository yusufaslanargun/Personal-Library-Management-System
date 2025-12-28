package com.example.plms.web.dto;

public record ListItemResponse(
    Long itemId,
    String title,
    Integer position,
    Integer priority
) {
}
