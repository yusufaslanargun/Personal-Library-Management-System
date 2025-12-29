package com.example.plms.repository;

import com.example.plms.domain.SyncState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncStateRepository extends JpaRepository<SyncState, Integer> {
    Optional<SyncState> findByUserId(Long userId);
    List<SyncState> findByUserIdIsNotNull();
}
