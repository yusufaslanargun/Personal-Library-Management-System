package com.example.plms.web.dto;

import java.util.List;

public record BookInfoRequest(
    String isbn,
    Integer pages,
    String publisher,
    List<String> authors
) {
}
