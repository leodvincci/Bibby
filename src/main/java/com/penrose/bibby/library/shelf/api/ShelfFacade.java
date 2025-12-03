package com.penrose.bibby.library.shelf.api;

import com.penrose.bibby.library.shelf.domain.Shelf;

import java.util.Optional;

public interface ShelfFacade {

    Optional<Shelf> getShelfEntityById(Long shelfId);
}
