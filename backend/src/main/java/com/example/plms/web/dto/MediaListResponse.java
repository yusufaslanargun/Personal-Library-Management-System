package com.example.plms.web.dto;

import java.util.List;

public record MediaListResponse(
    Long id,
    String name,
    List<ListItemResponse> items
) {
}
