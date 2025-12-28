package com.example.plms.web;

import com.example.plms.domain.MediaStatus;
import com.example.plms.domain.MediaType;
import com.example.plms.service.SearchService;
import com.example.plms.web.dto.SearchResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/items/search")
    public SearchResponse search(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) MediaType type,
        @RequestParam(required = false) MediaStatus status,
        @RequestParam(required = false) List<String> tags,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) String cast,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) String condition,
        @RequestParam(required = false) String location,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return searchService.search(query, type, status, tags, author, cast, year, condition, location, page, size);
    }
}
