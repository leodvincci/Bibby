package com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.mapping;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Placement;
import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.entity.PlacementEntity;

public class PlacementMapper {

  public static PlacementEntity toEntity(Placement placement) {
    return new PlacementEntity(placement.getBookId(), placement.getShelfId());
  }
}
