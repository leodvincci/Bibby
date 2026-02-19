package com.penrose.bibby.library.cataloging.book.core.port.outbound;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import java.util.Optional;

public interface ShelfAccessPort {

  Optional<ShelfDTO> findShelfById(Long shelfId);
}
