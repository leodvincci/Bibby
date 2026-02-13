package com.penrose.bibby.library.cataloging.book.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.cataloging.book.core.domain.*;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookDomainRepositoryImplTest {

  @Mock private BookJpaRepository bookJpaRepository;

  @InjectMocks
  private BookDomainRepositoryImpl
      bookDomainRepositoryImpl; // The class containing updateTheBooksShelf

  @Test
  void updateTheBooksShelf_updatesShelfAndSavesEntity() {
    // given
    Long newShelfId = 10L;
    AuthorRef authorOne = new AuthorRef(1L, new AuthorName("Author", "One"));
    AuthorRef authorTwo = new AuthorRef(2L, new AuthorName("Author", "Two"));
    AuthorRef authorThree = new AuthorRef(3L, new AuthorName("Author", "Three"));

    Book existing =
        new Book(
            new BookId(1L),
            1,
            new Title("Test Book"),
            List.of(authorOne, authorTwo, authorThree),
            new Isbn("9781492034025"),
            "Fiction",
            "Test Publisher",
            2020,
            5L,
            "Test Description",
            AvailabilityStatus.AVAILABLE,
            LocalDate.now(),
            LocalDate.now(),
            "2020-01-01");

    BookEntity bookEntity =
        new BookEntity(
            1L,
            "Test Book",
            "9781492034025",
            "Test Publisher",
            2020,
            "Fiction",
            1,
            "Test Description",
            5L,
            "AVAILABLE",
            LocalDate.now(),
            LocalDate.now(),
            Set.of(
                new AuthorEntity("Author", "One"),
                new AuthorEntity("Author", "Two"),
                new AuthorEntity("Author", "Three")));

    Long bookId = existing.getBookId().getId();
    when(bookJpaRepository.findById(bookId)).thenReturn(Optional.of(bookEntity));

    // before
    assertEquals(
        existing.getShelfId(),
        bookEntity.getShelfId(),
        "Precondition: Shelf ID should match existing value");

    // when
    bookDomainRepositoryImpl.updateTheBooksShelf(
        existing, existing.getBookId().getId(), newShelfId);

    // then
    assertEquals(newShelfId, bookEntity.getShelfId(), "Shelf ID should be updated");
    verify(bookJpaRepository).save(any(BookEntity.class));
    verify(bookJpaRepository).findById(bookId);
  }
}
