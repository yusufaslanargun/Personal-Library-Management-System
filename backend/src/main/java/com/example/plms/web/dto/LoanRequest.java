package com.example.plms.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LoanRequest(
    @NotBlank String toWhom,
    @NotNull LocalDate startDate,
    @NotNull LocalDate dueDate
) {
}
