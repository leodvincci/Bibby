package com.penrose.bibby.library.shelf;

public class ShelfMapper {


    public static Shelf toDomain(ShelfEntity shelfEntity){
        Shelf shelf = new Shelf();
        shelf.setId(shelfEntity.getShelfId());
        shelf.setLabel(shelfEntity.getShelfLabel());
        shelf.setShelfLabel(shelfEntity.getShelfLabel());
        shelf.setShelfPosition(shelfEntity.getShelfPosition());
        shelf.setBookCase(null);

        return shelf;
    }
}
