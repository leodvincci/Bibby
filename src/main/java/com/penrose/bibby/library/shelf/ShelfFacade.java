package com.penrose.bibby.library.shelf;

import java.util.Optional;

public interface ShelfFacade {

    Optional<ShelfEntity> getShelfEntityById(Long shelfId);
}
