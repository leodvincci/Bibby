package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.mapper.ShelfPortModelMapper;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryShelfUseCase {

  private final ShelfDomainRepositoryPort shelfDomainRepositoryPort;

  public QueryShelfUseCase(ShelfDomainRepositoryPort shelfDomainRepositoryPort) {
    this.shelfDomainRepositoryPort = shelfDomainRepositoryPort;
  }

  /**
   * Retrieves all shelves belonging to a given bookcase.
   *
   * @param bookcaseId the ID of the bookcase to query
   * @return a list of shelves within the specified bookcase
   */
  public List<ShelfResponse> findShelvesByBookcaseId(Long bookcaseId) {

    return shelfDomainRepositoryPort.findByBookcaseId(bookcaseId).stream()
        .map(ShelfPortModelMapper::toShelfResponse)
        .toList();
  }

  /**
   * Finds a shelf by its unique identifier within a transactional context.
   *
   * @param shelfId the ID of the shelf to look up
   * @return an {@link Optional} containing the shelf if found, or empty otherwise
   */
  @Transactional
  public Optional<Shelf> findShelfById(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId));
    if (shelf == null) {
      return Optional.empty();
    }
    return Optional.of(shelf);
  }

  /**
   * Builds lightweight summaries for all shelves in a bookcase, including each shelf's ID, label,
   * and current book count.
   *
   * @param bookcaseId the ID of the bookcase whose shelf summaries are requested
   * @return a list of {@link ShelfSummary} objects for the bookcase
   */
  public List<ShelfSummary> getShelfSummariesForBookcaseById(Long bookcaseId) {
    return shelfDomainRepositoryPort.findByBookcaseId(bookcaseId).stream()
        .map(
            shelf ->
                new ShelfSummary(
                    shelf.getShelfId().shelfId(), shelf.getShelfLabel(), shelf.getBookCount()))
        .toList();
  }

  /**
   * Retrieves every shelf in the system.
   *
   * @return a list of all shelves
   */
  public List<Shelf> findAll() {
    return shelfDomainRepositoryPort.findAll();
  }

  /**
   * Checks whether a shelf has reached its maximum book capacity.
   *
   * @param shelfId the ID of the shelf to check
   * @return {@code true} if the shelf is at capacity, {@code false} otherwise
   * @throws IllegalStateException if no shelf exists with the given ID
   */
  public boolean isFull(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId));
    if (shelf == null) {
      throw new IllegalStateException("Shelf not found with id: " + shelfId);
    }
    return shelf.isFull();
  }

  /**
   * Checks whether a shelf currently holds no books.
   *
   * @param shelfId the ID of the shelf to check
   * @return {@code true} if the shelf has zero books, {@code false} otherwise
   * @throws IllegalStateException if no shelf exists with the given ID
   */
  public boolean isEmpty(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId));
    if (shelf == null) {
      throw new IllegalStateException("Shelf not found with id: " + shelfId);
    }
    return shelf.getBookCount() == 0;
  }
}
