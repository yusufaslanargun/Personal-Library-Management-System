package com.example.plms.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "dvd_info")
public class DvdInfo {
    @Id
    private Long itemId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "item_id")
    private MediaItem item;

    private Integer runtime;

    private String director;

    @ElementCollection
    @CollectionTable(name = "dvd_cast", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "member")
    private Set<String> cast = new LinkedHashSet<>();

    protected DvdInfo() {
    }

    public DvdInfo(MediaItem item) {
        this.item = item;
    }

    public Long getItemId() {
        return itemId;
    }

    public MediaItem getItem() {
        return item;
    }

    public void setItem(MediaItem item) {
        this.item = item;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public Set<String> getCast() {
        return cast;
    }

    public void setCast(List<String> cast) {
        this.cast = cast == null ? new LinkedHashSet<>() : new LinkedHashSet<>(cast);
    }
}
