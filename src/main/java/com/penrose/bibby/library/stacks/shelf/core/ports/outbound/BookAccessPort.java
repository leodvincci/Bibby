package com.penrose.bibby.library.stacks.shelf.core.ports.outbound;

import java.util.List;

/**
 * Outbound port for accessing book information from the Cataloging context. This interface is owned
 * by the Shelf module and defines what it needs from books in its own terms, following the
 * Dependency Inversion Principle.
 *
 * <p>The Book module will provide an adapter that implements this interface.
 */
public interface BookAccessPort {

  /**
   * Retrieves the IDs of all books currently assigned to a specific shelf.
   *
   * @param shelfId The ID of the shelf
   * @return List of book IDs on the shelf
   */
  List<Long> getBookIdsByShelfId(Long shelfId);

  void deleteBooksOnShelves(List<Long> shelfIds);
}
