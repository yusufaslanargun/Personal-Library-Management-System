package com.example.plms.web.dto.sync;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SyncProgressLog(
    Long id,
    Long itemId,
    LocalDate date,
    Integer durationMinutes,
    Integer pageOrMinute,
    Integer percent,
    String readerName,
    OffsetDateTime updatedAt
) {
}
