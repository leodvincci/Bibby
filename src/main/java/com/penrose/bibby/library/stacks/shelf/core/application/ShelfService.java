package com.penrose.bibby.library.stacks.shelf.core.application;

import com.penrose.bibby.library.stacks.shelf.core.application.usecases.CreateShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.DeleteShelvesUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.QueryShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Service implementation for managing shelf-related operations within the library stacks domain.
 *
 * <p>This service acts as the primary implementation of the {@link ShelfFacade} inbound port,
 * orchestrating shelf operations by delegating to specialized use case classes. It follows
 * hexagonal architecture principles by keeping business logic encapsulated in use cases while
 * providing a clean interface to external adapters (CLI, REST, etc.).
 *
 * <p>The service maintains transactional boundaries and ensures that all shelf operations
 * are executed through well-defined use cases, promoting separation of concerns and testability.
 *
 * @see ShelfFacade for the contract this service fulfills
 * @see QueryShelfUseCase for shelf query operations
 * @see CreateShelfUseCase for shelf creation logic
 * @see DeleteShelvesUseCase for shelf deletion operations
 */
@Service
public class ShelfService implements ShelfFacade {

  private final QueryShelfUseCase queryShelfUseCase;
  private final CreateShelfUseCase createShelfUseCase;
  private final DeleteShelvesUseCase deleteShelvesUseCase;

  /**
   * Constructs a new ShelfService with the required use case dependencies.
   *
   * @param queryShelfUseCase handles all shelf query operations including lookups and capacity checks
   * @param createShelfUseCase manages shelf creation and initialization
   * @param deleteShelvesUseCase handles bulk shelf deletion operations
   */
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
  public Optional<ShelfResponse> findShelfById(Long shelfId) {
    return queryShelfUseCase.findShelfById(shelfId);
  }

  /** {@inheritDoc} */
  @Override
  public List<ShelfSummaryResponse> getShelfSummariesForBookcaseByBookcaseId(Long bookcaseId) {
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

  /** {@inheritDoc} */
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
