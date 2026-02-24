package com.penrose.bibby.library.stacks.shelf.core.application;

import com.penrose.bibby.library.stacks.shelf.core.application.usecases.CreateShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.DeleteShelvesUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.QueryShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class ShelfService implements ShelfFacade {

  private final QueryShelfUseCase queryShelfUseCase;
  private final CreateShelfUseCase createShelfUseCase;
  private final DeleteShelvesUseCase deleteShelvesUseCase;
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(ShelfService.class);

  public ShelfService(
      QueryShelfUseCase queryShelfUseCase,
      CreateShelfUseCase createShelfUseCase,
      DeleteShelvesUseCase deleteShelvesUseCase) {
    this.queryShelfUseCase = queryShelfUseCase;
    this.createShelfUseCase = createShelfUseCase;
    this.deleteShelvesUseCase = deleteShelvesUseCase;
  }

  @Override
  public List<Shelf> findShelvesByBookcaseId(Long bookcaseId) {
    return queryShelfUseCase.findShelvesByBookcaseId(bookcaseId);
  }

  @Override
  public Optional<Shelf> findShelfById(Long shelfId) {
    return queryShelfUseCase.findShelfById(shelfId);
  }

  @Override
  public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
    return queryShelfUseCase.getShelfSummariesForBookcase(bookcaseId);
  }

  @Override
  public void deleteAllShelvesInBookcase(Long bookcaseId) {
    deleteShelvesUseCase.execute(bookcaseId);
  }

  @Override
  public void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    Shelf shelf = new Shelf(shelfLabel, position, bookCapacity, null, List.of(), bookcaseId);
    createShelfUseCase.execute(shelf);
  }

  @Override
  public List<Shelf> findAll() {
    return queryShelfUseCase.findAll();
  }

  @Override
  public boolean isFull(Long shelfId) {
    return queryShelfUseCase.isFull(shelfId);
  }

  @Override
  public boolean isEmpty(Long shelfId) {
    return queryShelfUseCase.isEmpty(shelfId);
  }
}
