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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "external_link")
public class ExternalLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private MediaItem item;

    @Column(nullable = false)
    private String provider;

    private String externalId;

    private String url;

    private BigDecimal rating;

    private String summary;

    private OffsetDateTime lastSyncAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected ExternalLink() {
    }

    public ExternalLink(MediaItem item, String provider) {
        this.item = item;
        this.provider = provider;
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

    public void setItem(MediaItem item) {
        this.item = item;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public OffsetDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(OffsetDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
