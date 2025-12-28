package com.example.plms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "sync_state")
public class SyncState {
    @Id
    private Integer id = 1;

    private OffsetDateTime lastSyncAt;

    private String lastStatus;

    @Column(nullable = false)
    private Integer lastConflictCount = 0;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Integer getId() {
        return id;
    }

    public OffsetDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(OffsetDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public Integer getLastConflictCount() {
        return lastConflictCount;
    }

    public void setLastConflictCount(Integer lastConflictCount) {
        this.lastConflictCount = lastConflictCount;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
