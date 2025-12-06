package com.penrose.bibby.library.shelf.infrastructure.mapping;

import com.penrose.bibby.library.shelf.domain.Shelf;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShelfMapper {


    public Shelf toDomain(ShelfEntity shelfDTO, List<Long> bookIds){
        Shelf shelf = new Shelf();
        shelf.setId(shelfDTO.getShelfId());
        shelf.setBookCapacity(shelfDTO.getBookCapacity());
        shelf.setLabel(shelfDTO.getShelfLabel());
        shelf.setShelfLabel(shelfDTO.getShelfLabel());
        shelf.setShelfPosition(shelfDTO.getShelfPosition());
        shelf.setShelfDescription(shelfDTO.getShelfDescription());
        shelf.setBooks(bookIds);
        return shelf;
    }

    public ShelfEntity toEntity(Shelf shelf){

        return null;
    }

    public ShelfEntity updateEntity(ShelfEntity shelf) {

        return shelf;
    }
}
