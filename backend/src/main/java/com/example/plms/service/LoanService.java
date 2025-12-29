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
    public LoanResponse createLoan(Long userId, Long itemId, LoanRequest request) {
        if (request.dueDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueDate must be on or after startDate");
        }
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (loanRepository.findActiveLoanForOwner(itemId, userId).isPresent()) {
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
    public LoanResponse returnLoan(Long userId, Long loanId) {
        Loan loan = loanRepository.findByIdAndOwner(loanId, userId)
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
    public List<LoanResponse> overdue(Long userId) {
        return loanRepository.findOverdueForOwner(LocalDate.now(), userId).stream()
            .filter(loan -> loan.getItem().getDeletedAt() == null) // Filtre eklendi
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public LoanResponse activeLoan(Long userId, Long itemId) {
        Loan loan = loanRepository.findActiveLoanForOwner(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active loan"));
        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> list(Long userId, LoanStatus status) {
        List<Loan> loans = status == null
            ? loanRepository.findAllForOwner(userId)
            : loanRepository.findByStatusForOwner(status, userId);

        return loans.stream()
            // SADECE SİLİNMEMİŞ ÜRÜNLERİ FİLTRELE:
            .filter(loan -> loan.getItem().getDeletedAt() == null)
            .map(this::toResponse)
            .toList();
    }

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
            loan.getId(),
            loan.getItem().getId(),
            loan.getItem().getTitle(),
            loan.getItem().getType(),
            loan.getToWhom(),
            loan.getStartDate(),
            loan.getDueDate(),
            loan.getReturnedAt(),
            loan.getStatus()
        );
    }
}
