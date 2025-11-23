package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.bookcase.BookcaseEntity;
import org.springframework.stereotype.Component;

@Component
public class ShelfMapper {


    public Shelf toDomain(ShelfEntity shelfEntity){
        Shelf shelf = new Shelf();
        shelf.setId(shelfEntity.getShelfId());

        shelf.setLabel(shelfEntity.getShelfLabel());
        shelf.setShelfLabel(shelfEntity.getShelfLabel());
        shelf.setShelfPosition(shelfEntity.getShelfPosition());

        return shelf;
    }
}
