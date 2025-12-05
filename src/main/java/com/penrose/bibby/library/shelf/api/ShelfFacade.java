package com.penrose.bibby.library.shelf.api;

import com.penrose.bibby.library.shelf.domain.Shelf;

import java.util.List;
import java.util.Optional;

public interface ShelfFacade {

    List<ShelfDTO> getAllDTOShelves(Long bookcaseId);
}


