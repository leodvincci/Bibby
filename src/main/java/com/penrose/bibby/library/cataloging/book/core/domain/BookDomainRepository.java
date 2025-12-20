package com.penrose.bibby.library.cataloging.book.core.domain;

import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDetailView;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;

import java.util.List;

public interface BookDomainRepository {

    List<Book> getBooksByShelfId(Long shelfId);

    void registerBook(Book book);

    void updateBook(Book book);

    BookEntity getBookById(Long id);


    BookEntity findBookEntityByTitle(String bookTitle);

    void updateTheBooksShelf(Book book,Long bookId, Long newShelfId);

    void updateAvailabilityStatus(String bookTitle);

    void registerBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId);

    BookEntity findBookByIsbn(String isbn);

    BookDetailView getBookDetailView(Long bookId);

    /**
     * Creates a new book record using the given metadata, author IDs, ISBN, and shelf ID.
     *
     * @param bookMetaDataResponse Contains metadata information of the book such as title, description, publisher, and authors.
     * @param authorIds List of IDs corresponding to the authors of the book.
     * @param isbn The ISBN of the book, used as a unique identifier.
     * @param shelfId The ID of the shelf where the book is to be placed.
     */
    void createBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, List<Long> authorIds, String isbn, Long shelfId);

    List<BookEntity> getThreeBooksByAuthorId(Long id);
}
