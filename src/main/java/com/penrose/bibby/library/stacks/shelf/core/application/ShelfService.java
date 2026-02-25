package com.penrose.bibby.library.stacks.shelf.core.application;

import com.penrose.bibby.library.stacks.shelf.core.application.usecases.CreateShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.DeleteShelvesUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.QueryShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for managing shelf-related operations. This class delegates to
 * application use cases for the core shelf business logic.
 */
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

  /** {@inheritDoc} */
  @Override
  public List<ShelfResponse> findShelvesByBookcaseId(Long bookcaseId) {
    return queryShelfUseCase.findShelvesByBookcaseId(bookcaseId);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<Shelf> findShelfById(Long shelfId) {
    return queryShelfUseCase.findShelfById(shelfId);
  }

  /** {@inheritDoc} */
  @Override
  public List<ShelfSummary> getShelfSummariesForBookcaseByBookcaseId(Long bookcaseId) {
    return queryShelfUseCase.getShelfSummariesForBookcaseById(bookcaseId);
  }

  /**
   * Deletes every shelf belonging to the specified bookcase. Delegates to {@link
   * DeleteShelvesUseCase} for the actual removal.
   *
   * @param bookcaseId the ID of the bookcase whose shelves should be deleted
   */
  @Override
  public void deleteAllShelvesInBookcaseByBookcaseId(Long bookcaseId) {
    deleteShelvesUseCase.execute(bookcaseId);
  }

  /**
   * Creates a new shelf within a bookcase and persists it. Constructs a {@link Shelf} domain object
   * from the supplied parameters and delegates persistence to {@link CreateShelfUseCase}.
   *
   * @param bookcaseId the ID of the bookcase that will contain the new shelf
   * @param position the ordinal position of the shelf within the bookcase
   * @param shelfLabel the display label for the shelf
   * @param bookCapacity the maximum number of books the shelf can hold
   */
  @Override
  public void createShelfInBookcaseByBookcaseId(
      Long bookcaseId, int shelfPosition, String shelfLabel, int bookCapacity) {
    createShelfUseCase.execute(shelfLabel, shelfPosition, bookCapacity, bookcaseId);
  }

  /** {@inheritDoc} */
  @Override
  public List<ShelfResponse> findAll() {
    return queryShelfUseCase.findAll();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isFull(Long shelfId) {
    return queryShelfUseCase.isFull(shelfId);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEmpty(Long shelfId) {
    return queryShelfUseCase.isEmpty(shelfId);
  }
}
