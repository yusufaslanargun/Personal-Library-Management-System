package com.example.plms.service;

import com.example.plms.domain.Loan;
import com.example.plms.domain.MediaItem;
import com.example.plms.domain.ProgressLog;
import com.example.plms.repository.LoanRepository;
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
    private final LoanRepository loanRepository;

    public ProgressService(MediaItemRepository itemRepository,
                           ProgressLogRepository progressRepository,
                           LoanRepository loanRepository) {
        this.itemRepository = itemRepository;
        this.progressRepository = progressRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public ProgressLogResponse logProgress(Long userId, Long itemId, ProgressLogRequest request) {
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        Loan activeLoan = loanRepository.findActiveLoanForOwner(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active loan required to log progress"));
        int total = item.getTotalValue() == null ? 0 : item.getTotalValue();
        if (total <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total value not set for item");
        }
        if (request.pageOrMinute() == null || request.pageOrMinute() < 0 || request.pageOrMinute() > total) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Progress value out of range");
        }
        int percent = (int) Math.round((request.pageOrMinute() * 100.0) / total);
        percent = Math.max(0, Math.min(100, percent));

        ProgressLog log = new ProgressLog(item, request.date(), request.durationMinutes(), request.pageOrMinute(),
            percent, activeLoan.getToWhom());
        ProgressLog saved = progressRepository.save(log);

        item.setProgressPercent(percent);
        item.setProgressValue(request.pageOrMinute());
        itemRepository.save(item);

        return new ProgressLogResponse(saved.getId(), saved.getLogDate(), saved.getDurationMinutes(),
            saved.getPageOrMinute(), saved.getPercent(), saved.getReaderName());
    }

    @Transactional(readOnly = true)
    public List<ProgressLogResponse> history(Long userId, Long itemId) {
        itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        return progressRepository.findByItemIdOrderByLogDateDesc(itemId).stream()
            .map(log -> new ProgressLogResponse(log.getId(), log.getLogDate(), log.getDurationMinutes(),
                log.getPageOrMinute(), log.getPercent(), log.getReaderName()))
            .toList();
    }

    @Transactional
    public void deleteLog(Long userId, Long itemId, Long logId) {
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        ProgressLog log = progressRepository.findByIdAndOwner(logId, itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Progress log not found"));
        progressRepository.delete(log);

        ProgressLog latest = progressRepository.findFirstByItemIdOrderByLogDateDescIdDesc(itemId).orElse(null);
        if (latest == null) {
            item.setProgressPercent(0);
            item.setProgressValue(0);
        } else {
            item.setProgressPercent(latest.getPercent());
            item.setProgressValue(latest.getPageOrMinute());
        }
        itemRepository.save(item);
    }
}
