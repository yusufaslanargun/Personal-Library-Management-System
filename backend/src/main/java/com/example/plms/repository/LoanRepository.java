package com.example.plms.repository;

import com.example.plms.domain.Loan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findFirstByItemIdAndReturnedAtIsNull(Long itemId);
    List<Loan> findByReturnedAtIsNullAndDueDateBefore(LocalDate date);
    List<Loan> findByItemIdIn(List<Long> itemIds);

    @Query("""
        select l from Loan l
        join l.item i
        where i.owner.id = :ownerId
          and l.item.id = :itemId
          and l.returnedAt is null
        """)
    Optional<Loan> findActiveLoanForOwner(@Param("itemId") Long itemId, @Param("ownerId") Long ownerId);

    @Query("""
        select l from Loan l
        join l.item i
        where i.owner.id = :ownerId
          and l.id = :loanId
        """)
    Optional<Loan> findByIdAndOwner(@Param("loanId") Long loanId, @Param("ownerId") Long ownerId);

    @Query("""
        select l from Loan l
        join l.item i
        where i.owner.id = :ownerId
          and l.returnedAt is null
          and l.dueDate < :date
        """)
    List<Loan> findOverdueForOwner(@Param("date") LocalDate date, @Param("ownerId") Long ownerId);
}
