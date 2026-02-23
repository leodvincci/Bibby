package com.penrose.bibby.library.cataloging.book.infrastructure.adapter.outbound;

import com.penrose.bibby.library.cataloging.book.core.port.outbound.ShelfAccessPort;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ShelfAccessPortAdapter implements ShelfAccessPort {

  private final ShelfFacade shelfFacade;

  public ShelfAccessPortAdapter(ShelfFacade shelfFacade) {
    this.shelfFacade = shelfFacade;
  }

  @Override
  public Optional<ShelfDTO> findShelfById(Long shelfId) {
    return shelfFacade
        .findShelfById(shelfId)
        .map(
            shelf ->
                new ShelfDTO(
                    shelf.getId(),
                    shelf.getShelfLabel(),
                    shelf.getBookcaseId(),
                    shelf.getShelfPosition(),
                    shelf.getBookCapacity(),
                    shelf.getBookIds()));
  }

  @Override
  public boolean isFull(Long aLong) {
    return shelfFacade.isFull(aLong);
  }
}
