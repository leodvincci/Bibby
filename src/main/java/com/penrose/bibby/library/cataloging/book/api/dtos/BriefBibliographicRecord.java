package com.penrose.bibby.library.cataloging.book.api.dtos;

import java.util.List;

/**
 * Represents a summary of a bibliographic record containing key details about a book. This record
 * is intended for scenarios that require lightweight information about a book without the
 * complexity of full bibliographic details.
 *
 * @param briefBibliographicRecordId The unique identifier of the brief bibliographic record.
 * @param title The title of the book.
 * @param authors A list of authors associated with the book.
 * @param edition The edition number of the book.
 * @param publisher The publisher of the book.
 * @param publicationYear The year the book was published.
 * @param isbn The International Standard Book Number (ISBN) of the book.
 * @param summary A brief summary or description of the book.
 */
public record BriefBibliographicRecord(
    Long briefBibliographicRecordId,
    String title,
    List<String> authors,
    int edition,
    String publisher,
    int publicationYear,
    String isbn,
    String summary) {}
