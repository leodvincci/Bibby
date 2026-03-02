package com.penrose.bibby.library.stacks.bookcase.infrastructure.adapter.outbound;

import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.ShelfAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.DeleteShelvesUseCasePort;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfCommandFacade;
import org.springframework.stereotype.Component;

@Component("bookcaseShelfAccessPortAdapter")
public class ShelfAccessPortAdapter implements ShelfAccessPort {
  private final ShelfCommandFacade shelfCommandFacade;
  private final DeleteShelvesUseCasePort deleteShelvesUseCasePort;

  public ShelfAccessPortAdapter(
      ShelfCommandFacade shelfCommandFacade, DeleteShelvesUseCasePort deleteShelvesUseCasePort) {
    this.shelfCommandFacade = shelfCommandFacade;
    this.deleteShelvesUseCasePort = deleteShelvesUseCasePort;
  }

  @Override
  public void deleteAllShelvesInBookcase(Long bookcaseId) {
    deleteShelvesUseCasePort.execute(bookcaseId);
  }

  @Override
  public void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    shelfCommandFacade.createShelfInBookcaseByBookcaseId(
        bookcaseId, position, shelfLabel, bookCapacity);
  }
}
