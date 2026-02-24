package com.penrose.bibby.library.stacks.bookcase.infrastructure.adapter.outbound;

import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.ShelfAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import org.springframework.stereotype.Component;

@Component("bookcaseShelfAccessPortAdapter")
public class ShelfAccessPortAdapter implements ShelfAccessPort {
  private final ShelfFacade shelfFacade;

  public ShelfAccessPortAdapter(ShelfFacade shelfFacade) {
    this.shelfFacade = shelfFacade;
  }

  @Override
  public void deleteAllShelvesInBookcase(Long bookcaseId) {
    shelfFacade.deleteAllShelvesInBookcase(bookcaseId);
  }

  @Override
  public void createShelf(Long bookcaseId, int position, String string, int bookCapacity) {
    shelfFacade.createShelf(bookcaseId, position, string, bookCapacity);
  }
}
