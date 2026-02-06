package com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound;

import com.penrose.bibby.library.stacks.bookcase.contracts.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.BookcaseEntity;
import java.util.List;
import java.util.Optional;

public interface BookcaseFacade {
  Optional<BookcaseDTO> findBookCaseById(Long aLong);

  /**
   * Retrieves a list of all bookcases in the library.
   *
   * @return A list of BookcaseDTO objects representing all bookcases.
   */
  List<BookcaseDTO> getAllBookcases();

  CreateBookcaseResult createNewBookCase(
      Long userId,
      String bookcaseLabel,
      String bookcaseZone,
      String bookcaseZoneIndex,
      int shelfCount,
      int bookCapacity,
      String location);

  List<String> getAllBookcaseLocations();

  Optional<BookcaseEntity> findById(Long bookcaseId);

  List<BookcaseDTO> getAllBookcasesByLocation(String location);

  List<BookcaseDTO> getAllBookcasesByUserId(Long appUserId);
}
