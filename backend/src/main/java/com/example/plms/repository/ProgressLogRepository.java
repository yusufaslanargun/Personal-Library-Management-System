package com.example.plms.repository;

import com.example.plms.domain.ProgressLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgressLogRepository extends JpaRepository<ProgressLog, Long> {
    List<ProgressLog> findByItemIdOrderByLogDateDesc(Long itemId);
    List<ProgressLog> findByItemIdIn(List<Long> itemIds);
    Optional<ProgressLog> findFirstByItemIdOrderByLogDateDescIdDesc(Long itemId);

    @Query("""
        select p from ProgressLog p
        join p.item i
        where p.id = :logId
          and p.item.id = :itemId
          and i.owner.id = :ownerId
        """)
    Optional<ProgressLog> findByIdAndOwner(@Param("logId") Long logId,
                                          @Param("itemId") Long itemId,
                                          @Param("ownerId") Long ownerId);
}
