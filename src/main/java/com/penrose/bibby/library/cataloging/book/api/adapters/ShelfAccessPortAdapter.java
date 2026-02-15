package com.penrose.bibby.library.cataloging.book.api.adapters;

import com.penrose.bibby.library.cataloging.book.api.ports.outbound.ShelfAccessPort;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.api.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
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
