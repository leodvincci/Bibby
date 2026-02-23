package com.penrose.bibby.web.controllers.cataloging.book.mappers;

import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.core.domain.model.Book;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BookMapper {

  public static BookDTO toDTOFromDomain(Book book) {
    List<String> authorNames =
        book.getAuthors() != null
            ? book.getAuthors().stream()
                .map(a -> a.getAuthorFirstName() + " " + a.getAuthorLastName())
                .collect(Collectors.toList())
            : Collections.emptyList();

    return new BookDTO(
        book.getBookId().getId(),
        book.getEdition(),
        book.getTitle().getTitle(),
        authorNames,
        book.getIsbn() != null ? book.getIsbn().isbn() : null,
        book.getGenre(),
        book.getPublisher(),
        book.getPublicationYear(),
        book.getShelfId(),
        book.getDescription(),
        book.getAvailabilityStatus(),
        book.getCreatedAt(),
        book.getUpdatedAt(),
        book.getPublishedDate());
  }
}
