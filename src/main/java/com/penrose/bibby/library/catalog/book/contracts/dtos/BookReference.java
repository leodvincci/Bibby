package com.penrose.bibby.library.catalog.book.contracts.dtos;

import com.penrose.bibby.library.catalog.book.core.domain.BookId;

import java.util.List;

public record BookReference(BookId bookId, String title, List<String> authorDisplayNames) {
}
