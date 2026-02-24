package com.penrose.bibby.library.stacks.shelf.core.ports.inbound.mapper;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;

public class ShelfPortModelMapper {

  public static ShelfResponse toShelfResponse(Shelf shelf) {
    return new ShelfResponse(
        shelf.getId(),
        shelf.getShelfPosition(),
        shelf.getShelfLabel(),
        shelf.getBookCapacity(),
        shelf.getBookIds());
  }
}
