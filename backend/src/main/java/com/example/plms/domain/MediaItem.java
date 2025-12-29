package com.example.plms.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "media_item")
public class MediaItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer year;

    private String condition;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaStatus status = MediaStatus.AVAILABLE;

    private OffsetDateTime deletedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column(nullable = false)
    private Integer progressPercent = 0;

    @Column(nullable = false)
    private Integer progressValue = 0;

    @Column(nullable = false)
    private Integer totalValue = 0;

    @ManyToMany
    @JoinTable(
        name = "media_item_tag",
        joinColumns = @JoinColumn(name = "item_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToOne(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private BookInfo bookInfo;

    @OneToOne(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private DvdInfo dvdInfo;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExternalLink> externalLinks = new ArrayList<>();

    protected MediaItem() {
    }

    public MediaItem(MediaType type, String title, Integer year) {
        this.type = type;
        this.title = title;
        this.year = year;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public void setStatus(MediaStatus status) {
        this.status = status;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getProgressValue() {
        return progressValue;
    }

    public void setProgressValue(Integer progressValue) {
        this.progressValue = progressValue;
    }

    public Integer getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(Integer totalValue) {
        this.totalValue = totalValue;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public BookInfo getBookInfo() {
        return bookInfo;
    }

    public void setBookInfo(BookInfo bookInfo) {
        this.bookInfo = bookInfo;
        if (bookInfo != null) {
            bookInfo.setItem(this);
        }
    }

    public DvdInfo getDvdInfo() {
        return dvdInfo;
    }

    public void setDvdInfo(DvdInfo dvdInfo) {
        this.dvdInfo = dvdInfo;
        if (dvdInfo != null) {
            dvdInfo.setItem(this);
        }
    }

    public List<ExternalLink> getExternalLinks() {
        return externalLinks;
    }

    public void setExternalLinks(List<ExternalLink> externalLinks) {
        this.externalLinks = externalLinks;
    }
}
