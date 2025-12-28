package com.example.plms.repository;

import com.example.plms.domain.SyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncStateRepository extends JpaRepository<SyncState, Integer> {
}
