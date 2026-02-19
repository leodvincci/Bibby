package com.penrose.bibby.library.stacks.bookcase.core.domain.ports.outbound;

import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import java.util.List;
import java.util.Optional;

public interface BookcaseRepository {
  BookcaseEntity findBookcaseEntityByBookcaseLocation(String s);

  List<BookcaseEntity> findAll();

  List<String> getAllBookCaseLocations();

  BookcaseEntity save(BookcaseEntity bookcaseEntity);

  Optional<BookcaseEntity> findById(Long id);

  List<BookcaseEntity> findByLocation(String location);

  List<BookcaseEntity> findByAppUserId(Long appUserId);
}
