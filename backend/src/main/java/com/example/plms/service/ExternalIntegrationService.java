package com.example.plms.service;

import com.example.plms.domain.BookInfo;
import com.example.plms.domain.DvdInfo;
import com.example.plms.domain.ExternalLink;
import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaType;
import com.example.plms.domain.AppUser;
import com.example.plms.external.ExternalBookCandidate;
import com.example.plms.external.ExternalMetadata;
import com.example.plms.external.OmdbClient;
import com.example.plms.external.OpenLibraryClient;
import com.example.plms.repository.AppUserRepository;
import com.example.plms.repository.ExternalLinkRepository;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.web.dto.ExternalApplyRequest;
import com.example.plms.web.dto.ExternalDiffField;
import com.example.plms.web.dto.ExternalLinkRequest;
import com.example.plms.web.dto.IsbnConfirmRequest;
import com.example.plms.web.dto.ItemResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExternalIntegrationService {
    private static final String PROVIDER_OPEN_LIBRARY = "OPEN_LIBRARY";
    private static final String PROVIDER_OMDB = "OMDB";

    private final OpenLibraryClient openLibraryClient;
    private final OmdbClient omdbClient;
    private final MediaItemRepository itemRepository;
    private final ExternalLinkRepository externalLinkRepository;
    private final AppUserRepository userRepository;
    private final ItemMapper itemMapper;

    public ExternalIntegrationService(OpenLibraryClient openLibraryClient,
                                      OmdbClient omdbClient,
                                      MediaItemRepository itemRepository,
                                      ExternalLinkRepository externalLinkRepository,
                                      AppUserRepository userRepository,
                                      ItemMapper itemMapper) {
        this.openLibraryClient = openLibraryClient;
        this.omdbClient = omdbClient;
        this.itemRepository = itemRepository;
        this.externalLinkRepository = externalLinkRepository;
        this.userRepository = userRepository;
        this.itemMapper = itemMapper;
    }

    public List<ExternalBookCandidate> lookupIsbn(String isbn) {
        List<ExternalBookCandidate> candidates = new ArrayList<>();
        try {
            candidates.addAll(openLibraryClient.lookupByIsbn(isbn));
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "External provider error");
        }
        return candidates;
    }

    @Transactional
    public ItemResponse createFromIsbn(Long userId, IsbnConfirmRequest request) {
        MediaItem item = new MediaItem(MediaType.BOOK, request.title(),
            request.year() == null ? OffsetDateTime.now(ZoneOffset.UTC).getYear() : request.year());
        AppUser owner = userRepository.getReferenceById(userId);
        item.setOwner(owner);
        BookInfo info = new BookInfo(item);
        info.setIsbn(request.isbn());
        info.setPages(request.pageCount());
        info.setPublisher(request.publisher());
        if (request.authors() != null) {
            info.setAuthors(request.authors());
            info.setAuthorsText(String.join(", ", request.authors()));
        }
        item.setBookInfo(info);
        if (request.pageCount() != null) {
            item.setTotalValue(request.pageCount());
        }

        String provider = normalizeBookProvider(request.provider());
        ExternalLink link = new ExternalLink(item, provider);
        link.setExternalId(request.externalId());
        link.setUrl(request.infoLink());
        link.setRating(request.averageRating());
        link.setSummary(request.description());
        link.setLastSyncAt(OffsetDateTime.now(ZoneOffset.UTC));
        item.getExternalLinks().add(link);

        MediaItem saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }

    @Transactional
    public List<ExternalDiffField> refreshExternal(Long userId, Long itemId) {
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        ExternalLink link = pickExternalLink(item);
        if (item.getType() != MediaType.DVD && link == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No external link found");
        }
        ExternalMetadata metadata = fetchMetadata(item, link);
        if (metadata == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "External data not found");
        }
        if (link != null) {
            link.setLastSyncAt(OffsetDateTime.now(ZoneOffset.UTC));
            externalLinkRepository.save(link);
        }
        return diff(item, link, metadata);
    }

    @Transactional
    public ItemResponse applyExternal(Long userId, Long itemId, ExternalApplyRequest request) {
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        ExternalLink link = pickExternalLink(item);
        ExternalMetadata metadata = fetchMetadata(item, link);
        if (metadata == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "External data not found");
        }
        List<String> fields = request.fields() == null ? List.of() : request.fields();
        if (item.getType() == MediaType.DVD) {
            link = ensureOmdbLink(item, link, metadata);
            applyDvdMetadata(item, link, metadata, fields);
        } else {
            if (link == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No external link found");
            }
            applyBookMetadata(item, link, metadata, fields);
        }
        if (link != null) {
            link.setLastSyncAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        MediaItem saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }

    @Transactional
    public ItemResponse attachExternalLink(Long userId, Long itemId, ExternalLinkRequest request) {
        MediaItem item = itemRepository.findByIdAndDeletedAtIsNullAndOwner_Id(itemId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        String provider;
        if (item.getType() == MediaType.DVD) {
            if (request.provider() == null || !PROVIDER_OMDB.equalsIgnoreCase(request.provider())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported provider");
            }
            provider = PROVIDER_OMDB;
        } else {
            provider = normalizeBookProvider(request.provider());
        }
        ExternalLink link = new ExternalLink(item, provider);
        link.setExternalId(request.externalId());
        link.setUrl(request.url());
        link.setRating(request.rating());
        link.setSummary(request.summary());
        link.setLastSyncAt(OffsetDateTime.now(ZoneOffset.UTC));
        item.getExternalLinks().add(link);
        MediaItem saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }

    private ExternalLink pickExternalLink(MediaItem item) {
        if (item.getType() == MediaType.DVD) {
            return findByProvider(item, PROVIDER_OMDB);
        }
        return findByProvider(item, PROVIDER_OPEN_LIBRARY);
    }

    private List<ExternalDiffField> diff(MediaItem item, ExternalLink link, ExternalMetadata metadata) {
        if (item.getType() == MediaType.DVD) {
            return diffDvd(item, link, metadata);
        }
        return diffBook(item, link, metadata);
    }

    private List<ExternalDiffField> diffBook(MediaItem item, ExternalLink link, ExternalMetadata metadata) {
        List<ExternalDiffField> diffs = new ArrayList<>();
        addDiff(diffs, "title", item.getTitle(), metadata.title());
        BookInfo info = item.getBookInfo();
        addDiff(diffs, "authors", info == null ? null : String.join(", ", info.getAuthors()),
            metadata.authors() == null ? null : String.join(", ", metadata.authors()));
        addDiff(diffs, "publisher", info == null ? null : info.getPublisher(), metadata.publisher());
        addDiff(diffs, "pages", info == null ? null : toString(info.getPages()), toString(metadata.pageCount()));
        addDiff(diffs, "year", toString(item.getYear()), toString(metadata.year()));
        addDiff(diffs, "summary", link == null ? null : link.getSummary(), metadata.description());
        addDiff(diffs, "rating", link == null ? null : toString(link.getRating()), toString(metadata.rating()));
        return diffs;
    }

    private List<ExternalDiffField> diffDvd(MediaItem item, ExternalLink link, ExternalMetadata metadata) {
        List<ExternalDiffField> diffs = new ArrayList<>();
        addDiff(diffs, "title", item.getTitle(), metadata.title());
        DvdInfo info = item.getDvdInfo();
        addDiff(diffs, "cast", info == null ? null : String.join(", ", info.getCast()),
            metadata.authors() == null ? null : String.join(", ", metadata.authors()));
        addDiff(diffs, "director", info == null ? null : info.getDirector(), metadata.publisher());
        addDiff(diffs, "runtime", info == null ? null : toString(info.getRuntime()), toString(metadata.pageCount()));
        addDiff(diffs, "year", toString(item.getYear()), toString(metadata.year()));
        addDiff(diffs, "summary", link == null ? null : link.getSummary(), metadata.description());
        addDiff(diffs, "rating", link == null ? null : toString(link.getRating()), toString(metadata.rating()));
        return diffs;
    }

    private ExternalLink findByProvider(MediaItem item, String provider) {
        return item.getExternalLinks().stream()
            .filter(link -> provider.equalsIgnoreCase(link.getProvider()))
            .findFirst()
            .orElse(null);
    }

    private ExternalMetadata fetchMetadata(MediaItem item, ExternalLink link) {
        try {
            if (item.getType() == MediaType.DVD) {
                if (link != null) {
                    return omdbClient.fetchById(link.getExternalId());
                }
                return omdbClient.lookupByTitle(item.getTitle());
            }
            if (link == null) {
                return null;
            }
            if (!PROVIDER_OPEN_LIBRARY.equalsIgnoreCase(link.getProvider())) {
                return null;
            }
            return openLibraryClient.fetchById(link.getExternalId());
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "External provider error");
        }
    }

    private ExternalLink ensureOmdbLink(MediaItem item, ExternalLink link, ExternalMetadata metadata) {
        if (link != null) {
            return link;
        }
        ExternalLink created = new ExternalLink(item, PROVIDER_OMDB);
        created.setExternalId(metadata.externalId());
        created.setUrl(metadata.url());
        created.setRating(metadata.rating());
        created.setSummary(metadata.description());
        item.getExternalLinks().add(created);
        return created;
    }

    private void applyBookMetadata(MediaItem item, ExternalLink link, ExternalMetadata metadata, List<String> fields) {
        BookInfo info = item.getBookInfo();
        if (info == null) {
            info = new BookInfo(item);
            item.setBookInfo(info);
        }
        if (fields.contains("title")) {
            item.setTitle(metadata.title());
        }
        if (fields.contains("authors")) {
            List<String> authors = metadata.authors() == null ? List.of() : metadata.authors();
            info.setAuthors(authors);
            info.setAuthorsText(authors.isEmpty() ? null : String.join(", ", authors));
        }
        if (fields.contains("publisher")) {
            info.setPublisher(metadata.publisher());
        }
        if (fields.contains("pages")) {
            info.setPages(metadata.pageCount());
            if (metadata.pageCount() != null) {
                item.setTotalValue(metadata.pageCount());
            }
        }
        if (fields.contains("year")) {
            if (metadata.year() != null) {
                item.setYear(metadata.year());
            }
        }
        if (fields.contains("summary")) {
            link.setSummary(metadata.description());
        }
        if (fields.contains("rating")) {
            link.setRating(metadata.rating());
        }
    }

    private void applyDvdMetadata(MediaItem item, ExternalLink link, ExternalMetadata metadata, List<String> fields) {
        DvdInfo info = item.getDvdInfo();
        if (info == null) {
            info = new DvdInfo(item);
            item.setDvdInfo(info);
        }
        if (fields.contains("title")) {
            item.setTitle(metadata.title());
        }
        if (fields.contains("cast")) {
            List<String> cast = metadata.authors() == null ? List.of() : metadata.authors();
            info.setCast(cast);
        }
        if (fields.contains("director")) {
            info.setDirector(metadata.publisher());
        }
        if (fields.contains("runtime")) {
            info.setRuntime(metadata.pageCount());
            if (metadata.pageCount() != null) {
                item.setTotalValue(metadata.pageCount());
            }
        }
        if (fields.contains("year")) {
            if (metadata.year() != null) {
                item.setYear(metadata.year());
            }
        }
        if (fields.contains("summary")) {
            link.setSummary(metadata.description());
        }
        if (fields.contains("rating")) {
            link.setRating(metadata.rating());
        }
        if (link.getUrl() == null) {
            link.setUrl(metadata.url());
        }
        if (link.getExternalId() == null) {
            link.setExternalId(metadata.externalId());
        }
    }

    private void addDiff(List<ExternalDiffField> diffs, String field, String current, String updated) {
        if (!Objects.equals(normalize(current), normalize(updated))) {
            diffs.add(new ExternalDiffField(field, current, updated));
        }
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeBookProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return PROVIDER_OPEN_LIBRARY;
        }
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        if (PROVIDER_OPEN_LIBRARY.equals(normalized)) {
            return normalized;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported provider");
    }
}
