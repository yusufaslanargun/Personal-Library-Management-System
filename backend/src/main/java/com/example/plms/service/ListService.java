package com.example.plms.service;

import com.example.plms.domain.ListItem;
import com.example.plms.domain.ListItemId;
import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaList;
import com.example.plms.domain.AppUser;
import com.example.plms.repository.AppUserRepository;
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
    private final SyncOutboxService syncOutboxService;
    private final AppUserRepository userRepository;

    public ListService(MediaListRepository listRepository, ListItemRepository listItemRepository,
                       MediaItemRepository itemRepository, SyncOutboxService syncOutboxService,
                       AppUserRepository userRepository) {
        this.listRepository = listRepository;
        this.listItemRepository = listItemRepository;
        this.itemRepository = itemRepository;
        this.syncOutboxService = syncOutboxService;
        this.userRepository = userRepository;
    }

    @Transactional
    public MediaListResponse create(Long userId, String name) {
        MediaList list = new MediaList(name.trim());
        AppUser owner = userRepository.getReferenceById(userId);
        list.setOwner(owner);
        MediaList saved = listRepository.save(list);
        return new MediaListResponse(saved.getId(), saved.getName(), List.of());
    }

    @Transactional
    public MediaListResponse update(Long userId, Long id, String name) {
        MediaList list = listRepository.findByIdAndOwner_Id(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        list.setName(name.trim());
        return toResponse(list);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        MediaList list = listRepository.findByIdAndOwner_Id(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        listRepository.delete(list);
        syncOutboxService.enqueueDelete(userId, "LIST", String.valueOf(id));
    }

    @Transactional(readOnly = true)
    public List<MediaListResponse> findAll(Long userId) {
        return listRepository.findAllByOwner_Id(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MediaListResponse get(Long userId, Long id) {
        MediaList list = listRepository.findByIdAndOwner_Id(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        return toResponse(list);
    }

    @Transactional
    public MediaListResponse addItem(Long userId, Long listId, Long itemId, Integer priority) {
        if (listItemRepository.existsByIdListIdAndIdItemId(listId, itemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item already in list");
        }
        MediaList list = listRepository.findByIdAndOwner_Id(listId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        int nextPosition = listItemRepository.findByIdListIdOrderByPosition(listId).size();
        ListItem listItem = new ListItem(list, item, nextPosition, priority);
        listItemRepository.save(listItem);
        return toResponse(list);
    }

    @Transactional
    public MediaListResponse removeItem(Long userId, Long listId, Long itemId) {
        listRepository.findByIdAndOwner_Id(listId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        ListItemId id = new ListItemId(listId, itemId);
        if (!listItemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "List item not found");
        }
        listItemRepository.deleteById(id);
        syncOutboxService.enqueueDelete(userId, "LIST_ITEM", listId + ":" + itemId);
        reorderInternal(listId, null);
        MediaList list = listRepository.findByIdAndOwner_Id(listId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        return toResponse(list);
    }

    @Transactional
    public MediaListResponse reorder(Long userId, Long listId, List<Long> itemIds) {
        listRepository.findByIdAndOwner_Id(listId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        reorderInternal(listId, itemIds);
        MediaList list = listRepository.findByIdAndOwner_Id(listId, userId)
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
            if (item.getItem().getDeletedAt() == null) {
                responses.add(new ListItemResponse(
                    item.getItem().getId(),
                    item.getItem().getTitle(),
                    item.getPosition(),
                    item.getPriority()
                ));
            }
        }
        return new MediaListResponse(list.getId(), list.getName(), responses);
    }
}
