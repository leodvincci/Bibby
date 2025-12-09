package com.penrose.bibby.library.shelf.contracts.dtos;

import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;

import java.util.List;

public record ShelfDTO(Long shelfId,String shelfLabel, Long bookcaseId, int shelfPosition, int bookCapacity, String shelfDescription,  List<Long> bookIds) {

    public static ShelfDTO fromEntity(ShelfEntity shelfEntity) {
        return new ShelfDTO(
                shelfEntity.getShelfId(),
                shelfEntity.getShelfLabel(),
                shelfEntity.getBookcaseId(),
                shelfEntity.getShelfPosition(),
                shelfEntity.getBookCapacity(),
                shelfEntity.getShelfDescription(),
                null
        );
    }
}
