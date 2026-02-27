package com.penrose.bibby.library.cataloging.book.core.application;

import com.penrose.bibby.library.cataloging.book.api.dtos.*;
import com.penrose.bibby.library.cataloging.book.core.application.usecases.BookCommandUseCases;
import com.penrose.bibby.library.cataloging.book.core.domain.BookBuilder;
import com.penrose.bibby.library.cataloging.book.core.domain.model.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Isbn;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Title;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.BookDomainRepository;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.ShelfAccessPort;
import com.penrose.bibby.library.cataloging.book.infrastructure.adapter.mapping.BookMapper;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.repository.BookcaseJpaRepository;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import java.util.*;
import org.slf4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService implements BookFacade {
  private final BookJpaRepository bookJpaRepository;

  private final BookBuilder BookBuilder;
  private final BookMapper bookMapper;
  private final IsbnLookupService isbnLookupService;
  private final IsbnEnrichmentService isbnEnrichmentService;
  private final BookDomainRepository bookDomainRepository;
  private final ShelfAccessPort shelfAccessPort;
  private final BookcaseJpaRepository bookcaseJpaRepository;
  private final BookCommandUseCases bookCommandUseCases;
  Logger logger = org.slf4j.LoggerFactory.getLogger(BookService.class);

  public BookService(
      IsbnEnrichmentService isbnEnrichmentService,
      BookJpaRepository bookJpaRepository,
      BookBuilder bookBuilder,
      BookMapper bookMapper,
      IsbnLookupService isbnLookupService,
      BookDomainRepository bookDomainRepository,
      @Lazy ShelfAccessPort shelfAccessPort,
      BookcaseJpaRepository bookcaseJpaRepository,
      BookCommandUseCases bookCommandUseCases) {
    this.isbnEnrichmentService = isbnEnrichmentService;
    this.bookJpaRepository = bookJpaRepository;
    this.BookBuilder = bookBuilder;
    this.bookMapper = bookMapper;
    this.isbnLookupService = isbnLookupService;
    this.bookDomainRepository = bookDomainRepository;
    this.shelfAccessPort = shelfAccessPort;
    this.bookcaseJpaRepository = bookcaseJpaRepository;
    this.bookCommandUseCases = bookCommandUseCases;
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
  @Override
  public Optional<BookDTO> findBookById(Long bookId) {
    Optional<BookEntity> bookEntity = bookJpaRepository.findById(bookId);
    return bookEntity.map(BookDTO::fromEntity);
  }

  public List<Book> findBooksByShelf(Long id) {
    return findByShelfId(id);
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
  @Override
  @Transactional
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

  @Override
  public List<BookSummary> getBooksForShelf(Long shelfId) {
    return bookJpaRepository.findBookSummariesByShelfIdOrderByTitleAsc(shelfId);
  }

  // ============================================================
  // UPDATE Operations
  // ============================================================

  public Book assignBookToShelf(Long bookId, Long shelfId) {
    return bookDomainRepository.placeBookOnShelf(bookId, shelfId);
  }

  @Override
  public void checkOutBook(BookDTO bookDTO) {
    bookDomainRepository.updateAvailabilityStatus(bookDTO.title());
  }

  @Override
  public void checkInBook(String bookTitle) {
    bookDomainRepository.updateAvailabilityStatus(bookTitle);
  }

  @Override
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

  // ============================================================
  //      BookFacade Implementation
  // ============================================================

  @Override
  public void updateTheBooksShelf(BookDTO bookDTO, Long newShelfId) {
    Book book = bookMapper.toDomainFromDTO(bookDTO);
    Long bookId = bookDTO.id();
    bookDomainRepository.updateTheBooksShelf(book, bookId, newShelfId);
    logger.info("Updated book with title {} to shelf with id {}", bookDTO.title(), newShelfId);
  }

  @Override
  public BriefBibliographicRecord findBookBriefByShelfId(Long bookId) {
    return bookMapper.toBookBriefFromEntity(bookDomainRepository.getBookById(bookId));
  }

  @Override
  public BookMetaDataResponse findBookMetaDataByIsbn(String isbn) {
    GoogleBooksResponse googleBooksResponse = isbnLookupService.lookupBook(isbn).block();
    logger.info("Fetched book metadata for ISBN: {}", isbn);
    return bookMapper.toBookMetaDataResponseFromGoogleBooksResponse(googleBooksResponse, isbn);
  }

  @Override
  public void createBookFromMetaData(
      BookMetaDataResponse bookMetaDataResponse, List<Long> authorIds, String isbn, Long shelfId) {
    bookDomainRepository.createBookFromMetaData(bookMetaDataResponse, authorIds, isbn, shelfId);
  }

  @Override
  public void createNewBook(BookRequestDTO bookRequestDTO) {
    logger.info("Creating new book with title: {}", bookRequestDTO.title());
    Book book = bookMapper.toDomainFromBookRequestDTO(bookRequestDTO);
    book.setIsbn(new Isbn(bookRequestDTO.isbn()));
    book.setTitle(new Title(bookRequestDTO.title()));
    book.setShelfId(bookRequestDTO.bookshelfId());
    book.setPublisher(bookRequestDTO.publisher());
    logger.info("Mapped BookRequestDTO to Book domain object: {}", book);
    bookDomainRepository.registerBook(book);
    logger.info("Book registered successfully in the repository.");
  }

  @Override
  public BookDetailView getBookDetails(Long bookId) {
    return bookDomainRepository.getBookDetailView(bookId);
  }

  @Override
  public List<String> getBooksByAuthorId(Long id) {
    List<BookEntity> bookEntities = bookDomainRepository.getThreeBooksByAuthorId(id);
    logger.info(bookEntities.size() + " books found for author id: " + id);
    List<String> bookTitles = bookEntities.stream().limit(3).map(BookEntity::getTitle).toList();
    logger.info("Book titles: " + bookTitles);
    return bookTitles;
  }

  @Override
  public List<BriefBibliographicRecord> getBriefBibliographicRecordsByShelfId(Long shelfId) {
    return bookMapper.toBookBriefListFromBookDTOs(bookDomainRepository.getBooksByShelfId(shelfId));
  }

  @Override
  public void updatePublisher(String isbn, String newPublisher) {
    bookDomainRepository.updatePublisher(isbn, newPublisher);
  }

  @Override
  public boolean isDuplicate(String isbn) {
    return bookDomainRepository.findBookByIsbn(isbn) != null;
  }

  @Override
  public void deleteByShelfIdIn(List<Long> shelfIds) {
    bookDomainRepository.deleteByShelfId(shelfIds);
  }

  @Override
  public List<Book> findByShelfId(Long shelfId) {
    List<BookEntity> entities = bookJpaRepository.findByShelfId(shelfId);
    List<Book> books = new ArrayList<>();
    for (BookEntity entity : entities) {
      books.add(bookMapper.toDomainFromEntity(entity));
    }
    return books;
  }

  @Override
  public void placeBookOnShelf(Long bookId, BookShelfAssignmentRequest shelfAssignmentRequest) {
    bookCommandUseCases.placeBookOnShelf(bookId, shelfAssignmentRequest);
  }

  @Override
  public void deleteByShelfId(List<Long> shelfIds) {
    bookDomainRepository.deleteByShelfId(shelfIds);
  }

  @Override
  public List<Long> getBookIdsByShelfId(Long shelfId) {
    return bookDomainRepository.getBookIdsByShelfId(shelfId);
  }
}
