package com.penrose.bibby.library.catalog.book.infrastructure.external;

import java.util.List;

public record GoogleBooksResponse(List<GoogleBookItems> items) {
}
