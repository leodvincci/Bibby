package com.penrose.bibby.library.stacks.shelf.core.domain;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;

public interface ShelfDomainRepository {

  Shelf getById(ShelfId id);

  void save(Shelf shelf);

  // optional: queries that return Shelf or domain read models

}
