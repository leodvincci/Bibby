package com.penrose.bibby.library.catalog.book.infrastructure.repository;

import com.penrose.bibby.library.catalog.author.core.application.AuthorService;
import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
import com.penrose.bibby.library.catalog.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.catalog.book.contracts.dtos.BookDetailView;
import com.penrose.bibby.library.catalog.book.contracts.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.catalog.book.core.domain.Book;
import com.penrose.bibby.library.catalog.book.core.domain.BookDomainRepository;
import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.catalog.book.infrastructure.mapping.BookMapper;
import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class BookDomainRepositoryImpl implements BookDomainRepository {
    private final BookMapper  bookMapper;
    private final BookJpaRepository bookJpaRepository;
    Logger log = org.slf4j.LoggerFactory.getLogger(BookDomainRepositoryImpl.class);
    public BookDomainRepositoryImpl(
            BookMapper bookMapper,
            BookJpaRepository bookJpaRepository,
            AuthorService authorService) {

        this.bookMapper = bookMapper;
        this.bookJpaRepository = bookJpaRepository;

    }

    @Override
    public List<Book> getBooksByShelfId(Long shelfId) {
        List<BookEntity> bookEntities = bookJpaRepository.findByShelfId(shelfId);
        List<Book> books = new ArrayList<>();

        Set<AuthorDTO> authorDTO = null;
        ShelfDTO shelfDTO = null;

        for(BookEntity bookEntity : bookEntities){
            Book book = bookMapper.toDomainFromEntity(bookEntity);
            books.add(book);
        }
        return books;
    }

    // todo: looks like isbn and title are different value types, need to fix
    // todo(Leo): create a factory for BookEntity creation
    @Override
    public void registerBook(Book book) {
        log.info("Mapping book domain to entity for book: " + book.getTitle().title());
        BookEntity bookEntity = new BookEntity();
        bookEntity.setIsbn(book.getIsbn().isbn);
        bookEntity.setTitle(book.getTitle().title());
        bookEntity.setGenre(book.getGenre());
        bookEntity.setDescription(book.getDescription());
        bookEntity.setAvailabilityStatus("AVAILABLE");
        bookEntity.setPublicationYear(book.getPublicationYear());
        bookEntity.setCreatedAt(LocalDate.now());
        bookEntity.setUpdatedAt(LocalDate.now());
        log.info("Registering new book: " + book.getTitle().title());
        log.info("Book registered with id: " + bookEntity.getBookId());

        Set<AuthorEntity> authorEntities = bookMapper.toEntitySetFromAuthorRefs(book.getAuthors());
        log.info(authorEntities.toString());
        bookEntity.setAuthors(authorEntities);

        bookJpaRepository.save(bookEntity);
    }

    @Override
    public void updateBook(Book book) {
        BookEntity bookEntity = bookJpaRepository.findById(book.getBookId().getId()).get();
        bookEntity.setIsbn(book.getIsbn().isbn);
        bookEntity.setTitle(book.getTitle().title());
        bookEntity.setShelfId(book.getShelfId());
        bookEntity.setGenre(book.getGenre());
        bookEntity.setDescription(book.getDescription());
        bookEntity.setAvailabilityStatus(book.getAvailabilityStatus().toString());
        Set<AuthorEntity> authorEntities = bookMapper.toEntitySetFromAuthorRefs(book.getAuthors());
        bookEntity.setAuthors(authorEntities);
        bookEntity.setPublicationYear(book.getPublicationYear());
        bookEntity.setUpdatedAt(LocalDate.now());
        bookJpaRepository.save(bookEntity);
    }

    @Override
    public BookEntity getBookById(Long id) {
        return bookJpaRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }

    @Override
    public BookEntity findBookEntityByTitle(String bookTitle) {
        return bookJpaRepository.findByTitle(bookTitle);
    }


    @Override
    public void updateTheBooksShelf(Book book, Long bookId, Long newShelfId) {
        Optional<BookEntity> bookEntity = bookJpaRepository.findById(bookId);

        if (bookEntity.isEmpty()) {
            log.error("Book with id {} not found", bookId);
            throw new RuntimeException("Book not found with id: " + bookId);
        }

        bookEntity.get().setShelfId(newShelfId);
        log.info("Updated shelf id for book: {} to {}", book.getTitle(), newShelfId);

        bookJpaRepository.save(bookEntity.get());
        log.info("Persisted Book with title {} updated to database with id {}", book.getTitle(), newShelfId);
    }

    @Override
    public void updateAvailabilityStatus(String bookTitle) {

    }

    @Override
    public void registerBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId) {

    }

    @Override
    public BookEntity findBookByIsbn(String isbn) {
        return bookJpaRepository.findByIsbn(isbn);
    }

    @Override
    public BookDetailView getBookDetailView(Long bookId) {
        return bookJpaRepository.getBookDetailView(bookId);
    }

    @Override
    public void createBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId) {
        BookEntity bookEntity = bookMapper.toEntityFromBookMetaDataResponse(bookMetaDataResponse, isbn, shelfId);
        bookJpaRepository.save(bookEntity);
        System.out.println("\n\u001B[32mBook Successfully Imported\u001B[0m");
        System.out.println("--------------------------------");
        System.out.printf(" Title  : %s%n", bookEntity.getTitle());
        System.out.printf(" Author : %s%n", bookMetaDataResponse.authors());
        System.out.printf(" ISBN   : %s%n", isbn);
        System.out.println("--------------------------------");
        log.info("Created book from metadata with title: {} and ISBN: {}", bookEntity.getTitle(), isbn);
    }

    @Override
    public List<BookEntity> getThreeBooksByAuthorId(Long id) {
        return bookJpaRepository.findByAuthorsAuthorId(id);
    }
}
