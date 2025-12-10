package com.penrose.bibby.library.book.contracts.ports.outbound;

import com.penrose.bibby.library.shelf.contracts.dtos.ShelfDTO;

import java.util.Optional;


public interface ShelfAccessPort {

    Optional<ShelfDTO> findShelfById(Long shelfId);
}
