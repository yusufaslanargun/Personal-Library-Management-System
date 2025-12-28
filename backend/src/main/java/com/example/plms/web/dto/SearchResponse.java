package com.example.plms.web.dto;

import java.util.List;

public record SearchResponse(
    List<ItemResponse> items,
    int page,
    int size,
    long total
) {
}
