package com.example.plms.web.dto;

import java.time.LocalDate;

public record ProgressLogResponse(
    Long id,
    LocalDate date,
    Integer durationMinutes,
    Integer pageOrMinute,
    Integer percent
) {
}
