package com.example.plms.service;

import com.example.plms.domain.ListItem;
import com.example.plms.domain.ListItemId;
import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaList;
import com.example.plms.repository.ListItemRepository;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.repository.MediaListRepository;
import com.example.plms.web.dto.ListItemResponse;
import com.example.plms.web.dto.MediaListResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListService {
    private final MediaListRepository listRepository;
    private final ListItemRepository listItemRepository;
    private final MediaItemRepository itemRepository;

    public ListService(MediaListRepository listRepository, ListItemRepository listItemRepository, MediaItemRepository itemRepository) {
        this.listRepository = listRepository;
        this.listItemRepository = listItemRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public MediaListResponse create(String name) {
        MediaList list = new MediaList(name.trim());
        MediaList saved = listRepository.save(list);
        return new MediaListResponse(saved.getId(), saved.getName(), List.of());
    }

    @Transactional
    public MediaListResponse update(Long id, String name) {
        MediaList list = listRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        list.setName(name.trim());
        return toResponse(list);
    }

    @Transactional
    public void delete(Long id) {
        MediaList list = listRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        listRepository.delete(list);
    }

    @Transactional(readOnly = true)
    public List<MediaListResponse> findAll() {
        return listRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MediaListResponse get(Long id) {
        MediaList list = listRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        return toResponse(list);
    }

    @Transactional
    public MediaListResponse addItem(Long listId, Long itemId, Integer priority) {
        if (listItemRepository.existsByIdListIdAndIdItemId(listId, itemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item already in list");
        }
        MediaList list = listRepository.findById(listId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNull(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        int nextPosition = listItemRepository.findByIdListIdOrderByPosition(listId).size();
        ListItem listItem = new ListItem(list, item, nextPosition, priority);
        listItemRepository.save(listItem);
        return toResponse(list);
    }

    @Transactional
    public MediaListResponse removeItem(Long listId, Long itemId) {
        ListItemId id = new ListItemId(listId, itemId);
        if (!listItemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "List item not found");
        }
        listItemRepository.deleteById(id);
        reorderInternal(listId, null);
        MediaList list = listRepository.findById(listId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        return toResponse(list);
    }

    @Transactional
    public MediaListResponse reorder(Long listId, List<Long> itemIds) {
        reorderInternal(listId, itemIds);
        MediaList list = listRepository.findById(listId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        return toResponse(list);
    }

    private void reorderInternal(Long listId, List<Long> itemIds) {
        List<ListItem> items = listItemRepository.findByIdListIdOrderByPosition(listId);
        if (items.isEmpty()) {
            return;
        }
        List<Long> orderedIds;
        if (itemIds == null) {
            orderedIds = items.stream().map(item -> item.getId().getItemId()).toList();
        } else {
            Set<Long> unique = new HashSet<>(itemIds);
            if (unique.size() != itemIds.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate item ids in reorder request");
            }
            if (itemIds.size() != items.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reorder list must include all items");
            }
            orderedIds = itemIds;
        }
        int position = 0;
        for (Long id : orderedIds) {
            ListItem item = items.stream()
                .filter(it -> it.getId().getItemId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item not in list"));
            item.setPosition(position++);
        }
        listItemRepository.saveAll(items);
    }

    private MediaListResponse toResponse(MediaList list) {
        List<ListItem> items = listItemRepository.findByIdListIdOrderByPosition(list.getId());
        List<ListItemResponse> responses = new ArrayList<>();
        for (ListItem item : items) {
            responses.add(new ListItemResponse(
                item.getItem().getId(),
                item.getItem().getTitle(),
                item.getPosition(),
                item.getPriority()
            ));
        }
        return new MediaListResponse(list.getId(), list.getName(), responses);
    }
}
