package com.example.plms.repository;

import com.example.plms.domain.SyncOutboxEntry;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncOutboxRepository extends JpaRepository<SyncOutboxEntry, Long> {
    List<SyncOutboxEntry> findByUserIdAndQueuedAtAfterOrderByQueuedAtAsc(Long userId, OffsetDateTime since);
    List<SyncOutboxEntry> findByUserId(Long userId);
}
