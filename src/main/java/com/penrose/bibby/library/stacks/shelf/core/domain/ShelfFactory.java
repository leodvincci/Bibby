package com.penrose.bibby.library.stacks.shelf.core.domain;

import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
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
