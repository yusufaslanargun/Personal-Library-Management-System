package com.example.plms.repository;

import com.example.plms.domain.SyncOutboxEntry;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncOutboxRepository extends JpaRepository<SyncOutboxEntry, Long> {
    List<SyncOutboxEntry> findByQueuedAtAfterOrderByQueuedAtAsc(OffsetDateTime since);
}
