package com.example.plms.web;

import com.example.plms.service.ListService;
import com.example.plms.web.dto.ListItemRequest;
import com.example.plms.web.dto.ListRequest;
import com.example.plms.web.dto.MediaListResponse;
import com.example.plms.web.dto.ReorderRequest;
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
@RequestMapping("/lists")
public class ListsController {
    private final ListService listService;

    public ListsController(ListService listService) {
        this.listService = listService;
    }

    @GetMapping
    public List<MediaListResponse> list() {
        return listService.findAll();
    }

    @GetMapping("/{id}")
    public MediaListResponse get(@PathVariable Long id) {
        return listService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaListResponse create(@Valid @RequestBody ListRequest request) {
        return listService.create(request.name());
    }

    @PutMapping("/{id}")
    public MediaListResponse update(@PathVariable Long id, @Valid @RequestBody ListRequest request) {
        return listService.update(id, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        listService.delete(id);
    }

    @PostMapping("/{id}/items")
    public MediaListResponse addItem(@PathVariable Long id, @Valid @RequestBody ListItemRequest request) {
        return listService.addItem(id, request.itemId(), request.priority());
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public MediaListResponse removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return listService.removeItem(id, itemId);
    }

    @PostMapping("/{id}/items/reorder")
    public MediaListResponse reorder(@PathVariable Long id, @Valid @RequestBody ReorderRequest request) {
        return listService.reorder(id, request.itemIds());
    }
}
