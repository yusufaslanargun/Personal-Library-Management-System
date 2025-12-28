package com.example.plms.service;

import com.example.plms.domain.BookInfo;
import com.example.plms.domain.DvdInfo;
import com.example.plms.domain.ExternalLink;
import com.example.plms.domain.MediaItem;
import com.example.plms.web.dto.BookInfoResponse;
import com.example.plms.web.dto.DvdInfoResponse;
import com.example.plms.web.dto.ExternalLinkResponse;
import com.example.plms.web.dto.ItemResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {
    public ItemResponse toResponse(MediaItem item) {
        return new ItemResponse(
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
            toBookInfo(item.getBookInfo()),
            toDvdInfo(item.getDvdInfo()),
            item.getExternalLinks().stream().map(this::toExternalLink).toList()
        );
    }

    private BookInfoResponse toBookInfo(BookInfo bookInfo) {
        if (bookInfo == null) {
            return null;
        }
        return new BookInfoResponse(bookInfo.getIsbn(), bookInfo.getPages(), bookInfo.getPublisher(),
            bookInfo.getAuthors() == null ? List.of() : List.copyOf(bookInfo.getAuthors()));
    }

    private DvdInfoResponse toDvdInfo(DvdInfo dvdInfo) {
        if (dvdInfo == null) {
            return null;
        }
        return new DvdInfoResponse(dvdInfo.getRuntime(), dvdInfo.getDirector(),
            dvdInfo.getCast() == null ? List.of() : List.copyOf(dvdInfo.getCast()));
    }

    private ExternalLinkResponse toExternalLink(ExternalLink link) {
        return new ExternalLinkResponse(
            link.getId(),
            link.getProvider(),
            link.getExternalId(),
            link.getUrl(),
            link.getRating(),
            link.getSummary(),
            link.getLastSyncAt()
        );
    }
}
