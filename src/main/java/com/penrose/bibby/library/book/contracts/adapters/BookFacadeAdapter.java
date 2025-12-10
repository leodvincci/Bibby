package com.penrose.bibby.library.book.contracts.adapters;

import com.penrose.bibby.library.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.book.contracts.dtos.*;
import com.penrose.bibby.library.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.book.core.domain.Book;
import com.penrose.bibby.library.book.core.domain.BookDomainRepository;
import com.penrose.bibby.library.book.core.domain.Isbn;
import com.penrose.bibby.library.book.core.domain.Title;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.book.infrastructure.mapping.BookMapper;
import com.penrose.bibby.library.book.infrastructure.repository.BookJpaRepository;
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

    public BookFacadeAdapter(BookDomainRepository bookDomainRepository, AuthorFacade authorFacade, BookMapper bookMapper) {
        this.bookDomainRepository = bookDomainRepository;
        this.authorFacade = authorFacade;
        this.bookMapper = bookMapper;
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
    @Transactional
    public BookDTO findBookByTitle(String title) {
        return BookDTO.fromEntity(bookDomainRepository.findBookEntityByTitle(title));
    }

    @Override
    public BookMetaDataResponse findBookMetaDataByIsbn(String isbn) {
        return null;
    }

    @Override
    public void createBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId) {

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
}
