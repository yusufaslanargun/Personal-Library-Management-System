package com.example.plms.web;

import com.example.plms.domain.MediaType;
import com.example.plms.web.dto.BookInfoRequest;
import com.example.plms.web.dto.ItemCreateRequest;
import com.example.plms.web.dto.ItemResponse;
import com.example.plms.web.dto.ListItemRequest;
import com.example.plms.web.dto.ListRequest;
import com.example.plms.web.dto.MediaListResponse;
import com.example.plms.web.dto.ReorderRequest;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ListIntegrationTest {
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
    void reordersListItems() {
        ItemCreateRequest request = new ItemCreateRequest(
            MediaType.BOOK,
            "List Book",
            2023,
            null,
            null,
            List.of(),
            new BookInfoRequest("111", 100, "Publisher", List.of("Author")),
            null
        );
        ItemResponse item = restTemplate.postForEntity("/items", request, ItemResponse.class).getBody();
        assertNotNull(item);

        ResponseEntity<MediaListResponse> listResponse = restTemplate.postForEntity("/lists", new ListRequest("My List"), MediaListResponse.class);
        assertEquals(HttpStatus.CREATED, listResponse.getStatusCode());
        MediaListResponse list = listResponse.getBody();
        assertNotNull(list);

        restTemplate.postForEntity("/lists/" + list.id() + "/items", new ListItemRequest(item.id(), 0), MediaListResponse.class);
        MediaListResponse updated = restTemplate.getForObject("/lists/" + list.id(), MediaListResponse.class);
        assertNotNull(updated);
        assertEquals(1, updated.items().size());

        restTemplate.postForEntity("/lists/" + list.id() + "/items/reorder", new ReorderRequest(List.of(item.id())), MediaListResponse.class);
    }
}
