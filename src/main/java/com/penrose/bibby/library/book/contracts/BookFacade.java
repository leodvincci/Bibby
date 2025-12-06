package com.penrose.bibby.library.book.contracts;

import com.penrose.bibby.library.bookcase.contracts.BookcaseDTO;

import java.util.List;
import java.util.Optional;

public interface BookFacade {
    void setShelfForBook(Long id, Long shelfId);

    BookDTO findBookByIsbn(String isbn);

    BookDTO findBookByTitle(String title);

    BookMetaDataResponse findBookMetaDataByIsbn(String isbn);

    void createBookFromMetaData(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId);

    void createNewBook(BookRequestDTO bookRequestDTO);
}
