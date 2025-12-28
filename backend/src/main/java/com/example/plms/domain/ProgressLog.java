package com.example.plms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "progress_log")
public class ProgressLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private MediaItem item;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "page_or_minute", nullable = false)
    private Integer pageOrMinute;

    @Column(nullable = false)
    private Integer percent;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected ProgressLog() {
    }

    public ProgressLog(MediaItem item, LocalDate logDate, Integer durationMinutes, Integer pageOrMinute, Integer percent) {
        this.item = item;
        this.logDate = logDate;
        this.durationMinutes = durationMinutes;
        this.pageOrMinute = pageOrMinute;
        this.percent = percent;
    }

    @PrePersist
    void onCreate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public MediaItem getItem() {
        return item;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Integer getPageOrMinute() {
        return pageOrMinute;
    }

    public Integer getPercent() {
        return percent;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
