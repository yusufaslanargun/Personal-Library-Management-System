package com.example.plms.repository;

import com.example.plms.domain.Loan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findFirstByItemIdAndReturnedAtIsNull(Long itemId);
    List<Loan> findByReturnedAtIsNullAndDueDateBefore(LocalDate date);
    Optional<Loan> findFirstByItemIdAndReturnedAtIsNullAndItemOwner_Id(Long itemId, Long ownerId);
    List<Loan> findByReturnedAtIsNullAndDueDateBeforeAndItemOwner_Id(LocalDate date, Long ownerId);
    Optional<Loan> findByIdAndItemOwner_Id(Long loanId, Long ownerId);
    List<Loan> findByItemIdIn(List<Long> itemIds);
}
