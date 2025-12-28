package com.example.plms.web.dto.sync;

import java.util.List;

public record SyncBookInfo(
    String isbn,
    Integer pages,
    String publisher,
    List<String> authors
) {
}
