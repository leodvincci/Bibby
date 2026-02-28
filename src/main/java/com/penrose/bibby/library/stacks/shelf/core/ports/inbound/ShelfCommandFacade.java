package com.penrose.bibby.library.stacks.shelf.core.ports.inbound;

/**
 * Inbound port for state-changing shelf operations.
 *
 * <p>Defines the application-level contract for creating, deleting, and placing books on shelves.
 * External adapters (CLI commands, REST controllers, etc.) use this port to mutate shelf state.
 *
 * @see ShelfQueryFacade for read-only shelf operations
 * @see com.penrose.bibby.library.stacks.shelf.core.application.ShelfService
 */
public interface ShelfCommandFacade {

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
   * Places a book on a shelf by creating a new placement record.
   *
   * @param bookId the ID of the book to place
   * @param shelfId the ID of the shelf to place the book on
   * @throws IllegalArgumentException if the book does not exist
   */
  void placeBookOnShelf(Long bookId, Long shelfId);
}
