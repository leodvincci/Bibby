package com.penrose.bibby.library.cataloging.book.contracts.ports.inbound;
import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.*;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;

import java.util.List;
import java.util.Optional;

public interface BookFacade {
    void updateTheBooksShelf(BookDTO bookDTO, Long newShelfId);

    BookDTO findBookByIsbn(String isbn);

    BriefBibliographicRecord findBookBriefByShelfId(Long bookId);

    BookDTO findBookByTitle(String title);

    BookMetaDataResponse findBookMetaDataByIsbn(String isbn);

    /**
     * Creates a new book in the system using metadata, author details, and additional information
     * such as ISBN and shelf ID. This method facilitates the process of registering a book
     * in the library database with its corresponding metadata and location.
     *
     * @param bookMetaDataResponse the metadata response containing book details such as title, authors,
     *                             publisher, and description
     * @param authorIds            the list of IDs of the authors associated with the book
     * @param isbn                 the ISBN of the book being created
     * @param shelfId              the ID of the shelf where the book will be placed
     */
    void createBookFromMetaData(BookMetaDataResponse bookMetaDataResponse,List<Long> authorIds, String isbn, Long shelfId);

    void createNewBook(BookRequestDTO bookRequestDTO);

    void checkOutBook(BookDTO bookDTO);

    void checkInBook(String bookTitle);

    List<BookSummary> getBooksForShelf(Long shelfId);

    BookDetailView getBookDetails(Long bookId);

    Optional<BookDTO> findBookById(Long bookId);

    List<String> getBooksByAuthorId(Long id);

    List<BriefBibliographicRecord> getBriefBibliographicRecordsByShelfId(Long shelfId);
}
