package com.penrose.bibby.library.stacks.shelf.core.domain;

import com.penrose.bibby.library.cataloging.book.contracts.dtos.BriefBibliographicRecord;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;

import java.util.List;

public interface ShelfDomainRepository {



    Shelf getById(ShelfId id);

    void save(Shelf shelf);



    // optional: queries that return Shelf or domain read models

}
