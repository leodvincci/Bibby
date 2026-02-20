package com.penrose.bibby.library.stacks.shelf.core.ports.outbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;

public interface ShelfDomainRepository {

  Shelf getById(ShelfId id);

  void save(Long bookcaseId, int position, String shelfLabel, int bookCapacity);

  void deleteByBookcaseId(Long bookcaseId);

  List<Shelf> findByBookcaseId(Long bookcaseId);

  Shelf findById(Long shelfId);

  List<Shelf> findAll();
}
