package com.penrose.bibby.library.stacks.shelf.core.application;

import com.penrose.bibby.library.stacks.shelf.core.application.usecases.CreateShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.DeleteShelvesUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.QueryShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ShelfService implements ShelfFacade {

  private final QueryShelfUseCase queryShelfUseCase;
  private final CreateShelfUseCase createShelfUseCase;
  private final DeleteShelvesUseCase deleteShelvesUseCase;

  public ShelfService(
      QueryShelfUseCase queryShelfUseCase,
      CreateShelfUseCase createShelfUseCase,
      DeleteShelvesUseCase deleteShelvesUseCase) {
    this.queryShelfUseCase = queryShelfUseCase;
    this.createShelfUseCase = createShelfUseCase;
    this.deleteShelvesUseCase = deleteShelvesUseCase;
  }

  @Override
  public List<Shelf> findAllShelves(Long bookCaseId) {
    return queryShelfUseCase.findAllShelves(bookCaseId);
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
    createShelfUseCase.execute(bookcaseId, position, shelfLabel, bookCapacity);
  }

  @Override
  public List<Shelf> findAll() {
    return queryShelfUseCase.findAll();
  }
}
