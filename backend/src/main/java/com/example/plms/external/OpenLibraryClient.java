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
public class OpenLibraryClient {
    private static final Logger log = LoggerFactory.getLogger(OpenLibraryClient.class);

    private final RestTemplate restTemplate;
    private final ExternalProviderProperties.Provider props;
    private final SimpleRateLimiter rateLimiter;

    public OpenLibraryClient(RestTemplate restTemplate, ExternalProviderProperties properties) {
        this.restTemplate = restTemplate;
        this.props = properties.getOpenLibrary();
        this.rateLimiter = new SimpleRateLimiter(props.getRateLimitMs());
    }

    public List<ExternalBookCandidate> lookupByIsbn(String isbn) {
        if (props.isMock()) {
            return List.of(new ExternalBookCandidate(
                "OPEN_LIBRARY",
                "OL-MOCK-ISBN",
                "Mock Open Library Book",
                List.of("Mock Author"),
                "Mock Publisher",
                280,
                2023,
                "Mock Open Library description.",
                "https://openlibrary.org",
                BigDecimal.valueOf(4.0)
            ));
        }
        String bibKey = "ISBN:" + isbn;
        String url = props.getBaseUrl() + "/api/books?bibkeys=" + bibKey + "&format=json&jscmd=data";
        rateLimiter.acquire();
        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey(bibKey)) {
                return Collections.emptyList();
            }
            Map<?, ?> data = (Map<?, ?>) response.get(bibKey);
            ExternalBookCandidate candidate = toCandidate(isbn, data);
            return candidate == null ? Collections.emptyList() : List.of(candidate);
        } catch (RestClientException ex) {
            log.warn("Open Library lookup failed: {}", ex.getMessage());
            throw ex;
        }
    }

    public ExternalMetadata fetchById(String externalId) {
        if (props.isMock()) {
            return new ExternalMetadata(
                externalId,
                "Mock Open Library Book",
                List.of("Mock Author"),
                "Mock Publisher",
                280,
                2023,
                "Mock Open Library description.",
                BigDecimal.valueOf(4.0),
                "https://openlibrary.org"
            );
        }
        String bibKey = toBibKey(externalId);
        if (bibKey == null) {
            return null;
        }
        String url = props.getBaseUrl() + "/api/books?bibkeys=" + bibKey + "&format=json&jscmd=data";
        rateLimiter.acquire();
        Map<?, ?> response = restTemplate.getForObject(url, Map.class);
        if (response == null || !response.containsKey(bibKey)) {
            return null;
        }
        Map<?, ?> data = (Map<?, ?>) response.get(bibKey);
        ExternalBookCandidate candidate = toCandidate(externalId, data);
        if (candidate == null) {
            return null;
        }
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

    private ExternalBookCandidate toCandidate(String isbnOrId, Map<?, ?> data) {
        if (data == null) {
            return null;
        }
        String title = Objects.toString(data.get("title"), null);
        String subtitle = Objects.toString(data.get("subtitle"), null);
        if (subtitle != null && !subtitle.isBlank()) {
            title = title == null ? subtitle : title + ": " + subtitle;
        }
        List<String> authors = toAuthors(data.get("authors"));
        String publisher = toPublisher(data.get("publishers"));
        Integer pageCount = toInteger(data.get("number_of_pages"));
        Integer year = parseYear(Objects.toString(data.get("publish_date"), null));
        String description = toDescription(data.get("description"));
        String externalId = toExternalId(data, isbnOrId);
        String infoLink = toInfoLink(data.get("url"), externalId, isbnOrId);
        return new ExternalBookCandidate(
            "OPEN_LIBRARY",
            externalId,
            title,
            authors,
            publisher,
            pageCount,
            year,
            description,
            infoLink,
            null
        );
    }

    private List<String> toAuthors(Object value) {
        if (value instanceof List<?> list) {
            List<String> results = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String name = Objects.toString(map.get("name"), null);
                    if (name != null && !name.isBlank()) {
                        results.add(name);
                    }
                } else if (item != null) {
                    results.add(item.toString());
                }
            }
            return results;
        }
        return Collections.emptyList();
    }

    private String toPublisher(Object value) {
        if (value instanceof List<?> list && !list.isEmpty()) {
            Object item = list.get(0);
            if (item instanceof Map<?, ?> map) {
                return Objects.toString(map.get("name"), null);
            }
            return item == null ? null : item.toString();
        }
        return null;
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

    private Integer parseYear(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (int i = 0; i <= value.length() - 4; i++) {
            String part = value.substring(i, i + 4);
            if (part.chars().allMatch(Character::isDigit)) {
                try {
                    return Integer.parseInt(part);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private String toDescription(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object inner = map.get("value");
            return inner == null ? null : inner.toString();
        }
        return value == null ? null : value.toString();
    }

    private String toExternalId(Map<?, ?> data, String fallback) {
        if (data == null) {
            return fallback;
        }
        Object identifiers = data.get("identifiers");
        if (identifiers instanceof Map<?, ?> map) {
            Object openLibrary = map.get("openlibrary");
            if (openLibrary instanceof List<?> list && !list.isEmpty()) {
                Object value = list.get(0);
                if (value != null) {
                    return value.toString();
                }
            }
        }
        String url = Objects.toString(data.get("url"), null);
        String parsed = parseIdFromUrl(url);
        return parsed == null ? fallback : parsed;
    }

    private String parseIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int idx = url.lastIndexOf('/');
        if (idx == -1 || idx == url.length() - 1) {
            return null;
        }
        return url.substring(idx + 1);
    }

    private String toInfoLink(Object urlValue, String externalId, String fallback) {
        String url = urlValue == null ? null : urlValue.toString();
        if (url != null && !url.isBlank()) {
            return url.startsWith("http") ? url : props.getBaseUrl() + url;
        }
        if (externalId != null && externalId.startsWith("OL")) {
            return props.getBaseUrl() + "/books/" + externalId;
        }
        return props.getBaseUrl() + "/isbn/" + fallback;
    }

    private String toBibKey(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return null;
        }
        if (externalId.startsWith("ISBN:") || externalId.startsWith("OLID:")) {
            return externalId;
        }
        if (externalId.startsWith("OL")) {
            return "OLID:" + externalId;
        }
        return "ISBN:" + externalId;
    }
}
