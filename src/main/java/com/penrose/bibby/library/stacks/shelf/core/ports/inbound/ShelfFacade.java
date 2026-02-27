package com.penrose.bibby.library.stacks.shelf.core.ports.inbound;

import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse;
import java.util.List;
import java.util.Optional;

/**
 * Inbound port for the Shelf bounded context.
 *
 * <p>Defines the application-level contract that external adapters (CLI commands, REST controllers,
 * etc.) use to query, create, and delete shelves. All responses are expressed as inbound port
 * models ({@link ShelfResponse}, {@link ShelfSummaryResponse}) so that domain internals never leak
 * across the boundary.
 *
 * @see com.penrose.bibby.library.stacks.shelf.core.application.ShelfService
 */
public interface ShelfFacade {

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
   * bookcase. Useful for overview or listing screens where full shelf details are not needed.
   *
   * @param bookcaseId the ID of the parent bookcase
   * @return a list of {@link ShelfSummaryResponse} records; empty if the bookcase has no shelves
   */
  List<ShelfSummaryResponse> getShelfSummariesForBookcaseByBookcaseId(Long bookcaseId);

  /**
   * Deletes every shelf that belongs to the specified bookcase.
   *
   * @param bookcaseId the ID of the bookcase whose shelves should be removed
   */
  void deleteAllShelvesInBookcaseByBookcaseId(Long bookcaseId);

  /**
   * Creates a new shelf within a bookcase and persists it.
   *
   * @param bookcaseId the ID of the bookcase that will contain the new shelf
   * @param shelfPosition the ordinal position of the shelf within the bookcase (must be &ge; 1)
   * @param shelfLabel the display label for the shelf (must not be blank)
   * @param bookCapacity the maximum number of books the shelf can hold (must be &ge; 1)
   */
  void createShelfInBookcaseByBookcaseId(
      Long bookcaseId, int shelfPosition, String shelfLabel, int bookCapacity);

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

  /**
   * Places a book on a shelf by creating a new placement record.
   *
   * @param bookId the ID of the book to place
   * @param shelfId the ID of the shelf to place the book on
   * @throws IllegalArgumentException if the book does not exist
   */
  void placeBookOnShelf(Long bookId, Long shelfId);
}
