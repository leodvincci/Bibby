package com.penrose.bibby.library.bookcase.contracts;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

public interface BookcaseFacade {
    Optional<BookcaseDTO> findBookCaseById(Long aLong);

    List<BookcaseDTO> getAllBookcases();

    void createNewBookCase(String bookcaseLabel, int shelfCount, int bookCapacity);
}
