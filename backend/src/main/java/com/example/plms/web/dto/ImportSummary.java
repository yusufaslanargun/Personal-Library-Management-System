package com.example.plms.web.dto;

import java.util.List;

public record ImportSummary(
    int added,
    int updated,
    int skipped,
    List<String> errors
) {
}
