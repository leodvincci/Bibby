package com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound;

import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfSummary;

import java.util.List;
import java.util.Optional;

public interface ShelfFacade {

    List<ShelfDTO> getAllDTOShelves(Long bookcaseId);

    Optional<ShelfDTO> findShelfById(Long shelfId);

    List<ShelfDTO> findByBookcaseId(Long aLong);

    List<BookDTO> findBooksByShelf(Long aLong);

    List<ShelfSummary> getShelfSummariesForBookcase(Long bookCaseId);


    Boolean isFull(ShelfDTO shelfDTO);
}


