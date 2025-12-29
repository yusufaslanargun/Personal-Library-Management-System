package com.example.plms.service;

import com.example.plms.domain.MediaItem;
import com.example.plms.domain.MediaList;
import com.example.plms.repository.MediaItemRepository;
import com.example.plms.repository.MediaListRepository;
import com.example.plms.repository.ProgressLogRepository;
import com.example.plms.repository.LoanRepository;
import com.example.plms.repository.ExternalLinkRepository;
import com.example.plms.web.dto.ExportBookInfo;
import com.example.plms.web.dto.ExportBundle;
import com.example.plms.web.dto.ExportDvdInfo;
import com.example.plms.web.dto.ExportExternalLink;
import com.example.plms.web.dto.ExportItem;
import com.example.plms.web.dto.ExportList;
import com.example.plms.web.dto.ExportListItem;
import com.example.plms.web.dto.ExportLoan;
import com.example.plms.web.dto.ExportProgressLog;
import com.example.plms.web.dto.ImportSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.plms.repository.SyncStateRepository;
import com.example.plms.domain.SyncState;

@Service
public class ImportExportService {
    private static final String CSV_ITEMS = "items.csv";
    private static final String CSV_LISTS = "lists.csv";
    private static final String CSV_LIST_ITEMS = "list_items.csv";
    private static final String CSV_PROGRESS = "progress_logs.csv";
    private static final String CSV_LOANS = "loans.csv";
    private static final String CSV_EXTERNAL = "external_links.csv";

