package com.example.plms.external;

import com.example.plms.config.ExternalProviderProperties;
import com.example.plms.config.ExternalProviderProperties.Provider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GoogleBooksClientTest {
    private MockWebServer server;

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    @Test
    void parsesLookupResponse() throws Exception {
        String body = """
            {"items": [{"id": "abc",
              "volumeInfo": {
                "title": "Test Book",
                "authors": ["Author One", "Author Two"],
                "publisher": "Pub",
                "pageCount": 123,
                "publishedDate": "2020-01-01",
                "description": "Desc",
                "infoLink": "http://example.com",
                "averageRating": 4.5
              }}]}
            """;
        server.enqueue(new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));

        ExternalProviderProperties props = new ExternalProviderProperties();
        Provider provider = new Provider();
        provider.setBaseUrl(server.url("").toString());
        provider.setApiKey("");
        props.setGoogleBooks(provider);

        GoogleBooksClient client = new GoogleBooksClient(new RestTemplate(), props);
        List<ExternalBookCandidate> results = client.lookupByIsbn("123");

        assertFalse(results.isEmpty());
        assertEquals("GOOGLE_BOOKS", results.get(0).provider());
        assertEquals("Test Book", results.get(0).title());
    }
}
