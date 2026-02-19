package com.penrose.bibby.library.cataloging.book.api.adapters;

import com.penrose.bibby.library.cataloging.book.core.port.outbound.ShelfAccessPort;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ShelfAccessPortAdapter implements ShelfAccessPort {

  private final ShelfFacade shelfFacade;
  ShelfService shelfService;

  public ShelfAccessPortAdapter(ShelfFacade shelfFacade) {
    this.shelfFacade = shelfFacade;
  }

  @Override
  public Optional<ShelfDTO> findShelfById(Long shelfId) {
    return shelfFacade.findShelfById(shelfId);
  }
}
