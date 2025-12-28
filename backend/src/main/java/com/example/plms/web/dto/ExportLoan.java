package com.example.plms.web.dto;

import com.example.plms.domain.LoanStatus;
import java.time.LocalDate;

public record ExportLoan(
    Long id,
    Long itemId,
    String toWhom,
    LocalDate startDate,
    LocalDate dueDate,
    LocalDate returnedAt,
    LoanStatus status
) {
}
