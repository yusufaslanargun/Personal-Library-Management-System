package com.example.plms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "sync_outbox")
public class SyncOutboxEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String entityType;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 200)
    private String entityKey;

    @Column(nullable = false, length = 12)
    private String operation;

    @Column(nullable = false)
    private OffsetDateTime queuedAt;

    protected SyncOutboxEntry() {
    }

    public SyncOutboxEntry(String entityType, String entityKey, String operation, Long userId) {
        this.entityType = entityType;
        this.entityKey = entityKey;
        this.operation = operation;
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        queuedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEntityKey() {
        return entityKey;
    }

    public String getOperation() {
        return operation;
    }

    public OffsetDateTime getQueuedAt() {
        return queuedAt;
    }
}
