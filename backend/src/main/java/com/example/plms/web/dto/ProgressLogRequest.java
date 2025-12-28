package com.example.plms.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ProgressLogRequest(
    @NotNull LocalDate date,
    Integer durationMinutes,
    @NotNull Integer pageOrMinute
) {
}
