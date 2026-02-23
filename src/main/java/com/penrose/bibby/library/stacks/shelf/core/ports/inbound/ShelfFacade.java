package com.penrose.bibby.library.stacks.shelf.core.ports.inbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import java.util.List;
import java.util.Optional;

public interface ShelfFacade {

  List<Shelf> findShelvesByBookcaseId(Long bookcaseId);

  Optional<Shelf> findShelfById(Long shelfId);

  List<ShelfSummary> getShelfSummariesForBookcase(Long bookCaseId);

  void deleteAllShelvesInBookcase(Long bookcaseId);

  void createShelf(Long bookcaseId, int position, String s, int bookCapacity);

  List<Shelf> findAll();

  boolean isFull(Long shelfId);

  boolean isEmpty(Long shelfId);
}
