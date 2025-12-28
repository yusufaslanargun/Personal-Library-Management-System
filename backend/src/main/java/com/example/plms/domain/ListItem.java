package com.example.plms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "list_item")
public class ListItem {
    @EmbeddedId
    private ListItemId id;

    @ManyToOne
    @MapsId("listId")
    @JoinColumn(name = "list_id")
    private MediaList list;

    @ManyToOne
    @MapsId("itemId")
    @JoinColumn(name = "item_id")
    private MediaItem item;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    private Integer priority = 0;

    protected ListItem() {
    }

    public ListItem(MediaList list, MediaItem item, Integer position, Integer priority) {
        this.list = list;
        this.item = item;
        this.position = position;
        this.priority = priority == null ? 0 : priority;
        this.id = new ListItemId(list.getId(), item.getId());
    }

    public ListItemId getId() {
        return id;
    }

    public MediaList getList() {
        return list;
    }

    public MediaItem getItem() {
        return item;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
