package com.example.plms.web;

import com.example.plms.domain.MediaType;
import com.example.plms.web.dto.BookInfoRequest;
import com.example.plms.web.dto.AuthRegisterRequest;
import com.example.plms.web.dto.AuthResponse;
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
        String token = registerAndLogin();
        HttpHeaders headers = authHeaders(token);

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
        ItemResponse item = restTemplate.exchange("/items", HttpMethod.POST, new HttpEntity<>(request, headers), ItemResponse.class).getBody();
        assertNotNull(item);

        ResponseEntity<MediaListResponse> listResponse = restTemplate.exchange(
            "/lists", HttpMethod.POST, new HttpEntity<>(new ListRequest("My List"), headers), MediaListResponse.class);
        assertEquals(HttpStatus.CREATED, listResponse.getStatusCode());
        MediaListResponse list = listResponse.getBody();
        assertNotNull(list);

        restTemplate.exchange(
            "/lists/" + list.id() + "/items", HttpMethod.POST, new HttpEntity<>(new ListItemRequest(item.id(), 0), headers),
            MediaListResponse.class);
        MediaListResponse updated = restTemplate.exchange(
            "/lists/" + list.id(), HttpMethod.GET, new HttpEntity<>(headers), MediaListResponse.class).getBody();
        assertNotNull(updated);
        assertEquals(1, updated.items().size());

        restTemplate.exchange(
            "/lists/" + list.id() + "/items/reorder", HttpMethod.POST,
            new HttpEntity<>(new ReorderRequest(List.of(item.id())), headers),
            MediaListResponse.class);
    }

    private String registerAndLogin() {
        AuthRegisterRequest request = new AuthRegisterRequest("list@example.com", "password123", "List Tester");
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