    private final MediaItemRepository itemRepository;
    private final MediaListRepository listRepository;
    private final ProgressLogRepository progressRepository;
    private final LoanRepository loanRepository;
    private final ExternalLinkRepository externalLinkRepository;
    private final SyncStateRepository syncStateRepository;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    public ImportExportService(MediaItemRepository itemRepository,
                               MediaListRepository listRepository,
                               ProgressLogRepository progressRepository,
                               LoanRepository loanRepository,
                               ExternalLinkRepository externalLinkRepository,
                               SyncStateRepository syncStateRepository,
                               JdbcTemplate jdbcTemplate,
                               NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                               ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.listRepository = listRepository;
        this.progressRepository = progressRepository;
        this.loanRepository = loanRepository;
        this.externalLinkRepository = externalLinkRepository;
        this.syncStateRepository = syncStateRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ExportBundle exportBundle(Long userId) {
        List<MediaItem> ownedItems = itemRepository.findAllByOwner_Id(userId);
        List<Long> itemIds = ownedItems.stream().map(MediaItem::getId).toList();
        List<ExportItem> items = ownedItems.stream().map(this::toExportItem).toList();
        List<MediaList> ownedLists = listRepository.findAllByOwner_Id(userId);
        List<Long> listIds = ownedLists.stream().map(MediaList::getId).toList();
        List<ExportList> lists = ownedLists.stream()
            .map(list -> new ExportList(list.getId(), list.getName()))
            .toList();
        List<ExportListItem> listItems = listIds.isEmpty()
            ? List.of()
            : namedParameterJdbcTemplate.query(
                "SELECT list_id, item_id, position, priority FROM list_item WHERE list_id IN (:ids)",
                new MapSqlParameterSource("ids", listIds),
                (rs, rowNum) -> new ExportListItem(rs.getLong("list_id"), rs.getLong("item_id"),
                    rs.getInt("position"), rs.getInt("priority"))
            );
        List<ExportProgressLog> progressLogs = itemIds.isEmpty()
            ? List.of()
            : progressRepository.findByItemIdIn(itemIds).stream()
                .map(log -> new ExportProgressLog(log.getId(), log.getItem().getId(), log.getLogDate(),
                    log.getDurationMinutes(), log.getPageOrMinute(), log.getPercent()))
                .toList();
        List<ExportLoan> loans = itemIds.isEmpty()
            ? List.of()
            : loanRepository.findByItemIdIn(itemIds).stream()
                .map(loan -> new ExportLoan(loan.getId(), loan.getItem().getId(), loan.getToWhom(),
                    loan.getStartDate(), loan.getDueDate(), loan.getReturnedAt(), loan.getStatus()))
                .toList();
        List<ExportExternalLink> externalLinks = itemIds.isEmpty()
            ? List.of()
            : externalLinkRepository.findByItemIdIn(itemIds).stream()
                .map(link -> new ExportExternalLink(link.getId(), link.getItem().getId(), link.getProvider(),
                    link.getExternalId(), link.getUrl(), link.getRating(), link.getSummary(), link.getLastSyncAt()))
                .toList();
        return new ExportBundle("1", OffsetDateTime.now(), items, lists, listItems, progressLogs, loans, externalLinks);
    }

    public byte[] exportCsvZip(Long userId) {
        ExportBundle bundle = exportBundle(userId);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            writeCsv(zipOut, CSV_ITEMS, printer -> writeItemsCsv(printer, bundle.items()));
            writeCsv(zipOut, CSV_LISTS, printer -> writeListsCsv(printer, bundle.lists()));
            writeCsv(zipOut, CSV_LIST_ITEMS, printer -> writeListItemsCsv(printer, bundle.listItems()));
            writeCsv(zipOut, CSV_PROGRESS, printer -> writeProgressCsv(printer, bundle.progressLogs()));
            writeCsv(zipOut, CSV_LOANS, printer -> writeLoansCsv(printer, bundle.loans()));
            writeCsv(zipOut, CSV_EXTERNAL, printer -> writeExternalCsv(printer, bundle.externalLinks()));
            zipOut.finish();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public ImportSummary importJson(Long userId, byte[] payload) {
        try {
            ExportBundle bundle = objectMapper.readValue(payload, ExportBundle.class);
            return importBundle(userId, bundle);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON format");
        }
    }

    public ImportSummary importCsv(Long userId, byte[] payload) {
        Map<String, List<CSVRecord>> recordsByFile = new HashMap<>();
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(payload))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                Reader reader = new InputStreamReader(zipIn, StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
                recordsByFile.put(entry.getName(), parser.getRecords());
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CSV zip");
        }

        if (!recordsByFile.containsKey(CSV_ITEMS)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items.csv missing from archive");
        }

        ExportBundle bundle = new ExportBundle(
            "1",
            OffsetDateTime.now(),
            parseItemsCsv(recordsByFile.get(CSV_ITEMS)),
            parseListsCsv(recordsByFile.get(CSV_LISTS)),
            parseListItemsCsv(recordsByFile.get(CSV_LIST_ITEMS)),
            parseProgressCsv(recordsByFile.get(CSV_PROGRESS)),
            parseLoansCsv(recordsByFile.get(CSV_LOANS)),
            parseExternalCsv(recordsByFile.get(CSV_EXTERNAL))
        );
        return importBundle(userId, bundle);
    }

    @Transactional
    public ImportSummary importBundle(Long userId, ExportBundle bundle) {
        List<String> errors = validateBundle(bundle);
        if (!errors.isEmpty()) {
            return new ImportSummary(0, 0, 0, errors);
        }

        List<Long> conflictItems = jdbcTemplate.queryForList(
            "SELECT id FROM media_item WHERE user_id IS NOT NULL AND user_id <> ?",
            Long.class,
            userId
        );
        List<Long> conflictLists = jdbcTemplate.queryForList(
            "SELECT id FROM list WHERE user_id IS NOT NULL AND user_id <> ?",
            Long.class,
            userId
        );
        List<Long> conflictProgress = jdbcTemplate.queryForList(
            "SELECT pl.id FROM progress_log pl JOIN media_item mi ON mi.id = pl.item_id WHERE mi.user_id <> ?",
            Long.class,
            userId
        );
        List<Long> conflictLoans = jdbcTemplate.queryForList(
            "SELECT l.id FROM loan l JOIN media_item mi ON mi.id = l.item_id WHERE mi.user_id <> ?",
            Long.class,
            userId
        );
        List<Long> conflictExternal = jdbcTemplate.queryForList(
            "SELECT el.id FROM external_link el JOIN media_item mi ON mi.id = el.item_id WHERE mi.user_id <> ?",
            Long.class,
            userId
        );
        Set<Long> conflictItemIds = new HashSet<>(conflictItems);
        Set<Long> conflictListIds = new HashSet<>(conflictLists);
        Set<Long> conflictProgressIds = new HashSet<>(conflictProgress);
        Set<Long> conflictLoanIds = new HashSet<>(conflictLoans);
        Set<Long> conflictExternalIds = new HashSet<>(conflictExternal);
        for (ExportItem item : bundle.items()) {
            if (item.id() != null && conflictItemIds.contains(item.id())) {
                errors.add("item id already belongs to another user: " + item.id());
            }
        }
        if (bundle.lists() != null) {
            for (ExportList list : bundle.lists()) {
                if (list.id() != null && conflictListIds.contains(list.id())) {
                    errors.add("list id already belongs to another user: " + list.id());
                }
            }
        }
        if (bundle.progressLogs() != null) {
            for (ExportProgressLog log : bundle.progressLogs()) {
                if (log.id() != null && conflictProgressIds.contains(log.id())) {
                    errors.add("progress log id already belongs to another user: " + log.id());
                }
            }
        }
        if (bundle.loans() != null) {
            for (ExportLoan loan : bundle.loans()) {
                if (loan.id() != null && conflictLoanIds.contains(loan.id())) {
                    errors.add("loan id already belongs to another user: " + loan.id());
                }
            }
        }
        if (bundle.externalLinks() != null) {
            for (ExportExternalLink link : bundle.externalLinks()) {
                if (link.id() != null && conflictExternalIds.contains(link.id())) {
                    errors.add("external link id already belongs to another user: " + link.id());
                }
            }
        }
        if (!errors.isEmpty()) {
            return new ImportSummary(0, 0, 0, errors);
        }

        Set<Long> existingItems = new HashSet<>(jdbcTemplate.queryForList(
            "SELECT id FROM media_item WHERE user_id = ?",
            Long.class,
            userId
        ));
        Set<Long> existingLists = new HashSet<>(jdbcTemplate.queryForList(
            "SELECT id FROM list WHERE user_id = ?",
            Long.class,
            userId
        ));
        Set<Long> existingProgress = new HashSet<>(jdbcTemplate.queryForList(
            "SELECT pl.id FROM progress_log pl JOIN media_item mi ON mi.id = pl.item_id WHERE mi.user_id = ?",
            Long.class,
            userId
        ));
        Set<Long> existingLoans = new HashSet<>(jdbcTemplate.queryForList(
            "SELECT l.id FROM loan l JOIN media_item mi ON mi.id = l.item_id WHERE mi.user_id = ?",
            Long.class,
            userId
        ));
        Set<Long> existingExternal = new HashSet<>(jdbcTemplate.queryForList(
            "SELECT el.id FROM external_link el JOIN media_item mi ON mi.id = el.item_id WHERE mi.user_id = ?",
            Long.class,
            userId
        ));

        int added = 0;
        int updated = 0;

        for (ExportItem item : bundle.items()) {
            if (existingItems.contains(item.id())) {
                updated++;
            } else {
                added++;
            }
            upsertItem(userId, item);
            upsertBookInfo(item.bookInfo());
            upsertDvdInfo(item.dvdInfo());
            upsertTags(item.id(), item.tags());
        }

        for (ExportList list : bundle.lists()) {
            if (existingLists.contains(list.id())) {
                updated++;
            } else {
                added++;
            }
            jdbcTemplate.update("""
                INSERT INTO list (id, name, user_id, created_at, updated_at)
                VALUES (?, ?, ?, COALESCE((SELECT created_at FROM list WHERE id = ?), ?), ?)
                ON CONFLICT (id) DO UPDATE
                SET name = EXCLUDED.name, updated_at = EXCLUDED.updated_at
                WHERE list.user_id = EXCLUDED.user_id
                """, list.id(), list.name(), userId, list.id(), OffsetDateTime.now(), OffsetDateTime.now());
        }

        for (ExportListItem listItem : bundle.listItems()) {
            jdbcTemplate.update("DELETE FROM list_item WHERE list_id = ? AND item_id = ?", listItem.listId(), listItem.itemId());
            jdbcTemplate.update("""
                INSERT INTO list_item (list_id, item_id, position, priority, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (list_id, item_id) DO UPDATE
                SET position = EXCLUDED.position, priority = EXCLUDED.priority, updated_at = EXCLUDED.updated_at
                """, listItem.listId(), listItem.itemId(), listItem.position(), listItem.priority(), OffsetDateTime.now());
        }

        for (ExportProgressLog log : bundle.progressLogs()) {
            if (existingProgress.contains(log.id())) {
                updated++;
            } else {
                added++;
            }
            jdbcTemplate.update("""
                INSERT INTO progress_log (id, item_id, log_date, duration_minutes, page_or_minute, percent, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                item_id = EXCLUDED.item_id,
                log_date = EXCLUDED.log_date,
                duration_minutes = EXCLUDED.duration_minutes,
                page_or_minute = EXCLUDED.page_or_minute,
                percent = EXCLUDED.percent,
                updated_at = EXCLUDED.updated_at
                """, log.id(), log.itemId(), log.date(), log.durationMinutes(), log.pageOrMinute(), log.percent(), OffsetDateTime.now());
        }

        for (ExportLoan loan : bundle.loans()) {
            if (existingLoans.contains(loan.id())) {
                updated++;
            } else {
                added++;
            }
            jdbcTemplate.update("""
                INSERT INTO loan (id, item_id, to_whom, start_date, due_date, returned_at, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                item_id = EXCLUDED.item_id,
                to_whom = EXCLUDED.to_whom,
                start_date = EXCLUDED.start_date,
                due_date = EXCLUDED.due_date,
                returned_at = EXCLUDED.returned_at,
                status = EXCLUDED.status,
                updated_at = EXCLUDED.updated_at
                """, loan.id(), loan.itemId(), loan.toWhom(), loan.startDate(), loan.dueDate(), loan.returnedAt(),
                loan.status().name(), OffsetDateTime.now());
        }

        for (ExportExternalLink link : bundle.externalLinks()) {
            if (existingExternal.contains(link.id())) {
                updated++;
            } else {
                added++;
            }
            jdbcTemplate.update("""
                INSERT INTO external_link (id, item_id, provider, external_id, url, rating, summary, last_sync_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                item_id = EXCLUDED.item_id,
                provider = EXCLUDED.provider,
                external_id = EXCLUDED.external_id,
                url = EXCLUDED.url,
                rating = EXCLUDED.rating,
                summary = EXCLUDED.summary,
                last_sync_at = EXCLUDED.last_sync_at,
                updated_at = EXCLUDED.updated_at
                """, link.id(), link.itemId(), link.provider(), link.externalId(), link.url(), link.rating(), link.summary(),
                link.lastSyncAt(), OffsetDateTime.now());
        }

        updateSequences();
        markNeedsFullSync(userId);
        return new ImportSummary(added, updated, 0, List.of());
    }

    private void upsertItem(Long userId, ExportItem item) {
        jdbcTemplate.update("""
            INSERT INTO media_item (id, user_id, type, title, year, condition, location, status, deleted_at, created_at, updated_at,
              progress_percent, progress_value, total_value)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
              user_id = EXCLUDED.user_id,
              type = EXCLUDED.type,
              title = EXCLUDED.title,
              year = EXCLUDED.year,
              condition = EXCLUDED.condition,
              location = EXCLUDED.location,
              status = EXCLUDED.status,
              deleted_at = EXCLUDED.deleted_at,
              created_at = EXCLUDED.created_at,
              updated_at = EXCLUDED.updated_at,
              progress_percent = EXCLUDED.progress_percent,
              progress_value = EXCLUDED.progress_value,
              total_value = EXCLUDED.total_value
            WHERE media_item.user_id = EXCLUDED.user_id
            """,
            item.id(), userId, item.type().name(), item.title(), item.year(), item.condition(), item.location(),
            item.status().name(), item.deletedAt(), item.createdAt(), item.updatedAt(),
            item.progressPercent(), item.progressValue(), item.totalValue());
    }

    private void upsertBookInfo(ExportBookInfo info) {
        if (info == null) {
            return;
        }
        jdbcTemplate.update("""
            INSERT INTO book_info (item_id, isbn, pages, publisher, authors_text)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (item_id) DO UPDATE SET
              isbn = EXCLUDED.isbn,
              pages = EXCLUDED.pages,
              publisher = EXCLUDED.publisher,
              authors_text = EXCLUDED.authors_text
            """, info.itemId(), info.isbn(), info.pages(), info.publisher(), join(info.authors()));
        jdbcTemplate.update("DELETE FROM book_author WHERE item_id = ?", info.itemId());
        if (info.authors() != null) {
            for (String author : info.authors()) {
                jdbcTemplate.update("INSERT INTO book_author (item_id, author) VALUES (?, ?)", info.itemId(), author);
            }
        }
    }

    private void upsertDvdInfo(ExportDvdInfo info) {
        if (info == null) {
            return;
        }
        jdbcTemplate.update("""
            INSERT INTO dvd_info (item_id, runtime, director)
            VALUES (?, ?, ?)
            ON CONFLICT (item_id) DO UPDATE SET
              runtime = EXCLUDED.runtime,
              director = EXCLUDED.director
            """, info.itemId(), info.runtime(), info.director());
        jdbcTemplate.update("DELETE FROM dvd_cast WHERE item_id = ?", info.itemId());
        if (info.cast() != null) {
            for (String member : info.cast()) {
                jdbcTemplate.update("INSERT INTO dvd_cast (item_id, member) VALUES (?, ?)", info.itemId(), member);
            }
        }
    }

    private void upsertTags(Long itemId, List<String> tags) {
        jdbcTemplate.update("DELETE FROM media_item_tag WHERE item_id = ?", itemId);
        if (tags == null) {
            return;
        }
        for (String name : tags) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Long tagId = jdbcTemplate.queryForObject(
                "INSERT INTO tag (name) VALUES (?) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id",
                Long.class, name
            );
            jdbcTemplate.update("""
                INSERT INTO media_item_tag (item_id, tag_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """, itemId, tagId);
        }
    }

