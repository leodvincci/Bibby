package com.penrose.bibby.library.cataloging.book.api.adapters;

import com.penrose.bibby.library.cataloging.author.core.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.api.dtos.*;
import com.penrose.bibby.library.cataloging.book.core.application.IsbnLookupService;
import com.penrose.bibby.library.cataloging.book.core.domain.model.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Isbn;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Title;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.BookDomainRepository;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.cataloging.book.infrastructure.mapping.BookMapper;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookFacadeAdapter implements BookFacade {
  private final AuthorFacade authorFacade;
  Logger log = org.slf4j.LoggerFactory.getLogger(BookFacadeAdapter.class);
  BookDomainRepository bookDomainRepository;
  BookMapper bookMapper;
  IsbnLookupService isbnLookupService;

  public BookFacadeAdapter(
      BookDomainRepository bookDomainRepository,
      AuthorFacade authorFacade,
      BookMapper bookMapper,
      IsbnLookupService isbnLookupService) {
    this.bookDomainRepository = bookDomainRepository;
    this.authorFacade = authorFacade;
    this.bookMapper = bookMapper;
    this.isbnLookupService = isbnLookupService;
  }

  @Override
  public void updateTheBooksShelf(BookDTO bookDTO, Long newShelfId) {
    Book book = bookMapper.toDomainFromDTO(bookDTO);
    Long bookId = bookDTO.id();
    bookDomainRepository.updateTheBooksShelf(book, bookId, newShelfId);
    log.info("Updated book with title {} to shelf with id {}", bookDTO.title(), newShelfId);
  }

  @Override
  public BookDTO findBookByIsbn(String isbn) {
    return BookDTO.fromEntity(bookDomainRepository.findBookByIsbn(isbn));
  }

  @Override
  public BriefBibliographicRecord findBookBriefByShelfId(Long bookId) {
    return bookMapper.toBookBriefFromEntity(bookDomainRepository.getBookById(bookId));
  }

  @Override
  @Transactional
  public BookDTO findBookByTitle(String title) {
    return BookDTO.fromEntity(bookDomainRepository.findBookEntityByTitle(title));
  }

  /**
   * Retrieves metadata for a book based on its ISBN. This method queries an external service to
   * fetch the book details and maps the response to a {@link BookMetaDataResponse}.
   *
   * @param isbn the ISBN of the book for which metadata is to be retrieved
   * @return a {@link BookMetaDataResponse} containing the metadata of the book, including title,
   *     authors, publisher, and optional description
   */
  @Override
  public BookMetaDataResponse findBookMetaDataByIsbn(String isbn) {
    GoogleBooksResponse googleBooksResponse = isbnLookupService.lookupBook(isbn).block();
    log.info("Fetched book metadata for ISBN: {}", isbn);
    return bookMapper.toBookMetaDataResponseFromGoogleBooksResponse(googleBooksResponse, isbn);
  }

  /**
   * Creates a new book record using metadata, author IDs, ISBN, and shelf ID.
   *
   * @param bookMetaDataResponse Contains metadata information of the book such as title,
   *     description, publisher, and authors.
   * @param authorIds List of IDs representing the authors of the book.
   * @param isbn The ISBN of the book, serving as a unique identifier.
   * @param shelfId The ID of the shelf where the book should be stored.
   */
  @Override
  public void createBookFromMetaData(
      BookMetaDataResponse bookMetaDataResponse, List<Long> authorIds, String isbn, Long shelfId) {
    bookDomainRepository.createBookFromMetaData(bookMetaDataResponse, authorIds, isbn, shelfId);
  }

  @Override
  public void createNewBook(BookRequestDTO bookRequestDTO) {
    log.info("Creating new book with title: {}", bookRequestDTO.title());
    Book book = bookMapper.toDomainFromBookRequestDTO(bookRequestDTO);
    book.setIsbn(new Isbn(bookRequestDTO.isbn()));
    book.setTitle(new Title(bookRequestDTO.title()));
    book.setShelfId(bookRequestDTO.bookshelfId());
    book.setPublisher(bookRequestDTO.publisher());
    log.info("Mapped BookRequestDTO to Book domain object: {}", book);
    bookDomainRepository.registerBook(book);
    log.info("Book registered successfully in the repository.");
  }

  @Override
  public void checkOutBook(BookDTO bookDTO) {}

  @Override
  public void checkInBook(String bookTitle) {}

  @Override
  public List<BookSummary> getBooksForShelf(Long shelfId) {
    return List.of();
  }

  @Override
  public BookDetailView getBookDetails(Long bookId) {
    return bookDomainRepository.getBookDetailView(bookId);
  }

  @Override
  public Optional<BookDTO> findBookById(Long bookId) {
    return Optional.of(bookMapper.toDTOfromEntity(bookDomainRepository.getBookById(bookId)));
  }

  @Override
  public List<String> getBooksByAuthorId(Long id) {
    List<BookEntity> bookEntities = bookDomainRepository.getThreeBooksByAuthorId(id);
    log.info(bookEntities.size() + " books found for author id: " + id);
    List<String> bookTitles = bookEntities.stream().limit(3).map(BookEntity::getTitle).toList();
    log.info("Book titles: " + bookTitles);
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
    bookDomainRepository.deleteByShelfIdIn(shelfIds);
  }

  @Override
  public List<BookDTO> findByShelfId(Long shelfId) {
    return bookDomainRepository.getBooksByShelfId(shelfId);
  }
}
