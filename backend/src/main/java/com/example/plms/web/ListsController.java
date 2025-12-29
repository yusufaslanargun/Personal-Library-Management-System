package com.example.plms.web;

import com.example.plms.service.ListService;
import com.example.plms.security.AuthenticatedUser;
import com.example.plms.web.dto.ListItemRequest;
import com.example.plms.web.dto.ListRequest;
import com.example.plms.web.dto.MediaListResponse;
import com.example.plms.web.dto.ReorderRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lists")
public class ListsController {
    private final ListService listService;

    public ListsController(ListService listService) {
        this.listService = listService;
    }

    @GetMapping
    public List<MediaListResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return listService.findAll(user.id());
    }

    @GetMapping("/{id}")
    public MediaListResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return listService.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaListResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid @RequestBody ListRequest request) {
        return listService.create(user.id(), request.name());
    }

    @PutMapping("/{id}")
    public MediaListResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                    @PathVariable Long id, @Valid @RequestBody ListRequest request) {
        return listService.update(user.id(), id, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        listService.delete(user.id(), id);
    }

    @PostMapping("/{id}/items")
    public MediaListResponse addItem(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable Long id, @Valid @RequestBody ListItemRequest request) {
        return listService.addItem(user.id(), id, request.itemId(), request.priority());
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public MediaListResponse removeItem(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable Long id, @PathVariable Long itemId) {
        return listService.removeItem(user.id(), id, itemId);
    }

    @PostMapping("/{id}/items/reorder")
    public MediaListResponse reorder(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable Long id, @Valid @RequestBody ReorderRequest request) {
        return listService.reorder(user.id(), id, request.itemIds());
    }
}
