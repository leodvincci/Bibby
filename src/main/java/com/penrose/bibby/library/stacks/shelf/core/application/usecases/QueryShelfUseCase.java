package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.mapper.ShelfPortModelMapper;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use case for the Shelf aggregate.
 *
 * <p>Handles all query operations against shelves — lookups by ID, filtered listings by bookcase,
 * lightweight summary projections, and capacity checks. Domain {@link Shelf} entities retrieved
 * from the {@link ShelfDomainRepositoryPort} are mapped to inbound port models ({@link
 * ShelfResponse}, {@link ShelfSummaryResponse}) before crossing the application boundary, keeping
 * domain internals encapsulated.
 */
@Service
public class QueryShelfUseCase {

  private final ShelfDomainRepositoryPort shelfDomainRepositoryPort;

  public QueryShelfUseCase(ShelfDomainRepositoryPort shelfDomainRepositoryPort) {
    this.shelfDomainRepositoryPort = shelfDomainRepositoryPort;
  }

  /**
   * Lists every shelf that belongs to a bookcase, mapped to full {@link ShelfResponse} port models.
   *
   * @param bookcaseId the ID of the parent bookcase
   * @return all shelves in the bookcase; an empty list if the bookcase has none
   */
  public List<ShelfResponse> findShelvesByBookcaseId(Long bookcaseId) {

    return shelfDomainRepositoryPort.findByBookcaseId(bookcaseId).stream()
        .map(ShelfPortModelMapper::toShelfResponse)
        .toList();
  }

  /**
   * Looks up a single shelf by its numeric ID.
   *
   * <p>Runs inside a transaction so that any lazily-loaded associations on the {@link Shelf}
   * aggregate (e.g. book IDs) are available during mapping.
   *
   * @param shelfId the numeric shelf identifier
   * @return the matching {@link ShelfResponse} wrapped in an {@link Optional}, or {@link
   *     Optional#empty()} when no shelf exists for the given ID
   */
  @Transactional
  public Optional<ShelfResponse> findShelfById(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId));
    if (shelf == null) {
      return Optional.empty();
    }
    return Optional.of(ShelfPortModelMapper.toShelfResponse(shelf));
  }

  /**
   * Produces a lightweight summary projection for every shelf in a bookcase.
   *
   * <p>Unlike {@link #findShelvesByBookcaseId(Long)}, this method returns only the shelf ID, label,
   * and current book count — useful for overview/listing screens where full shelf details are not
   * needed.
   *
   * @param bookcaseId the ID of the parent bookcase
   * @return a list of {@link ShelfSummaryResponse} records; empty if the bookcase has no shelves
   */
  public List<ShelfSummaryResponse> getShelfSummariesForBookcaseById(Long bookcaseId) {
    return shelfDomainRepositoryPort.findByBookcaseId(bookcaseId).stream()
        .map(
            shelf ->
                new ShelfSummaryResponse(
                    shelf.getShelfId().shelfId(), shelf.getShelfLabel(), shelf.getBookCount()))
        .toList();
  }

  /**
   * Retrieves every shelf in the system as full {@link ShelfResponse} port models.
   *
   * @return all persisted shelves; an empty list when none exist
   */
  public List<ShelfResponse> findAll() {
    return shelfDomainRepositoryPort.findAll().stream()
        .map(ShelfPortModelMapper::toShelfResponse)
        .toList();
  }

  /**
   * Determines whether a shelf has reached its {@link Shelf#getBookCapacity() book capacity}.
   *
   * <p>Delegates to {@link Shelf#isFull()}, which compares the current book count against the
   * configured capacity.
   *
   * @param shelfId the numeric shelf identifier
   * @return {@code true} if the shelf's book count equals or exceeds its capacity
   * @throws IllegalStateException if no shelf exists for the given ID
   */
  public boolean isFull(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId));
    if (shelf == null) {
      throw new IllegalStateException("Shelf not found with id: " + shelfId);
    }
    return shelf.isFull();
  }

  /**
   * Determines whether a shelf currently contains zero books.
   *
   * @param shelfId the numeric shelf identifier
   * @return {@code true} if the shelf's {@link Shelf#getBookCount() book count} is zero
   * @throws IllegalStateException if no shelf exists for the given ID
   */
  public boolean isEmpty(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId));
    if (shelf == null) {
      throw new IllegalStateException("Shelf not found with id: " + shelfId);
    }
    return shelf.getBookCount() == 0;
  }
}
