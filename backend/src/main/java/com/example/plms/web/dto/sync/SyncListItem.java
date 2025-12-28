package com.example.plms.web.dto.sync;

import java.time.OffsetDateTime;

public record SyncListItem(
    Long listId,
    Long itemId,
    Integer position,
    Integer priority,
    OffsetDateTime updatedAt
) {
}
