package com.penrose.bibby.library.book.core.domain;

import com.penrose.bibby.library.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.book.contracts.dtos.BookDetailView;
import com.penrose.bibby.library.book.contracts.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;

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
}
