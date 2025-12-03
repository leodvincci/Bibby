package com.penrose.bibby.library.shelf.api;

import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;

import java.util.Optional;

public interface ShelfFacade {

    Optional<ShelfEntity> getShelfEntityById(Long shelfId);
}
