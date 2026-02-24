package com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound;

import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Adapter that implements the Shelf module's {@link BookAccessPort}. Delegates to the Cataloging
 * context's {@link BookFacade} to access book data without depending on Cataloging's domain models.
 *
 * <p>This adapter lives in the Shelf module's infrastructure layer, keeping the dependency arrow
 * pointing from Shelf's infrastructure toward Book's public inbound port. The {@link BookFacade}
 * dependency is lazily resolved to break a circular bean dependency between the Shelf and Book
 * modules at startup.
 */
@Component
public class BookAccessPortAdapter implements BookAccessPort {

  private final BookFacade bookFacade;

  public BookAccessPortAdapter(@Lazy BookFacade bookFacade) {
    this.bookFacade = bookFacade;
  }

  @Override
  public List<Long> getBookIdsByShelfId(Long shelfId) {
    return bookFacade.getBookIdsByShelfId(shelfId);
  }

  @Override
  public void deleteBooksOnShelves(List<Long> shelfIds) {
    bookFacade.deleteByShelfId(shelfIds);
  }
}
