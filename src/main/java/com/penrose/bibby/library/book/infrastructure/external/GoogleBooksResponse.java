package com.penrose.bibby.library.book.infrastructure.external;

import java.util.List;

public record GoogleBooksResponse(List<GoogleBookItems> items) {
}
