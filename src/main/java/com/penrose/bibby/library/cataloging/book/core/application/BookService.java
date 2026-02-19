package com.penrose.bibby.library.cataloging.book.core.application;

import com.penrose.bibby.library.cataloging.author.api.dtos.AuthorDTO;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookLocationResponse;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookRequestDTO;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookSummary;
import com.penrose.bibby.library.cataloging.book.core.domain.BookBuilder;
import com.penrose.bibby.library.cataloging.book.core.domain.model.Book;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.AuthorAccessPort;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.BookDomainRepository;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.ShelfAccessPort;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.cataloging.book.infrastructure.mapping.BookMapper;
import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.repository.BookcaseJpaRepository;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import java.util.*;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {
  private final BookJpaRepository bookJpaRepository;
  private final AuthorAccessPort authorAccessPort;

  private final BookBuilder BookBuilder;
  private final BookMapper bookMapper;
  private final IsbnLookupService isbnLookupService;
  private final IsbnEnrichmentService isbnEnrichmentService;
  private final BookDomainRepository bookDomainRepository;
  private final ShelfAccessPort shelfAccessPort;
  private final BookcaseJpaRepository bookcaseJpaRepository;
  Logger logger = org.slf4j.LoggerFactory.getLogger(BookService.class);

  public BookService(
      IsbnEnrichmentService isbnEnrichmentService,
      BookJpaRepository bookJpaRepository,
      BookBuilder bookBuilder,
      BookMapper bookMapper,
      IsbnLookupService isbnLookupService,
      AuthorAccessPort authorAccessPort,
      BookDomainRepository bookDomainRepository,
      ShelfAccessPort shelfAccessPort,
      BookcaseJpaRepository bookcaseJpaRepository) {
    this.isbnEnrichmentService = isbnEnrichmentService;
    this.bookJpaRepository = bookJpaRepository;
    this.BookBuilder = bookBuilder;
    this.bookMapper = bookMapper;
    this.isbnLookupService = isbnLookupService;
    this.authorAccessPort = authorAccessPort;
    this.bookDomainRepository = bookDomainRepository;
    this.shelfAccessPort = shelfAccessPort;
    this.bookcaseJpaRepository = bookcaseJpaRepository;
  }

  private void validateBookDoesNotExist(BookRequestDTO bookDTO) {
    Optional<BookDTO> bookEntity = findBookByTitleIgnoreCase(bookDTO.title());
    bookEntity.ifPresent(
        existingBook -> {
          throw new IllegalArgumentException("Book Already Exists: " + existingBook.title());
        });
  }

  // ============================================================
  //      READ Operations
  // ============================================================
  public Optional<BookDTO> findBookById(Long bookId) {
    BookEntity bookEntity = bookJpaRepository.findById(bookId).orElse(null);
    return Optional.of(BookDTO.fromEntity(bookEntity));
  }

  public List<BookEntity> findBooksByShelf(Long id) {
    return bookJpaRepository.findByShelfId(id);
  }

  public Optional<BookDTO> findBookByTitleIgnoreCase(String title) {
    Optional<BookEntity> bookEntity = bookJpaRepository.findByTitleIgnoreCase(title);
    return Optional.of(BookDTO.fromEntity(bookEntity.orElse(null)));
  }

  /**
   * Retrieves a book entity based on the given title. The search is case-insensitive. If no book
   * with the specified title is found, the method returns null.
   *
   * @param title the title of the book to search for
   * @return the book entity with the specified title, or null if no such book exists
   */
  public BookDTO findBookByTitle(String title) {
    Optional<BookEntity> bookEntity = bookJpaRepository.findByTitleIgnoreCase(title);
    if (bookEntity.isEmpty()) {
      return null;
    }
    return bookMapper.toDTOfromEntity(bookEntity.orElse(null));
  }

  public List<BookEntity> findBookByKeyword(String keyword) {
    List<BookEntity> bookEntities = bookJpaRepository.findByTitleContaining(keyword);
    for (BookEntity book : bookEntities) {
      System.out.println(book.getTitle());
    }
    return bookEntities;
  }

  public List<BookSummary> getBooksForShelf(Long shelfId) {
    return bookJpaRepository.findBookSummariesByShelfIdOrderByTitleAsc(shelfId);
  }

  // ============================================================
  // UPDATE Operations
  // ============================================================

  public BookEntity assignBookToShelf(Long bookId, Long shelfId) {
    BookEntity bookEntity =
        bookJpaRepository
            .findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
    ShelfDTO shelf =
        shelfAccessPort
            .findShelfById(shelfId)
            .orElseThrow(() -> new IllegalArgumentException("Shelf not found: " + shelfId));

    long bookCount = bookJpaRepository.countByShelfId(shelfId);
    if (bookCount >= shelf.bookCapacity()) {
      throw new IllegalStateException("Shelf is full");
    }

    bookEntity.setShelfId(shelfId);
    Book book = bookMapper.toDomainFromEntity(bookEntity);
    bookDomainRepository.updateBook(book);
    return bookEntity;
  }

  public void checkOutBook(BookDTO bookDTO) {
    bookDomainRepository.updateAvailabilityStatus(bookDTO.title());
  }

  public Book bookMapper(BookDTO bookDTO, Set<AuthorDTO> authorDTOs) {
    Optional<ShelfDTO> shelfEntity = shelfAccessPort.findShelfById(bookDTO.shelfId());
    return bookMapper.toDomain(bookDTO, authorDTOs, shelfEntity.orElse(null));
  }

  public void checkInBook(String bookTitle) {
    bookDomainRepository.updateAvailabilityStatus(bookTitle);
  }

  public BookDTO findBookByIsbn(String isbn) {
    BookEntity bookEntity = bookJpaRepository.findByIsbn(isbn);
    if (bookEntity == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with ISBN: " + isbn);
    }

    return bookMapper.toDTOfromEntity(bookEntity);
  }

  public List<BookDTO> getBooksByShelfId(Long shelfId) {
    List<BookEntity> bookEntities = bookJpaRepository.findByShelfId(shelfId);
    List<BookDTO> bookDTOs = new ArrayList<>();
    for (BookEntity bookEntity : bookEntities) {
      bookDTOs.add(bookMapper.toDTOfromEntity(bookEntity));
    }
    return bookDTOs;
  }

  public BookLocationResponse getBookLocation(Long bookId) {
    BookEntity bookEntity =
        bookJpaRepository
            .findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
    if (bookEntity.getShelfId() == null) {
      throw new IllegalStateException("Book is not currently assigned to a shelf");
    }
    ShelfDTO shelf =
        shelfAccessPort
            .findShelfById(bookEntity.getShelfId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Shelf not found for shelf ID: " + bookEntity.getShelfId()));

    BookcaseEntity bookcaseEntity =
        bookcaseJpaRepository
            .findById(shelf.bookcaseId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Bookcase not found for bookcase ID: " + shelf.bookcaseId()));

    return new BookLocationResponse(
        bookcaseEntity.getBookcaseLocation(),
        bookcaseEntity.getBookcaseLocation(),
        shelf.shelfLabel());
  }
}
