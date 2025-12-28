package com.example.plms.web.dto;

import java.util.List;

public record DvdInfoRequest(
    Integer runtime,
    String director,
    List<String> cast
) {
}
