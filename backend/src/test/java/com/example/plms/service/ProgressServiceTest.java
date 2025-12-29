package com.example.plms.service;

import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaType;
import com.example.plms.repository.LoanRepository;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.repository.ProgressLogRepository;
import com.example.plms.web.dto.ProgressLogRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {
    @Mock
    private MediaItemRepository itemRepository;

    @Mock
    private ProgressLogRepository progressLogRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private ProgressService progressService;

    @Test
    void clampsProgressWithinRange() {
        long userId = 1L;
        MediaItem item = new MediaItem(MediaType.BOOK, "Test", 2020);
        item.setTotalValue(200);
        when(itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(1L, userId)).thenReturn(Optional.of(item));
        when(loanRepository.findActiveLoanForOwner(1L, userId)).thenReturn(Optional.of(new com.example.plms.domain.Loan(
            item, "Reader", LocalDate.now(), LocalDate.now().plusDays(1)
        )));
        when(progressLogRepository.save(org.mockito.Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressLogRequest request = new ProgressLogRequest(LocalDate.now(), 30, 100);
        var response = progressService.logProgress(userId, 1L, request);

        assertEquals(50, response.percent());
    }

    @Test
    void rejectsProgressAboveTotal() {
        long userId = 1L;
        MediaItem item = new MediaItem(MediaType.BOOK, "Test", 2020);
        item.setTotalValue(100);
        when(itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(1L, userId)).thenReturn(Optional.of(item));
        when(loanRepository.findActiveLoanForOwner(1L, userId)).thenReturn(Optional.of(new com.example.plms.domain.Loan(
            item, "Reader", LocalDate.now(), LocalDate.now().plusDays(1)
        )));

        ProgressLogRequest request = new ProgressLogRequest(LocalDate.now(), 10, 120);
        assertThrows(ResponseStatusException.class, () -> progressService.logProgress(userId, 1L, request));
    }

    @Test
    void rejectsProgressWithoutActiveLoan() {
        long userId = 1L;
        MediaItem item = new MediaItem(MediaType.BOOK, "Test", 2020);
        item.setTotalValue(100);
        when(itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(1L, userId)).thenReturn(Optional.of(item));
        when(loanRepository.findActiveLoanForOwner(1L, userId)).thenReturn(Optional.empty());

        ProgressLogRequest request = new ProgressLogRequest(LocalDate.now(), 10, 50);
        assertThrows(ResponseStatusException.class, () -> progressService.logProgress(userId, 1L, request));
    }
}
