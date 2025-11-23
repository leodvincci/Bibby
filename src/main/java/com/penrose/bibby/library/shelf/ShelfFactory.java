package com.penrose.bibby.library.shelf;

import org.springframework.stereotype.Component;

@Component
public class ShelfFactory {

    public ShelfEntity createEntity(Long bookCaseId, int shelfPosition, String shelfLabel, int bookCapacity){
        ShelfEntity shelfEntity = new ShelfEntity();
        shelfEntity.setBookcaseId(bookCaseId);
        shelfEntity.setShelfLabel(shelfLabel);
        shelfEntity.setShelfPosition(shelfPosition);
        shelfEntity.setBookCapacity(bookCapacity);
        return shelfEntity;
    }
}
