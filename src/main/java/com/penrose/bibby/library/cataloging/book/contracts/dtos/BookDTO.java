package com.penrose.bibby.library.cataloging.book.contracts.dtos;

import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.cataloging.book.core.domain.AvailabilityStatus;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;

import java.time.LocalDate;
import java.util.*;

public record BookDTO(Long id,
                      int edition,
                      String title,
                      List<String> authors,
                      String isbn,
                      String genre,
                      String publisher,
                      int publicationYear,
                      Long shelfId,
                      String description,
                      AvailabilityStatus availabilityStatus,
                      LocalDate createdAt,
                      LocalDate updatedAt,
                      String publishedDate) {


    public static BookDTO fromEntity(BookEntity bookEntity) {

        List<String> authors = new ArrayList<>();

            for (AuthorEntity author : bookEntity.getAuthors()) {
                authors.add(author.getFirstName() + " " + author.getLastName());
            }

        return new BookDTO(
                bookEntity.getBookId(),
                bookEntity.getEdition(),
                bookEntity.getTitle(),
                authors,
                bookEntity.getIsbn(),
                bookEntity.getGenre(),
                bookEntity.getPublisher(),
                bookEntity.getPublicationYear(),
                bookEntity.getShelfId(),
                bookEntity.getDescription(),
                AvailabilityStatus.AVAILABLE,
                bookEntity.getCreatedAt(),
                bookEntity.getUpdatedAt(),
                null
        );
    }
}
