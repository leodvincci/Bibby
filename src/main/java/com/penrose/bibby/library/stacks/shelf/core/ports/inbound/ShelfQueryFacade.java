package com.penrose.bibby.library.stacks.shelf.core.ports.inbound;

import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse;
import java.util.List;
import java.util.Optional;

/**
 * Inbound port for read-only shelf queries.
 *
 * <p>Defines the application-level contract for querying shelves. External adapters (CLI commands,
 * REST controllers, etc.) use this port to retrieve shelf data without mutating state.
 *
 * @see ShelfCommandFacade for state-changing shelf operations
 * @see com.penrose.bibby.library.stacks.shelf.core.application.ShelfService
 */
public interface ShelfQueryFacade {

  /**
   * Returns every shelf that belongs to a bookcase, each mapped to a full {@link ShelfResponse}.
   *
   * @param bookcaseId the ID of the parent bookcase
   * @return all shelves in the bookcase; an empty list if none exist
   */
  List<ShelfResponse> findShelvesByBookcaseId(Long bookcaseId);

  /**
   * Looks up a single shelf by its numeric identifier.
   *
   * @param shelfId the numeric shelf identifier
   * @return the matching {@link ShelfResponse} wrapped in an {@link Optional}, or {@link
   *     Optional#empty()} when no shelf exists for the given ID
   */
  Optional<ShelfResponse> findShelfById(Long shelfId);

  /**
   * Produces a lightweight summary projection (ID, label, book count) for every shelf in a
   * bookcase.
   *
   * @param bookcaseId the ID of the parent bookcase
   * @return a list of {@link ShelfSummaryResponse} records; empty if the bookcase has no shelves
   */
  List<ShelfSummaryResponse> getShelfSummariesForBookcaseByBookcaseId(Long bookcaseId);

  /**
   * Retrieves every shelf in the system as full {@link ShelfResponse} port models.
   *
   * @return all persisted shelves; an empty list when none exist
   */
  List<ShelfResponse> findAll();

  /**
   * Determines whether a shelf has reached its maximum book capacity.
   *
   * @param shelfId the numeric shelf identifier
   * @return {@code true} if the shelf's book count equals or exceeds its capacity
   * @throws IllegalStateException if no shelf exists for the given ID
   */
  boolean isFull(Long shelfId);

  /**
   * Determines whether a shelf currently contains zero books.
   *
   * @param shelfId the numeric shelf identifier
   * @return {@code true} if the shelf's book count is zero
   * @throws IllegalStateException if no shelf exists for the given ID
   */
  boolean isEmpty(Long shelfId);
}
