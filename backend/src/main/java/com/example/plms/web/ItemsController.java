package com.example.plms.web;

import com.example.plms.service.ItemService;
import com.example.plms.web.dto.ItemCreateRequest;
import com.example.plms.web.dto.ItemResponse;
import com.example.plms.web.dto.ItemUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
    public ItemResponse create(@Valid @RequestBody ItemCreateRequest request) {
        return itemService.createManual(request);
    }

    @GetMapping
    public List<ItemResponse> listActive() {
        return itemService.listActive();
    }

    @GetMapping("/trash")
    public List<ItemResponse> listTrash() {
        return itemService.listTrash();
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable Long id) {
        return itemService.getItem(id);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @RequestBody ItemUpdateRequest request) {
        return itemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@PathVariable Long id) {
        itemService.softDelete(id);
    }

    @PostMapping("/{id}/restore")
    public ItemResponse restore(@PathVariable Long id) {
        itemService.restore(id);
        return itemService.getItemIncludingDeleted(id);
    }
}
