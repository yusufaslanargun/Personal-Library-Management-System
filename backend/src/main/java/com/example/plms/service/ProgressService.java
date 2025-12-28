package com.example.plms.service;

import com.example.plms.domain.MediaItem;
import com.example.plms.domain.ProgressLog;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.repository.ProgressLogRepository;
import com.example.plms.web.dto.ProgressLogRequest;
import com.example.plms.web.dto.ProgressLogResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProgressService {
    private final MediaItemRepository itemRepository;
    private final ProgressLogRepository progressRepository;

    public ProgressService(MediaItemRepository itemRepository, ProgressLogRepository progressRepository) {
        this.itemRepository = itemRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional
    public ProgressLogResponse logProgress(Long itemId, ProgressLogRequest request) {
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        int total = item.getTotalValue() == null ? 0 : item.getTotalValue();
        if (total <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total value not set for item");
        }
        if (request.pageOrMinute() == null || request.pageOrMinute() < 0 || request.pageOrMinute() > total) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Progress value out of range");
        }
        int percent = (int) Math.round((request.pageOrMinute() * 100.0) / total);
        percent = Math.max(0, Math.min(100, percent));

        ProgressLog log = new ProgressLog(item, request.date(), request.durationMinutes(), request.pageOrMinute(), percent);
        ProgressLog saved = progressRepository.save(log);

        item.setProgressPercent(percent);
        item.setProgressValue(request.pageOrMinute());
        itemRepository.save(item);

        return new ProgressLogResponse(saved.getId(), saved.getLogDate(), saved.getDurationMinutes(), saved.getPageOrMinute(), saved.getPercent());
    }

    @Transactional(readOnly = true)
    public List<ProgressLogResponse> history(Long itemId) {
        itemRepository.findByIdAndDeletedAtIsNull(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        return progressRepository.findByItemIdOrderByLogDateDesc(itemId).stream()
            .map(log -> new ProgressLogResponse(log.getId(), log.getLogDate(), log.getDurationMinutes(), log.getPageOrMinute(), log.getPercent()))
            .toList();
    }
}
