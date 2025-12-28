package com.example.plms.web.dto;

public record ExportListItem(
    Long listId,
    Long itemId,
    Integer position,
    Integer priority
) {
}
