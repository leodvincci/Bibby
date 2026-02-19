package com.penrose.bibby.library.stacks.shelf.core.ports.inbound;

import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import java.util.List;
import java.util.Optional;

public interface ShelfFacade {

  List<ShelfDTO> getAllDTOShelves(Long bookcaseId);

  Optional<ShelfDTO> findShelfById(Long shelfId);

  List<ShelfDTO> findByBookcaseId(Long aLong);

  List<BookDTO> findBooksByShelf(Long aLong);

  List<ShelfSummary> getShelfSummariesForBookcase(Long bookCaseId);

  void deleteAllShelvesInBookcase(Long bookcaseId);

  Boolean isFull(ShelfDTO shelfDTO);

  void createShelf(Long bookcaseId, int position, String s, int bookCapacity);
}
