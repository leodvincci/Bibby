package com.penrose.bibby.library.stacks.shelf.core.ports.outbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;

public interface ShelfDomainRepositoryPort {

  Shelf getShelfByShelfId(ShelfId id);

  void createNewShelfInBookcase(Shelf shelf);

  void deleteByBookcaseId(Long bookcaseId);

  List<Shelf> findByBookcaseId(Long bookcaseId);

  List<Shelf> findAll();
}
