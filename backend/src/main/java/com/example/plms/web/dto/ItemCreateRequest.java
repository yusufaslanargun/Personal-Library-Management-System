package com.example.plms.web.dto;

import com.example.plms.domain.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ItemCreateRequest(
    @NotNull MediaType type,
    @NotBlank String title,
    @NotNull Integer year,
    String condition,
    String location,
    List<String> tags,
    BookInfoRequest bookInfo,
    DvdInfoRequest dvdInfo
) {
}
