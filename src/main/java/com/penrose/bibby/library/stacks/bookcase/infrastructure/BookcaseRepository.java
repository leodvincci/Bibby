package com.penrose.bibby.library.stacks.bookcase.infrastructure;

import java.util.List;
import java.util.Optional;

public interface BookcaseRepository {
  BookcaseEntity findBookcaseEntityByBookcaseLabel(String s);

  List<BookcaseEntity> findAll();

  List<String> getAllBookCaseLocations();

  BookcaseEntity save(BookcaseEntity bookcaseEntity);

  Optional<BookcaseEntity> findById(Long id);

  List<BookcaseEntity> findByLocation(String location);

  List<BookcaseEntity> findByAppUserId(Long appUserId);
}
