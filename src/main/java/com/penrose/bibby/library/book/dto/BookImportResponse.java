package com.penrose.bibby.library.book.dto;

import java.util.List;

public record BookImportResponse(
        Long bookId,
        String title,
        String isbn,
        List<String> authors,
        String publisher,
        String description
) {
}
