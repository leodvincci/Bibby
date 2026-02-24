package com.penrose.bibby.library.stacks.bookcase.core.ports.outbound;

import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import java.util.List;
import java.util.Optional;

public interface BookcaseRepository {
  Bookcase findBookcaseByBookcaseLocation(String bookcaseLocation);

  List<Bookcase> findAll();

  List<String> getAllBookCaseLocations();

  void save(Bookcase bookcaseEntity);

  Bookcase findById(Long id);

  List<Bookcase> findByLocation(String location);

  List<Bookcase> findByAppUserId(Long appUserId);

  void deleteById(Long bookcaseId);
}
