package com.penrose.bibby.library.stacks.shelf.core.domain;

public interface ShelfDomainRepository {

    Shelf getById(Long id);

    void save(Shelf shelf);


    // optional: queries that return Shelf or domain read models

}
