package com.penrose.bibby.library.cataloging.book.contracts.adapters;

import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.*;
import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.cataloging.book.core.application.IsbnLookupService;
import com.penrose.bibby.library.cataloging.book.core.domain.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.BookDomainRepository;
import com.penrose.bibby.library.cataloging.book.core.domain.Isbn;
import com.penrose.bibby.library.cataloging.book.core.domain.Title;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.cataloging.book.infrastructure.mapping.BookMapper;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class BookFacadeAdapter implements BookFacade {
    private final AuthorFacade authorFacade;
    Logger log = org.slf4j.LoggerFactory.getLogger(BookFacadeAdapter.class);
    BookDomainRepository bookDomainRepository;
    BookMapper bookMapper;
    IsbnLookupService isbnLookupService;

    public BookFacadeAdapter(BookDomainRepository bookDomainRepository, AuthorFacade authorFacade, BookMapper bookMapper, IsbnLookupService isbnLookupService) {
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

    @Override
    public BookMetaDataResponse findBookMetaDataByIsbn(String isbn) {
        GoogleBooksResponse googleBooksResponse = isbnLookupService.lookupBook(isbn).block();
        log.info("Fetched book metadata for ISBN: {}", isbn);
        return bookMapper.toBookMetaDataResponseFromGoogleBooksResponse(googleBooksResponse,isbn);
    }

    @Override
    public void createBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, List<Long> authorIds, String isbn, Long shelfId) {
        bookDomainRepository.createBookFromMetaData(bookMetaDataResponse, authorIds, isbn, shelfId);
    }

    @Override
    public void createNewBook(BookRequestDTO bookRequestDTO) {
        log.info("Creating new book with title: {}", bookRequestDTO.title());
        Book book = bookMapper.toDomainFromBookRequestDTO(bookRequestDTO);
        book.setIsbn(new Isbn(bookRequestDTO.isbn()));
        book.setTitle(new Title(bookRequestDTO.title()));
        log.info("Registering book with title: {} and ISBN: {}", book.getTitle().title(), book.getIsbn().isbn);
        bookDomainRepository.registerBook(book);

    }

    @Override
    public void checkOutBook(BookDTO bookDTO) {

    }

    @Override
    public void checkInBook(String bookTitle) {

    }

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
        log.info(bookEntities .size() + " books found for author id: " + id);
        List<String> bookTitles = bookEntities.stream().limit(3).map(BookEntity::getTitle).toList();
        log.info("Book titles: " + bookTitles);
        return bookTitles;
    }


    @Override
    public List<BriefBibliographicRecord> getBriefBibliographicRecordsByShelfId(Long shelfId) {
        return bookMapper.toBookBriefListFromEntities(
                bookDomainRepository.getBooksByShelfId(shelfId)
        );
    }
}
