package com.example.plms.repository;

import com.example.plms.domain.ListItem;
import com.example.plms.domain.ListItemId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListItemRepository extends JpaRepository<ListItem, ListItemId> {
    List<ListItem> findByIdListIdOrderByPosition(Long listId);
    boolean existsByIdListIdAndIdItemId(Long listId, Long itemId);
}
