package com.example.plms.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ExportBundle(
    String version,
    OffsetDateTime exportedAt,
    List<ExportItem> items,
    List<ExportList> lists,
    List<ExportListItem> listItems,
    List<ExportProgressLog> progressLogs,
    List<ExportLoan> loans,
    List<ExportExternalLink> externalLinks
) {
}
