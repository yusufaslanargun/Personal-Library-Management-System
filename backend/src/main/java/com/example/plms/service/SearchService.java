package com.example.plms.service;

import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaStatus;
import com.example.plms.domain.MediaType;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.web.dto.ItemResponse;
import com.example.plms.web.dto.SearchResponse;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final MediaItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public SearchService(NamedParameterJdbcTemplate jdbcTemplate, MediaItemRepository itemRepository, ItemMapper itemMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String query, MediaType type, MediaStatus status, List<String> tags,
                                 String author, String cast, Integer year, String condition,
                                 String location, int page, int size) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String normalizedAuthor = author == null || author.isBlank() ? null : author.trim();
        String normalizedCast = cast == null || cast.isBlank() ? null : cast.trim();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * safeSize;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("query", normalizedQuery, Types.VARCHAR);
        params.addValue("type", type == null ? null : type.name(), Types.VARCHAR);
        params.addValue("status", status == null ? null : status.name(), Types.VARCHAR);
        params.addValue("year", year, Types.INTEGER);
        params.addValue("author", normalizedAuthor, Types.VARCHAR);
        params.addValue("castMember", normalizedCast, Types.VARCHAR);
        params.addValue("condition", condition == null || condition.isBlank() ? null : condition.trim(), Types.VARCHAR);
        params.addValue("location", location == null || location.isBlank() ? null : location.trim(), Types.VARCHAR);
        params.addValue("limit", safeSize);
        params.addValue("offset", offset);
        List<String> normalizedTags = tags == null ? List.of() : tags.stream()
            .map(String::trim)
            .filter(tag -> !tag.isBlank())
            .toList();
        boolean hasTags = !normalizedTags.isEmpty();
        if (hasTags) {
            params.addValue("tags", normalizedTags);
            params.addValue("tagCount", normalizedTags.size());
        }

        String baseWhere = "WHERE mi.deleted_at IS NULL "
            + "AND (:type IS NULL OR mi.type = :type) "
            + "AND (:status IS NULL OR mi.status = :status) "
            + "AND (:year IS NULL OR mi.year = :year) "
            + "AND (:author IS NULL OR EXISTS (SELECT 1 FROM book_info bi "
            + "WHERE bi.item_id = mi.id AND bi.authors_text ILIKE '%' || :author || '%')) "
            + "AND (:castMember IS NULL OR EXISTS (SELECT 1 FROM dvd_cast dc "
            + "WHERE dc.item_id = mi.id AND dc.member ILIKE '%' || :castMember || '%')) "
            + "AND (:condition IS NULL OR mi.condition = :condition) "
            + "AND (:location IS NULL OR mi.location = :location) "
            + "AND (:query IS NULL OR mi.search_vector @@ websearch_to_tsquery('simple', :query))";

        String tagWhere = hasTags ? " AND t.name IN (:tags) " : "";
        String tagHaving = hasTags ? " HAVING COUNT(DISTINCT t.name) = :tagCount " : "";

        String selectSql = "SELECT mi.id, "
            + "ts_rank(mi.search_vector, websearch_to_tsquery('simple', :query)) AS rank "
            + "FROM media_item mi "
            + "LEFT JOIN media_item_tag mit ON mit.item_id = mi.id "
            + "LEFT JOIN tag t ON t.id = mit.tag_id "
            + baseWhere + tagWhere + " "
            + "GROUP BY mi.id "
            + tagHaving
            + "ORDER BY rank DESC NULLS LAST, mi.id ASC "
            + "LIMIT :limit OFFSET :offset";

        List<Long> ids = jdbcTemplate.query(selectSql, params, (rs, rowNum) -> rs.getLong("id"));

        String countSql = "SELECT COUNT(*) FROM ("
            + "SELECT mi.id "
            + "FROM media_item mi "
            + "LEFT JOIN media_item_tag mit ON mit.item_id = mi.id "
            + "LEFT JOIN tag t ON t.id = mit.tag_id "
            + baseWhere + tagWhere + " "
            + "GROUP BY mi.id "
            + tagHaving
            + ") sub";
        Long total = jdbcTemplate.queryForObject(countSql, params, Long.class);
        if (total == null) {
            total = 0L;
        }

        if (ids.isEmpty()) {
            return new SearchResponse(List.of(), page, size, total);
        }

        List<MediaItem> items = itemRepository.findByIdIn(ids);
        Map<Long, MediaItem> mapped = new HashMap<>();
        for (MediaItem item : items) {
            mapped.put(item.getId(), item);
        }
        List<ItemResponse> ordered = ids.stream()
            .map(mapped::get)
            .filter(item -> item != null)
            .map(itemMapper::toResponse)
            .toList();

        return new SearchResponse(ordered, page, safeSize, total);
    }
}
