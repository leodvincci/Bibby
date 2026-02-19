package com.penrose.bibby.library.stacks.shelf.core.ports.outbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;

public interface ShelfDomainRepository {

  Shelf getById(ShelfId id);

  void save(Long bookcaseId, int position, String shelfLabel, int bookCapacity);

  Long getBookcaseIdByShelfId(Long shelfId);

  List<Shelf> findByBookcaseId(Long bookCaseId);

  List<Shelf> findShelfSummariesByBookcaseId(Long bookcaseId);

  void deleteByBookcaseId(Long bookcaseId);

  Shelf findById(Long shelfId);

  List<Shelf> getShelfShelfOptionResponse(Long bookcaseId);

  List<Shelf> findAll();

  // optional: queries that return Shelf or domain read models

}
