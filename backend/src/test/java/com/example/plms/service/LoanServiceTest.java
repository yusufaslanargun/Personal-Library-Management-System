package com.example.plms.service;

import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaType;
import com.example.plms.repository.LoanRepository;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.web.dto.LoanRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {
    @Mock
    private LoanRepository loanRepository;

    @Mock
    private MediaItemRepository itemRepository;

    @InjectMocks
    private LoanService loanService;

    @Test
    void rejectsInvalidDueDate() {
        long userId = 1L;
        LoanRequest request = new LoanRequest("Alex", LocalDate.now(), LocalDate.now().minusDays(1));
        assertThrows(ResponseStatusException.class, () -> loanService.createLoan(userId, 1L, request));
    }

    @Test
    void rejectsSecondActiveLoan() {
        long userId = 1L;
        MediaItem item = new MediaItem(MediaType.BOOK, "Test", 2020);
        when(itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(1L, userId)).thenReturn(Optional.of(item));
        com.example.plms.domain.Loan existingLoan = new com.example.plms.domain.Loan(item, "Alex", LocalDate.now(), LocalDate.now().plusDays(3));
        when(loanRepository.findFirstByItemIdAndReturnedAtIsNullAndItemOwner_Id(1L, userId)).thenReturn(Optional.of(existingLoan));

        LoanRequest request = new LoanRequest("Alex", LocalDate.now(), LocalDate.now().plusDays(3));
        assertThrows(ResponseStatusException.class, () -> loanService.createLoan(userId, 1L, request));
    }
}
