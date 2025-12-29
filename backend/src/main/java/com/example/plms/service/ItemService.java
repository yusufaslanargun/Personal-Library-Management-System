package com.example.plms.service;

import com.example.plms.domain.BookInfo;
import com.example.plms.domain.DvdInfo;
import com.example.plms.domain.AppUser;
import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaType;
import com.example.plms.repository.AppUserRepository;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.web.dto.BookInfoRequest;
import com.example.plms.web.dto.DvdInfoRequest;
import com.example.plms.web.dto.ItemCreateRequest;
import com.example.plms.web.dto.ItemResponse;
import com.example.plms.web.dto.ItemUpdateRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ItemService {
    private final MediaItemRepository itemRepository;
    private final AppUserRepository userRepository;
    private final TagService tagService;
    private final ItemMapper itemMapper;

    public ItemService(MediaItemRepository itemRepository, AppUserRepository userRepository, TagService tagService,
                       ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.tagService = tagService;
        this.itemMapper = itemMapper;
    }

    @Transactional
    public ItemResponse createManual(Long userId, ItemCreateRequest request) {
        MediaItem item = new MediaItem(request.type(), request.title(), request.year());
        AppUser owner = userRepository.getReferenceById(userId);
        item.setOwner(owner);
        item.setCondition(request.condition());
        item.setLocation(request.location());
        item.setTags(tagService.resolveTags(request.tags()));

        if (request.type() == MediaType.BOOK) {
            applyBookInfo(item, request.bookInfo());
        } else if (request.type() == MediaType.DVD) {
            applyDvdInfo(item, request.dvdInfo());
        }

        MediaItem saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ItemResponse getItem(Long userId, Long id) {
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        return itemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemIncludingDeleted(Long userId, Long id) {
        MediaItem item = itemRepository.findByIdAndOwner_Id(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        return itemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> listActive(Long userId) {
        return itemRepository.findAllByDeletedAtIsNullAndOwner_Id(userId).stream().map(itemMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> listTrash(Long userId) {
        return itemRepository.findAllByDeletedAtIsNotNullAndOwner_Id(userId).stream().map(itemMapper::toResponse).toList();
    }

    @Transactional
    public ItemResponse update(Long userId, Long id, ItemUpdateRequest request) {
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        if (request.type() != null) {
            item.setType(request.type());
        }
        if (request.title() != null) {
            item.setTitle(request.title());
        }
        if (request.year() != null) {
            item.setYear(request.year());
        }
        if (request.condition() != null) {
            item.setCondition(request.condition());
        }
        if (request.location() != null) {
            item.setLocation(request.location());
        }
        if (request.tags() != null) {
            item.setTags(tagService.resolveTags(request.tags()));
        }

        if (item.getType() == MediaType.BOOK) {
            applyBookInfo(item, request.bookInfo());
            item.setDvdInfo(null);
        } else if (item.getType() == MediaType.DVD) {
            applyDvdInfo(item, request.dvdInfo());
            item.setBookInfo(null);
        }

        MediaItem saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }

    @Transactional
    public void softDelete(Long userId, Long id) {
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        item.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        itemRepository.save(item);
    }

    @Transactional
    public void restore(Long userId, Long id) {
        MediaItem item = itemRepository.findByIdAndOwner_Id(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        item.setDeletedAt(null);
        itemRepository.save(item);
    }

    private void applyBookInfo(MediaItem item, BookInfoRequest request) {
        if (request == null) {
            return;
        }
        BookInfo info = item.getBookInfo();
        if (info == null) {
            info = new BookInfo(item);
            item.setBookInfo(info);
        }
        info.setIsbn(request.isbn());
        info.setPages(request.pages());
        info.setPublisher(request.publisher());
        if (request.authors() != null) {
            info.setAuthors(request.authors());
            info.setAuthorsText(String.join(", ", request.authors()));
        }
        if (request.pages() != null) {
            item.setTotalValue(request.pages());
        }
    }

    private void applyDvdInfo(MediaItem item, DvdInfoRequest request) {
        if (request == null) {
            return;
        }
        DvdInfo info = item.getDvdInfo();
        if (info == null) {
            info = new DvdInfo(item);
            item.setDvdInfo(info);
        }
        info.setRuntime(request.runtime());
        info.setDirector(request.director());
        if (request.cast() != null) {
            info.setCast(request.cast());
        }
        if (request.runtime() != null) {
            item.setTotalValue(request.runtime());
        }
    }
}
