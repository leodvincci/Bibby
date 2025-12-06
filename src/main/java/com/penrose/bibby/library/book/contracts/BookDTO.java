package com.penrose.bibby.library.book.contracts;

import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.book.domain.AvailabilityStatus;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record BookDTO(Long id,
                      int edition,
                      String title,
                      Set<Long> authorIds,
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
        Set<Long> authorIds = new HashSet<>();
        for (AuthorEntity author : bookEntity.getAuthors()) {
            authorIds.add(author.getAuthorId());
        }

        return new BookDTO(
                bookEntity.getBookId(),
                bookEntity.getEdition(),
                bookEntity.getTitle(),
                authorIds,
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
