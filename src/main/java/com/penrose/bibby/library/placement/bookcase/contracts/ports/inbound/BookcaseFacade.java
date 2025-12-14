package com.penrose.bibby.library.placement.bookcase.contracts.ports.inbound;

import com.penrose.bibby.library.placement.bookcase.contracts.dtos.BookcaseDTO;

import java.util.List;
import java.util.Optional;

public interface BookcaseFacade {
    Optional<BookcaseDTO> findBookCaseById(Long aLong);

    List<BookcaseDTO> getAllBookcases();

    String createNewBookCase(String bookcaseLabel, int shelfCount, int bookCapacity);
}
