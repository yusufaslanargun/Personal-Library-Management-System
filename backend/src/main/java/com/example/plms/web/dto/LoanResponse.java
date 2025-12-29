package com.example.plms.web.dto;

import com.example.plms.domain.LoanStatus;
import com.example.plms.domain.MediaType;
import java.time.LocalDate;

public record LoanResponse(
    Long id,
    Long itemId,
    String itemTitle,
    MediaType itemType,
    String toWhom,
    LocalDate startDate,
    LocalDate dueDate,
    LocalDate returnedAt,
    LoanStatus status
) {
}
