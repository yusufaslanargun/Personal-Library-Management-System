package com.example.plms.service;

import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaType;
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

    @InjectMocks
    private ProgressService progressService;

    @Test
    void clampsProgressWithinRange() {
        MediaItem item = new MediaItem(MediaType.BOOK, "Test", 2020);
        item.setTotalValue(200);
        when(itemRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(item));
        when(progressLogRepository.save(org.mockito.Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProgressLogRequest request = new ProgressLogRequest(LocalDate.now(), 30, 100);
        var response = progressService.logProgress(1L, request);

        assertEquals(50, response.percent());
    }

    @Test
    void rejectsProgressAboveTotal() {
        MediaItem item = new MediaItem(MediaType.BOOK, "Test", 2020);
        item.setTotalValue(100);
        when(itemRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(item));

        ProgressLogRequest request = new ProgressLogRequest(LocalDate.now(), 10, 120);
        assertThrows(ResponseStatusException.class, () -> progressService.logProgress(1L, request));
    }
}
