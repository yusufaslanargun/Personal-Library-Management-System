package com.example.plms.web.dto;

import com.example.plms.domain.MediaStatus;
import com.example.plms.domain.MediaType;
import java.time.OffsetDateTime;
import java.util.List;

public record ExportItem(
    Long id,
    MediaType type,
    String title,
    Integer year,
    String condition,
    String location,
    MediaStatus status,
    OffsetDateTime deletedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Integer progressPercent,
    Integer progressValue,
    Integer totalValue,
    List<String> tags,
    ExportBookInfo bookInfo,
    ExportDvdInfo dvdInfo
) {
}
