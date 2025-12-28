package com.example.plms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ListItemId implements Serializable {
    @Column(name = "list_id")
    private Long listId;

    @Column(name = "item_id")
    private Long itemId;

    protected ListItemId() {
    }

    public ListItemId(Long listId, Long itemId) {
        this.listId = listId;
        this.itemId = itemId;
    }

    public Long getListId() {
        return listId;
    }

    public Long getItemId() {
        return itemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ListItemId that = (ListItemId) o;
        return Objects.equals(listId, that.listId) && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listId, itemId);
    }
}
