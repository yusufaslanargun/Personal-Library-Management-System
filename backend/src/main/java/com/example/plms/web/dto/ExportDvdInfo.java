package com.example.plms.web.dto;

import java.util.List;

public record ExportDvdInfo(
    Long itemId,
    Integer runtime,
    String director,
    List<String> cast
) {
}
