package com.example.plms.web.dto;

import java.util.List;

public record ExternalApplyRequest(
    List<String> fields
) {
}
