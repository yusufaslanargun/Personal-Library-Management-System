package com.example.plms.service;

import com.example.plms.domain.Loan;
import com.example.plms.domain.LoanStatus;
import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaStatus;
import com.example.plms.repository.LoanRepository;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.web.dto.LoanRequest;
import com.example.plms.web.dto.LoanResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final MediaItemRepository itemRepository;

    public LoanService(LoanRepository loanRepository, MediaItemRepository itemRepository) {
        this.loanRepository = loanRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public LoanResponse createLoan(Long itemId, LoanRequest request) {
        if (request.dueDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueDate must be on or after startDate");
        }
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (loanRepository.findFirstByItemIdAndReturnedAtIsNull(itemId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item already loaned");
        }

        Loan loan = new Loan(item, request.toWhom(), request.startDate(), request.dueDate());
        item.setStatus(MediaStatus.LOANED);
        try {
            Loan saved = loanRepository.save(loan);
            itemRepository.save(item);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item already loaned");
        }
    }

    @Transactional
    public LoanResponse returnLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        if (loan.getReturnedAt() != null) {
            return toResponse(loan);
        }
        loan.setReturnedAt(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);
        MediaItem item = loan.getItem();
        item.setStatus(MediaStatus.AVAILABLE);
        loanRepository.save(loan);
        itemRepository.save(item);
        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> overdue() {
        return loanRepository.findByReturnedAtIsNullAndDueDateBefore(LocalDate.now()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public LoanResponse activeLoan(Long itemId) {
        Loan loan = loanRepository.findFirstByItemIdAndReturnedAtIsNull(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active loan"));
        return toResponse(loan);
    }

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
            loan.getId(),
            loan.getItem().getId(),
            loan.getToWhom(),
            loan.getStartDate(),
            loan.getDueDate(),
            loan.getReturnedAt(),
            loan.getStatus()
        );
    }
}
