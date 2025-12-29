package com.example.plms.web;

import com.example.plms.service.ItemService;
import com.example.plms.security.AuthenticatedUser;
import com.example.plms.web.dto.ItemCreateRequest;
import com.example.plms.web.dto.ItemResponse;
import com.example.plms.web.dto.ItemUpdateRequest;
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
@RequestMapping("/items")
public class ItemsController {
    private final ItemService itemService;

    public ItemsController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody ItemCreateRequest request) {
        return itemService.createManual(user.id(), request);
    }

    @GetMapping
    public List<ItemResponse> listActive(@AuthenticationPrincipal AuthenticatedUser user) {
        return itemService.listActive(user.id());
    }

    @GetMapping("/trash")
    public List<ItemResponse> listTrash(@AuthenticationPrincipal AuthenticatedUser user) {
        return itemService.listTrash(user.id());
    }

    @GetMapping("/{id}")
    public ItemResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return itemService.getItem(user.id(), id);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable Long id, @RequestBody ItemUpdateRequest request) {
        return itemService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        itemService.softDelete(user.id(), id);
    }

    @PostMapping("/{id}/restore")
    public ItemResponse restore(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        itemService.restore(user.id(), id);
        return itemService.getItemIncludingDeleted(user.id(), id);
    }
}
