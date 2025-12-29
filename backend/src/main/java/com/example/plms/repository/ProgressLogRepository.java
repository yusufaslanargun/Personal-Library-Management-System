package com.example.plms.repository;

import com.example.plms.domain.ProgressLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressLogRepository extends JpaRepository<ProgressLog, Long> {
    List<ProgressLog> findByItemIdOrderByLogDateDesc(Long itemId);
    List<ProgressLog> findByItemIdIn(List<Long> itemIds);
}
