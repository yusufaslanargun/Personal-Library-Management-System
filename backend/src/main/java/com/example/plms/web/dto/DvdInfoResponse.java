package com.example.plms.web.dto;

import java.util.List;

public record DvdInfoResponse(
    Integer runtime,
    String director,
    List<String> cast
) {
}
