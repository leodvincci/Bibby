package com.penrose.bibby.library.book.contracts.ports.inbound;

import com.penrose.bibby.library.book.contracts.dtos.*;

import java.util.List;
import java.util.Optional;

public interface BookFacade {
    void setShelfForBook(Long id, Long shelfId);

    BookDTO findBookByIsbn(String isbn);

    BookDTO findBookByTitle(String title);

    BookMetaDataResponse findBookMetaDataByIsbn(String isbn);

    void createBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId);

    void createNewBook(BookRequestDTO bookRequestDTO);

    void checkOutBook(BookDTO bookDTO);

    void checkInBook(String bookTitle);

    List<BookSummary> getBooksForShelf(Long shelfId);

    BookDetailView getBookDetails(Long bookId);

    Optional<BookDTO> findBookById(Long bookId);
}
