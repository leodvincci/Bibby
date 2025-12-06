package com.penrose.bibby.library.shelf.contracts;

import com.penrose.bibby.library.book.contracts.BookDTO;

import java.util.List;
import java.util.Optional;

public interface ShelfFacade {

    List<ShelfDTO> getAllDTOShelves(Long bookcaseId);

    Optional<ShelfDTO> findShelfById(Long shelfId);

    List<ShelfDTO> findByBookcaseId(Long aLong);

    List<BookDTO> findBooksByShelf(Long aLong);

    List<ShelfSummary> getShelfSummariesForBookcase(Long bookCaseId);
}


