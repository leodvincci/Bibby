package com.penrose.bibby.library.cataloging.book.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorName;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
import com.penrose.bibby.library.cataloging.book.core.domain.AvailabilityStatus;
import com.penrose.bibby.library.cataloging.book.core.domain.model.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.BookId;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Isbn;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Title;
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

  @Test
  void deleteByShelfIdIn_deletesMultipleBooksWithMatchingShelfIds() {
    // given
    List<Long> shelfIds = List.of(1L, 2L, 3L);

    BookEntity book1 =
        new BookEntity(
            1L,
            "Book One",
            "9781234567890",
            "Publisher One",
            2020,
            "Fiction",
            1,
            "Description One",
            1L,
            "AVAILABLE",
            LocalDate.now(),
            LocalDate.now(),
            Set.of(new AuthorEntity("Author", "One")));

    BookEntity book2 =
        new BookEntity(
            2L,
            "Book Two",
            "9781234567891",
            "Publisher Two",
            2021,
            "Non-Fiction",
            1,
            "Description Two",
            2L,
            "AVAILABLE",
            LocalDate.now(),
            LocalDate.now(),
            Set.of(new AuthorEntity("Author", "Two")));

    BookEntity book3 =
        new BookEntity(
            3L,
            "Book Three",
            "9781234567892",
            "Publisher Three",
            2022,
            "Science",
            1,
            "Description Three",
            3L,
            "AVAILABLE",
            LocalDate.now(),
            LocalDate.now(),
            Set.of(new AuthorEntity("Author", "Three")));

    List<BookEntity> booksToDelete = List.of(book1, book2, book3);
    when(bookJpaRepository.findByShelfIdIn(shelfIds)).thenReturn(booksToDelete);

    // when
    bookDomainRepositoryImpl.deleteByShelfIdIn(shelfIds);

    // then
    verify(bookJpaRepository).findByShelfIdIn(shelfIds);
    verify(bookJpaRepository).deleteAll(booksToDelete);
  }

  @Test
  void deleteByShelfIdIn_handlesEmptyResultWhenNoBooksMatchShelfIds() {
    // given
    List<Long> shelfIds = List.of(99L, 100L);
    List<BookEntity> emptyList = List.of();
    when(bookJpaRepository.findByShelfIdIn(shelfIds)).thenReturn(emptyList);

    // when
    bookDomainRepositoryImpl.deleteByShelfIdIn(shelfIds);

    // then
    verify(bookJpaRepository).findByShelfIdIn(shelfIds);
    verify(bookJpaRepository).deleteAll(emptyList);
  }

  @Test
  void deleteByShelfIdIn_deletesSingleBookWithMatchingShelfId() {
    // given
    List<Long> shelfIds = List.of(5L);

    BookEntity book =
        new BookEntity(
            1L,
            "Single Book",
            "9781234567893",
            "Single Publisher",
            2023,
            "Biography",
            1,
            "Single Description",
            5L,
            "AVAILABLE",
            LocalDate.now(),
            LocalDate.now(),
            Set.of(new AuthorEntity("Single", "Author")));

    List<BookEntity> booksToDelete = List.of(book);
    when(bookJpaRepository.findByShelfIdIn(shelfIds)).thenReturn(booksToDelete);

    // when
    bookDomainRepositoryImpl.deleteByShelfIdIn(shelfIds);

    // then
    verify(bookJpaRepository).findByShelfIdIn(shelfIds);
    verify(bookJpaRepository).deleteAll(booksToDelete);
  }
}
