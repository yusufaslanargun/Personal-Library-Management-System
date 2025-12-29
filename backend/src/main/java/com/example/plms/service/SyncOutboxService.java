package com.example.plms.service;

import com.example.plms.domain.SyncOutboxEntry;
import com.example.plms.repository.SyncOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncOutboxService {
    public static final String OP_DELETE = "DELETE";

    private final SyncOutboxRepository repository;

    public SyncOutboxService(SyncOutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void enqueueDelete(Long userId, String entityType, String entityKey) {
        repository.save(new SyncOutboxEntry(entityType, entityKey, OP_DELETE, userId));
    }
}