    private void updateSequences() {
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('media_item','id'), COALESCE(MAX(id), 1)) FROM media_item");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('list','id'), COALESCE(MAX(id), 1)) FROM list");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('progress_log','id'), COALESCE(MAX(id), 1)) FROM progress_log");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('loan','id'), COALESCE(MAX(id), 1)) FROM loan");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('external_link','id'), COALESCE(MAX(id), 1)) FROM external_link");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('tag','id'), COALESCE(MAX(id), 1)) FROM tag");
    }

    private void markNeedsFullSync(Long userId) {
        SyncState state = syncStateRepository.findByUserId(userId)
            .orElseGet(() -> {
                SyncState created = new SyncState();
                created.setUserId(userId);
                return syncStateRepository.save(created);
            });
        state.setNeedsFullSync(true);
        syncStateRepository.save(state);
    }

    private List<String> validateBundle(ExportBundle bundle) {
        List<String> errors = new ArrayList<>();
        if (bundle.items() == null) {
            errors.add("items list is missing");
            return errors;
        }
        Set<Long> itemIds = new HashSet<>();
        Set<Long> listIds = new HashSet<>();
        for (ExportItem item : bundle.items()) {
            if (item.id() == null) {
                errors.add("item id missing");
                continue;
            }
            itemIds.add(item.id());
            if (item.title() == null || item.title().isBlank()) {
                errors.add("item title missing for id " + item.id());
            }
            if (item.type() == null) {
                errors.add("item type missing for id " + item.id());
            }
            if (item.year() == null) {
                errors.add("item year missing for id " + item.id());
            }
        }
        if (bundle.lists() != null) {
            for (ExportList list : bundle.lists()) {
                if (list.id() == null) {
                    errors.add("list id missing");
                } else {
                    listIds.add(list.id());
                }
            }
        }
        if (bundle.listItems() != null) {
            for (ExportListItem listItem : bundle.listItems()) {
                if (!itemIds.contains(listItem.itemId())) {
                    errors.add("list item references unknown item " + listItem.itemId());
                }
                if (!listIds.contains(listItem.listId())) {
                    errors.add("list item references unknown list " + listItem.listId());
                }
            }
        }
        if (bundle.progressLogs() != null) {
            for (ExportProgressLog log : bundle.progressLogs()) {
                if (!itemIds.contains(log.itemId())) {
                    errors.add("progress log references unknown item " + log.itemId());
                }
            }
        }
        if (bundle.loans() != null) {
            for (ExportLoan loan : bundle.loans()) {
                if (!itemIds.contains(loan.itemId())) {
                    errors.add("loan references unknown item " + loan.itemId());
                }
            }
        }
        if (bundle.externalLinks() != null) {
            for (ExportExternalLink link : bundle.externalLinks()) {
                if (!itemIds.contains(link.itemId())) {
                    errors.add("external link references unknown item " + link.itemId());
                }
            }
        }
        return errors;
    }

    private ExportItem toExportItem(MediaItem item) {
        ExportBookInfo bookInfo = null;
        if (item.getBookInfo() != null) {
            bookInfo = new ExportBookInfo(item.getId(), item.getBookInfo().getIsbn(), item.getBookInfo().getPages(),
                item.getBookInfo().getPublisher(), item.getBookInfo().getAuthors() == null ? List.of() : List.copyOf(item.getBookInfo().getAuthors()));
        }
        ExportDvdInfo dvdInfo = null;
        if (item.getDvdInfo() != null) {
            dvdInfo = new ExportDvdInfo(item.getId(), item.getDvdInfo().getRuntime(), item.getDvdInfo().getDirector(),
                item.getDvdInfo().getCast() == null ? List.of() : List.copyOf(item.getDvdInfo().getCast()));
        }
        return new ExportItem(
            item.getId(),
            item.getType(),
            item.getTitle(),
            item.getYear(),
            item.getCondition(),
            item.getLocation(),
            item.getStatus(),
            item.getDeletedAt(),
            item.getCreatedAt(),
            item.getUpdatedAt(),
            item.getProgressPercent(),
            item.getProgressValue(),
            item.getTotalValue(),
            item.getTags().stream().map(tag -> tag.getName()).sorted().toList(),
            bookInfo,
            dvdInfo
        );
    }

    private void writeCsv(ZipOutputStream zipOut, String filename, CsvWriter writer) throws IOException {
        zipOut.putNextEntry(new ZipEntry(filename));
        OutputStreamWriter out = new OutputStreamWriter(zipOut, StandardCharsets.UTF_8);
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT);
        writer.write(printer);
        printer.flush();
        zipOut.closeEntry();
    }

    private void writeItemsCsv(CSVPrinter printer, List<ExportItem> items) throws IOException {
        printer.printRecord("id", "type", "title", "year", "condition", "location", "status", "deletedAt",
            "createdAt", "updatedAt", "progressPercent", "progressValue", "totalValue", "tags",
            "bookIsbn", "bookPages", "bookPublisher", "bookAuthors", "dvdRuntime", "dvdDirector", "dvdCast");
        for (ExportItem item : items) {
            printer.printRecord(
                item.id(),
                item.type(),
                item.title(),
                item.year(),
                item.condition(),
                item.location(),
                item.status(),
                item.deletedAt(),
                item.createdAt(),
                item.updatedAt(),
                item.progressPercent(),
                item.progressValue(),
                item.totalValue(),
                join(item.tags()),
                item.bookInfo() == null ? null : item.bookInfo().isbn(),
                item.bookInfo() == null ? null : item.bookInfo().pages(),
                item.bookInfo() == null ? null : item.bookInfo().publisher(),
                item.bookInfo() == null ? null : join(item.bookInfo().authors()),
                item.dvdInfo() == null ? null : item.dvdInfo().runtime(),
                item.dvdInfo() == null ? null : item.dvdInfo().director(),
                item.dvdInfo() == null ? null : join(item.dvdInfo().cast())
            );
        }
    }

    private void writeListsCsv(CSVPrinter printer, List<ExportList> lists) throws IOException {
        printer.printRecord("id", "name");
        for (ExportList list : lists) {
            printer.printRecord(list.id(), list.name());
        }
    }

    private void writeListItemsCsv(CSVPrinter printer, List<ExportListItem> items) throws IOException {
        printer.printRecord("listId", "itemId", "position", "priority");
        for (ExportListItem item : items) {
            printer.printRecord(item.listId(), item.itemId(), item.position(), item.priority());
        }
    }

    private void writeProgressCsv(CSVPrinter printer, List<ExportProgressLog> logs) throws IOException {
        printer.printRecord("id", "itemId", "date", "durationMinutes", "pageOrMinute", "percent");
        for (ExportProgressLog log : logs) {
            printer.printRecord(log.id(), log.itemId(), log.date(), log.durationMinutes(), log.pageOrMinute(), log.percent());
        }
    }

    private void writeLoansCsv(CSVPrinter printer, List<ExportLoan> loans) throws IOException {
        printer.printRecord("id", "itemId", "toWhom", "startDate", "dueDate", "returnedAt", "status");
        for (ExportLoan loan : loans) {
            printer.printRecord(loan.id(), loan.itemId(), loan.toWhom(), loan.startDate(), loan.dueDate(), loan.returnedAt(), loan.status());
        }
    }

    private void writeExternalCsv(CSVPrinter printer, List<ExportExternalLink> links) throws IOException {
        printer.printRecord("id", "itemId", "provider", "externalId", "url", "rating", "summary", "lastSyncAt");
        for (ExportExternalLink link : links) {
            printer.printRecord(link.id(), link.itemId(), link.provider(), link.externalId(), link.url(), link.rating(), link.summary(), link.lastSyncAt());
        }
    }

    private List<ExportItem> parseItemsCsv(List<CSVRecord> records) {
        if (records == null) {
            return List.of();
        }
        List<ExportItem> items = new ArrayList<>();
        for (CSVRecord record : records) {
            Long id = parseLong(record.get("id"));
            ExportBookInfo bookInfo = null;
            if (notBlank(record.get("bookIsbn")) || notBlank(record.get("bookPublisher"))) {
                bookInfo = new ExportBookInfo(id, record.get("bookIsbn"), parseInteger(record.get("bookPages")),
                    record.get("bookPublisher"), split(record.get("bookAuthors")));
            }
            ExportDvdInfo dvdInfo = null;
            if (notBlank(record.get("dvdRuntime")) || notBlank(record.get("dvdDirector"))) {
                dvdInfo = new ExportDvdInfo(id, parseInteger(record.get("dvdRuntime")),
                    record.get("dvdDirector"), split(record.get("dvdCast")));
            }
            items.add(new ExportItem(
                id,
                parseEnum(record.get("type")),
                record.get("title"),
                parseInteger(record.get("year")),
                record.get("condition"),
                record.get("location"),
                parseStatus(record.get("status")),
                parseOffsetDateTime(record.get("deletedAt")),
                parseOffsetDateTime(record.get("createdAt")),
                parseOffsetDateTime(record.get("updatedAt")),
                parseInteger(record.get("progressPercent")),
                parseInteger(record.get("progressValue")),
                parseInteger(record.get("totalValue")),
                split(record.get("tags")),
                bookInfo,
                dvdInfo
            ));
        }
        return items;
    }

    private List<ExportList> parseListsCsv(List<CSVRecord> records) {
        if (records == null) {
            return List.of();
        }
        List<ExportList> lists = new ArrayList<>();
        for (CSVRecord record : records) {
            lists.add(new ExportList(parseLong(record.get("id")), record.get("name")));
        }
        return lists;
    }

    private List<ExportListItem> parseListItemsCsv(List<CSVRecord> records) {
        if (records == null) {
            return List.of();
        }
        List<ExportListItem> items = new ArrayList<>();
        for (CSVRecord record : records) {
            items.add(new ExportListItem(parseLong(record.get("listId")), parseLong(record.get("itemId")),
                parseInteger(record.get("position")), parseInteger(record.get("priority"))));
        }
        return items;
    }

    private List<ExportProgressLog> parseProgressCsv(List<CSVRecord> records) {
        if (records == null) {
            return List.of();
        }
        List<ExportProgressLog> logs = new ArrayList<>();
        for (CSVRecord record : records) {
            logs.add(new ExportProgressLog(parseLong(record.get("id")), parseLong(record.get("itemId")),
                LocalDate.parse(record.get("date")), parseInteger(record.get("durationMinutes")),
                parseInteger(record.get("pageOrMinute")), parseInteger(record.get("percent"))));
        }
        return logs;
    }

    private List<ExportLoan> parseLoansCsv(List<CSVRecord> records) {
        if (records == null) {
            return List.of();
        }
        List<ExportLoan> loans = new ArrayList<>();
        for (CSVRecord record : records) {
            loans.add(new ExportLoan(parseLong(record.get("id")), parseLong(record.get("itemId")),
                record.get("toWhom"), LocalDate.parse(record.get("startDate")),
                LocalDate.parse(record.get("dueDate")), parseLocalDate(record.get("returnedAt")),
                parseLoanStatus(record.get("status"))));
        }
        return loans;
    }

    private List<ExportExternalLink> parseExternalCsv(List<CSVRecord> records) {
        if (records == null) {
            return List.of();
        }
        List<ExportExternalLink> links = new ArrayList<>();
        for (CSVRecord record : records) {
            links.add(new ExportExternalLink(parseLong(record.get("id")), parseLong(record.get("itemId")),
                record.get("provider"), record.get("externalId"), record.get("url"),
                parseBigDecimal(record.get("rating")), record.get("summary"),
                parseOffsetDateTime(record.get("lastSyncAt"))));
        }
        return links;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join("|", values);
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\|"));
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Long parseLong(String value) {
        if (!notBlank(value)) {
            return null;
        }
        return Long.parseLong(value);
    }

    private Integer parseInteger(String value) {
        if (!notBlank(value)) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (!notBlank(value)) {
            return null;
        }
        return OffsetDateTime.parse(value);
    }

    private LocalDate parseLocalDate(String value) {
        if (!notBlank(value)) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (!notBlank(value)) {
            return null;
        }
        return new BigDecimal(value);
    }

    private com.example.plms.domain.MediaType parseEnum(String value) {
        return com.example.plms.domain.MediaType.valueOf(value);
    }

    private com.example.plms.domain.MediaStatus parseStatus(String value) {
        return com.example.plms.domain.MediaStatus.valueOf(value);
    }

    private com.example.plms.domain.LoanStatus parseLoanStatus(String value) {
        return com.example.plms.domain.LoanStatus.valueOf(value);
    }

    @FunctionalInterface
    private interface CsvWriter {
        void write(CSVPrinter printer) throws IOException;
    }
}
