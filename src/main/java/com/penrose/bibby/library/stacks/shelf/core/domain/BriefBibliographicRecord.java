package com.penrose.bibby.library.stacks.shelf.core.domain;

import java.util.List;

/**
 * A lightweight, read-only bibliographic view of a book used for shelf browsing and display.
 * <p>
 * Represents a “brief bib record” (not the full Book domain model) and contains only the core
 * descriptive metadata needed to identify and present a resource in the Shelf context.
 */
public record BriefBibliographicRecord(
        Long bookId,
        String title,
        List<String> authors,
        int edition,
        String publisher,
        String publicationYear,
        String isbn,
        String summary
    ){

}
