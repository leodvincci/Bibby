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
