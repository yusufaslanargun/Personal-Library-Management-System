package com.example.plms.web.dto;

import java.time.LocalDate;

public record ExportProgressLog(
    Long id,
    Long itemId,
    LocalDate date,
    Integer durationMinutes,
    Integer pageOrMinute,
    Integer percent,
    String readerName
) {
}
