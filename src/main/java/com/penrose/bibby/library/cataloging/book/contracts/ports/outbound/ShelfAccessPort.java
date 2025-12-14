package com.penrose.bibby.library.cataloging.book.contracts.ports.outbound;

import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;

import java.util.Optional;


public interface ShelfAccessPort {

    Optional<ShelfDTO> findShelfById(Long shelfId);
}
