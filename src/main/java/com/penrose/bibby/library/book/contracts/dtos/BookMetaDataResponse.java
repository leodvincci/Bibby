package com.penrose.bibby.library.book.contracts;

import java.util.List;

public record BookMetaDataResponse(
        Long bookId,
        String title,
        String isbn,
        List<String> authors,
        String publisher,
        String description
) {
}
