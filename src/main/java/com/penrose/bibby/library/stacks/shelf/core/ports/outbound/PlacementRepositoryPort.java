package com.penrose.bibby.library.stacks.shelf.core.ports.outbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Placement;

public interface PlacementRepositoryPort {
  void placeBookOnShelf(Placement placement);
}
