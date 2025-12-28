package com.example.plms.web.dto.sync;

import com.example.plms.domain.LoanStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SyncLoan(
    Long id,
    Long itemId,
    String toWhom,
    LocalDate startDate,
    LocalDate dueDate,
    LocalDate returnedAt,
    LoanStatus status,
    OffsetDateTime updatedAt
) {
}
