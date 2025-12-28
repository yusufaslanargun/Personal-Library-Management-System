package com.example.plms.web.dto.sync;

import java.util.List;

public record SyncDvdInfo(
    Integer runtime,
    String director,
    List<String> cast
) {
}
