package com.penrose.bibby.library.cataloging.book.api.dtos;

import java.util.List;

public record BriefBibliographicRecord(
    Long briefBibliographicRecordId,
    String title,
    List<String> authors,
    int edition,
    String publisher,
    int publicationYear,
    String isbn,
    String summary) {}
