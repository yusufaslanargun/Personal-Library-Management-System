package com.example.plms.external;

import com.example.plms.config.ExternalProviderProperties;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OmdbClient {
    private static final Logger log = LoggerFactory.getLogger(OmdbClient.class);

    private final RestTemplate restTemplate;
    private final ExternalProviderProperties.Provider props;
    private final SimpleRateLimiter rateLimiter;

    public OmdbClient(RestTemplate restTemplate, ExternalProviderProperties properties) {
        this.restTemplate = restTemplate;
        this.props = properties.getOmdb();
        this.rateLimiter = new SimpleRateLimiter(props.getRateLimitMs());
    }

    public ExternalMetadata lookupByTitle(String title) {
        if (props.isMock()) {
            return mockMetadata("mock-imdb-id", title);
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            return null;
        }
        String url = props.getBaseUrl() + "/?t=" + encode(title) + "&apikey=" + props.getApiKey();
        rateLimiter.acquire();
        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            return toMetadata(response);
        } catch (RestClientException ex) {
            log.warn("OMDb lookup failed: {}", ex.getMessage());
            throw ex;
        }
    }

    public ExternalMetadata fetchById(String imdbId) {
        if (props.isMock()) {
            return mockMetadata(imdbId, "Mock DVD");
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            return null;
        }
        String url = props.getBaseUrl() + "/?i=" + encode(imdbId) + "&apikey=" + props.getApiKey();
        rateLimiter.acquire();
        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            return toMetadata(response);
        } catch (RestClientException ex) {
            log.warn("OMDb lookup failed: {}", ex.getMessage());
            throw ex;
        }
    }

    private String encode(String value) {
        return value.replace(" ", "+");
    }

    private Integer parseYear(String value) {
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.substring(0, 4));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseRuntime(String value) {
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value)) {
            return null;
        }
        String[] parts = value.split(" ");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseRating(String value) {
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> toList(String value) {
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value)) {
            return Collections.emptyList();
        }
        return List.of(value.split(",\\s*"));
    }

    private ExternalMetadata toMetadata(Map<?, ?> response) {
        if (response == null || "False".equalsIgnoreCase(String.valueOf(response.get("Response")))) {
            return null;
        }
        String imdbId = String.valueOf(response.get("imdbID"));
        Integer year = parseYear(String.valueOf(response.get("Year")));
        Integer runtime = parseRuntime(String.valueOf(response.get("Runtime")));
        String director = String.valueOf(response.get("Director"));
        String plot = String.valueOf(response.get("Plot"));
        String urlValue = String.valueOf(response.get("Website"));
        BigDecimal rating = parseRating(String.valueOf(response.get("imdbRating")));
        return new ExternalMetadata(imdbId, String.valueOf(response.get("Title")),
            toList(String.valueOf(response.get("Actors"))), director, runtime, year, plot, rating, urlValue);
    }

    private ExternalMetadata mockMetadata(String imdbId, String title) {
        return new ExternalMetadata(
            imdbId,
            title,
            List.of("Mock Actor"),
            "Mock Director",
            120,
            2022,
            "Mock plot for testing.",
            BigDecimal.valueOf(4.1),
            "https://example.com/mock"
        );
    }
}
