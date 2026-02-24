package com.penrose.bibby.web.controllers.stacks.shelf.mappers;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import org.springframework.stereotype.Component;

@Component
public class ShelfResponseMapper {

  public ShelfOptionResponse toShelfOption(Shelf shelf) {
    Long shelfId = shelf.getShelfId().shelfId();
    String shelfLabel = shelf.getShelfLabel();
    int bookCapacity = shelf.getBookCapacity();
    long bookCount = shelf.getBookIds().size();
    boolean hasSpace = bookCount < bookCapacity;
    return new ShelfOptionResponse(shelfId, shelfLabel, bookCapacity, bookCount, hasSpace);
  }

  public ShelfOptionResponse toShelfOption(ShelfResponse shelf) {
    Long shelfId = shelf.id();
    String shelfLabel = shelf.shelfLabel();
    int bookCapacity = shelf.bookCapacity();
    long bookCount = shelf.bookIds().size();
    boolean hasSpace = bookCount < bookCapacity;
    return new ShelfOptionResponse(shelfId, shelfLabel, bookCapacity, bookCount, hasSpace);
  }
}
