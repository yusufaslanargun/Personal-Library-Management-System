package com.example.plms.service;

import com.example.plms.config.SyncProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SyncShutdownHook {
    private static final Logger log = LoggerFactory.getLogger(SyncShutdownHook.class);

    private final SyncService syncService;
    private final SyncProperties properties;

    public SyncShutdownHook(SyncService syncService, SyncProperties properties) {
        this.syncService = syncService;
        this.properties = properties;
    }

    @PreDestroy
    public void flush() {
        if (!properties.isEnabled() || !properties.isFlushOnShutdown()) {
            return;
        }
        try {
            syncService.flushAllUsers();
        } catch (Exception ex) {
            log.warn("Sync flush on shutdown failed: {}", ex.getMessage());
        }
    }
}
