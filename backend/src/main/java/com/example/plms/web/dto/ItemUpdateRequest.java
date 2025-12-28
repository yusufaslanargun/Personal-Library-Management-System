package com.example.plms.web.dto;

import com.example.plms.domain.MediaType;
import java.util.List;

public record ItemUpdateRequest(
    MediaType type,
    String title,
    Integer year,
    String condition,
    String location,
    List<String> tags,
    BookInfoRequest bookInfo,
    DvdInfoRequest dvdInfo
) {
}
