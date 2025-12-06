package com.penrose.bibby.library.book.contracts;

import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.book.domain.AvailabilityStatus;

import java.time.LocalDate;
import java.util.HashSet;
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
}
