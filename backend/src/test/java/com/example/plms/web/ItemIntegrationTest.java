package com.example.plms.web;

import com.example.plms.domain.MediaType;
import com.example.plms.web.dto.BookInfoRequest;
import com.example.plms.web.dto.DvdInfoRequest;
import com.example.plms.web.dto.ItemCreateRequest;
import com.example.plms.web.dto.ItemResponse;
import com.example.plms.web.dto.SearchResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ItemIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createsSearchesAndSoftDeletes() {
        ItemCreateRequest request = new ItemCreateRequest(
            MediaType.BOOK,
            "Integration Book",
            2024,
            "New",
            "Shelf A",
            List.of("integration"),
            new BookInfoRequest("123", 100, "Publisher", List.of("Author One")),
            null
        );

        ResponseEntity<ItemResponse> createResponse = restTemplate.postForEntity("/items", request, ItemResponse.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        ItemResponse created = createResponse.getBody();
        assertNotNull(created);
        assertNotNull(created.id());

        ItemCreateRequest dvdRequest = new ItemCreateRequest(
            MediaType.DVD,
            "Integration DVD",
            2023,
            "Used",
            "Shelf B",
            List.of("integration"),
            null,
            new DvdInfoRequest(120, "Director", List.of("Actor One"))
        );
        ResponseEntity<ItemResponse> dvdResponse = restTemplate.postForEntity("/items", dvdRequest, ItemResponse.class);
        assertEquals(HttpStatus.CREATED, dvdResponse.getStatusCode());

        SearchResponse search = restTemplate.getForObject("/items/search?query=Integration", SearchResponse.class);
        assertNotNull(search);
        assertTrue(search.total() >= 1);

        SearchResponse authorSearch = restTemplate.getForObject("/items/search?author=Author%20One", SearchResponse.class);
        assertNotNull(authorSearch);
        assertTrue(authorSearch.items().stream().anyMatch(item -> "Integration Book".equals(item.title())));

        SearchResponse castSearch = restTemplate.getForObject("/items/search?cast=Actor%20One", SearchResponse.class);
        assertNotNull(castSearch);
        assertTrue(castSearch.items().stream().anyMatch(item -> "Integration DVD".equals(item.title())));

        restTemplate.delete("/items/" + created.id());
        ItemResponse[] active = restTemplate.getForObject("/items", ItemResponse[].class);
        ItemResponse[] trash = restTemplate.getForObject("/items/trash", ItemResponse[].class);
        assertNotNull(active);
        assertNotNull(trash);
        assertTrue(trash.length >= 1);
    }
}
