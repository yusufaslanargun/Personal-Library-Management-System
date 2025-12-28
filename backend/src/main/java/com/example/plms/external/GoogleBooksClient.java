package com.example.plms.external;

import com.example.plms.config.ExternalProviderProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleBooksClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleBooksClient.class);

    private final RestTemplate restTemplate;
    private final ExternalProviderProperties.Provider props;
    private final SimpleRateLimiter rateLimiter;

    public GoogleBooksClient(RestTemplate restTemplate, ExternalProviderProperties properties) {
        this.restTemplate = restTemplate;
        this.props = properties.getGoogleBooks();
        this.rateLimiter = new SimpleRateLimiter(props.getRateLimitMs());
    }

    public List<ExternalBookCandidate> lookupByIsbn(String isbn) {
        if (props.isMock()) {
            return List.of(new ExternalBookCandidate(
                "GOOGLE_BOOKS",
                "mock-volume-1",
                "Mock Book " + isbn,
                List.of("Mock Author"),
                "Mock Publisher",
                320,
                2024,
                "Mock description for testing.",
                "https://example.com/mock",
                BigDecimal.valueOf(4.2)
            ));
        }
        String url = props.getBaseUrl() + "/volumes?q=isbn:" + isbn + apiKeyQuery();
        rateLimiter.acquire();
        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("items")) {
                return Collections.emptyList();
            }
            List<?> items = (List<?>) response.get("items");
            List<ExternalBookCandidate> candidates = new ArrayList<>();
            for (Object item : items) {
                Map<?, ?> itemMap = (Map<?, ?>) item;
                String volumeId = Objects.toString(itemMap.get("id"), null);
                Map<?, ?> volumeInfo = (Map<?, ?>) itemMap.get("volumeInfo");
                if (volumeInfo == null) {
                    continue;
                }
                candidates.add(toCandidate(volumeId, volumeInfo));
            }
            return candidates;
        } catch (RestClientException ex) {
            log.warn("Google Books lookup failed: {}", ex.getMessage());
            throw ex;
        }
    }

    public ExternalMetadata fetchById(String volumeId) {
        if (props.isMock()) {
            return new ExternalMetadata(
                volumeId,
                "Mock Book",
                List.of("Mock Author"),
                "Mock Publisher",
                320,
                2024,
                "Mock description for testing.",
                BigDecimal.valueOf(4.2),
                "https://example.com/mock"
            );
        }
        String url = props.getBaseUrl() + "/volumes/" + volumeId + apiKeyQuery();
        rateLimiter.acquire();
        Map<?, ?> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            return null;
        }
        Map<?, ?> volumeInfo = (Map<?, ?>) response.get("volumeInfo");
        if (volumeInfo == null) {
            return null;
        }
        ExternalBookCandidate candidate = toCandidate(volumeId, volumeInfo);
        return new ExternalMetadata(
            candidate.externalId(),
            candidate.title(),
            candidate.authors(),
            candidate.publisher(),
            candidate.pageCount(),
            candidate.year(),
            candidate.description(),
            candidate.averageRating(),
            candidate.infoLink()
        );
    }

    private ExternalBookCandidate toCandidate(String volumeId, Map<?, ?> volumeInfo) {
        String title = Objects.toString(volumeInfo.get("title"), null);
        List<String> authors = toStringList(volumeInfo.get("authors"));
        String publisher = Objects.toString(volumeInfo.get("publisher"), null);
        Integer pageCount = toInteger(volumeInfo.get("pageCount"));
        Integer year = parseYear(Objects.toString(volumeInfo.get("publishedDate"), null));
        String description = Objects.toString(volumeInfo.get("description"), null);
        String infoLink = Objects.toString(volumeInfo.get("infoLink"), null);
        BigDecimal rating = toBigDecimal(volumeInfo.get("averageRating"));
        return new ExternalBookCandidate("GOOGLE_BOOKS", volumeId, title, authors, publisher,
            pageCount, year, description, infoLink, rating);
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> results = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    results.add(item.toString());
                }
            }
            return results;
        }
        return Collections.emptyList();
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private Integer parseYear(String publishedDate) {
        if (publishedDate == null || publishedDate.isBlank()) {
            return null;
        }
        String yearPart = publishedDate.length() >= 4 ? publishedDate.substring(0, 4) : publishedDate;
        try {
            return Integer.parseInt(yearPart);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String apiKeyQuery() {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            return "";
        }
        return "&key=" + props.getApiKey();
    }
}
