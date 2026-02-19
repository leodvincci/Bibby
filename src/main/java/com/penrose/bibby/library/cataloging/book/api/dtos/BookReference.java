package com.penrose.bibby.library.cataloging.book.api.dtos;

import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.BookId;
import java.util.List;

public record BookReference(BookId bookId, String title, List<String> authorDisplayNames) {}
