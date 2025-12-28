package com.example.plms.web;

import com.example.plms.domain.MediaType;
import com.example.plms.web.dto.BookInfoRequest;
import com.example.plms.web.dto.DvdInfoRequest;
import com.example.plms.web.dto.AuthRegisterRequest;
import com.example.plms.web.dto.AuthResponse;
import com.example.plms.web.dto.ItemCreateRequest;
import com.example.plms.web.dto.ItemResponse;
import com.example.plms.web.dto.SearchResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
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
        String token = registerAndLogin();
        HttpHeaders headers = authHeaders(token);

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

        ResponseEntity<ItemResponse> createResponse = restTemplate.exchange(
            "/items", HttpMethod.POST, new HttpEntity<>(request, headers), ItemResponse.class);
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
        ResponseEntity<ItemResponse> dvdResponse = restTemplate.exchange(
            "/items", HttpMethod.POST, new HttpEntity<>(dvdRequest, headers), ItemResponse.class);
        assertEquals(HttpStatus.CREATED, dvdResponse.getStatusCode());

        SearchResponse search = restTemplate.exchange(
            "/items/search?query=Integration", HttpMethod.GET, new HttpEntity<>(headers), SearchResponse.class).getBody();
        assertNotNull(search);
        assertTrue(search.total() >= 1);

        SearchResponse authorSearch = restTemplate.exchange(
            "/items/search?author=Author%20One", HttpMethod.GET, new HttpEntity<>(headers), SearchResponse.class).getBody();
        assertNotNull(authorSearch);
        assertTrue(authorSearch.items().stream().anyMatch(item -> "Integration Book".equals(item.title())));

        SearchResponse castSearch = restTemplate.exchange(
            "/items/search?cast=Actor%20One", HttpMethod.GET, new HttpEntity<>(headers), SearchResponse.class).getBody();
        assertNotNull(castSearch);
        assertTrue(castSearch.items().stream().anyMatch(item -> "Integration DVD".equals(item.title())));

        restTemplate.exchange("/items/" + created.id(), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        ItemResponse[] active = restTemplate.exchange(
            "/items", HttpMethod.GET, new HttpEntity<>(headers), ItemResponse[].class).getBody();
        ItemResponse[] trash = restTemplate.exchange(
            "/items/trash", HttpMethod.GET, new HttpEntity<>(headers), ItemResponse[].class).getBody();
        assertNotNull(active);
        assertNotNull(trash);
        assertTrue(trash.length >= 1);
    }

    private String registerAndLogin() {
        AuthRegisterRequest request = new AuthRegisterRequest("tester@example.com", "password123", "Tester");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/auth/register", request, AuthResponse.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody().token();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
