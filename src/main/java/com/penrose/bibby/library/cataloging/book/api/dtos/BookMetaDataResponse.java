package com.penrose.bibby.library.cataloging.book.api.dtos;

import java.util.List;
import java.util.Optional;

public record BookMetaDataResponse(
    Long bookId,
    String title,
    String isbn,
    List<String> authors,
    String publisher,
    Optional<String> description) {}
