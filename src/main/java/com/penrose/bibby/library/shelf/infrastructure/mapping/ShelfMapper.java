package com.penrose.bibby.library.shelf.infrastructure.mapping;

import com.penrose.bibby.library.shelf.contracts.ShelfDTO;
import com.penrose.bibby.library.shelf.domain.Shelf;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShelfMapper {


    public Shelf toDomain(ShelfEntity shelfEntity){
        Shelf shelf = new Shelf();
        shelf.setId(shelfEntity.getShelfId());
        shelf.setBookCapacity(shelfEntity.getBookCapacity());
        shelf.setLabel(shelfEntity.getShelfLabel());
        shelf.setShelfPosition(shelfEntity.getShelfPosition());
        shelf.setShelfDescription(shelfEntity.getShelfDescription());

        return shelf;
    }

    public ShelfEntity toEntity(Shelf shelf){

        return null;
    }

    public ShelfEntity updateEntity(ShelfEntity shelf) {

        return shelf;
    }

//    public Shelf toDomain(ShelfEntity shelfEntity) {
//        Shelf shelf = new Shelf();
//        shelf.setId(shelfEntity.getShelfId());
//        shelf.setBookCapacity(shelfEntity.getBookCapacity());
//        shelf.setLabel(shelfEntity.getShelfLabel());
//        shelf.setShelfLabel(shelfEntity.getShelfLabel());
//        shelf.setShelfPosition(shelfEntity.getShelfPosition());
//        shelf.setShelfDescription(shelfEntity.getShelfDescription());
//        return shelf;
//    }
}
