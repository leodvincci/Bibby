package com.penrose.bibby.library.catalog.book.contracts.ports.outbound;

import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;

import java.util.Optional;


public interface ShelfAccessPort {

    Optional<ShelfDTO> findShelfById(Long shelfId);
}
