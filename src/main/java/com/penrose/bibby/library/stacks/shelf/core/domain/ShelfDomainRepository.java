package com.penrose.bibby.library.stacks.shelf.core.domain;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;

public interface ShelfDomainRepository {

  Shelf getById(ShelfId id);

  void save(Long bookcaseId, int position, String shelfLabel, int bookCapacity);

  Long getBookcaseIdByShelfId(Long shelfId);

  List<Shelf> findByBookcaseId(Long bookCaseId);

  List<ShelfSummary> findShelfSummariesByBookcaseId(Long bookcaseId);

  void deleteByBookcaseId(Long bookcaseId);

  Shelf findById(Long shelfId);

  List<ShelfOptionResponse> getShelfShelfOptionResponse(Long bookcaseId);

  List<Shelf> findAll();

  // optional: queries that return Shelf or domain read models

}
