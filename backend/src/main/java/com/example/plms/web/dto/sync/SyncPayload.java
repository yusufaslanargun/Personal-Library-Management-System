package com.example.plms.web.dto.sync;

import java.util.List;

public record SyncPayload(
    boolean fullSync,
    List<SyncItem> items,
    List<SyncList> lists,
    List<SyncListItem> listItems,
    List<SyncProgressLog> progressLogs,
    List<SyncLoan> loans,
    List<SyncExternalLink> externalLinks,
    List<SyncDelete> deletes
) {
}
