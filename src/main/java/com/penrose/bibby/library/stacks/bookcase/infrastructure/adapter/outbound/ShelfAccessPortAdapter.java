package com.penrose.bibby.library.stacks.bookcase.infrastructure.adapter.outbound;

import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.ShelfAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfCommandFacade;
import org.springframework.stereotype.Component;

@Component("bookcaseShelfAccessPortAdapter")
public class ShelfAccessPortAdapter implements ShelfAccessPort {
  private final ShelfCommandFacade shelfFacade;

  public ShelfAccessPortAdapter(ShelfCommandFacade shelfFacade) {
    this.shelfFacade = shelfFacade;
  }

  @Override
  public void deleteAllShelvesInBookcase(Long bookcaseId) {
    shelfFacade.deleteAllShelvesInBookcaseByBookcaseId(bookcaseId);
  }

  @Override
  public void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    shelfFacade.createShelfInBookcaseByBookcaseId(bookcaseId, position, shelfLabel, bookCapacity);
  }
}
