package com.penrose.bibby.library.shelf.contracts;

import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;

import java.util.List;

public record ShelfDTO(Long shelfId,String shelfLabel, Long bookcaseId, int shelfPosition, int bookCapacity, String shelfDescription,  List<Long> bookIds) {

    public static ShelfDTO fromEntity(ShelfEntity shelfDTO) {
        return new ShelfDTO(
                shelfDTO.getShelfId(),
                shelfDTO.getShelfLabel(),
                shelfDTO.getBookcaseId(),
                shelfDTO.getShelfPosition(),
                shelfDTO.getBookCapacity(),
                shelfDTO.getShelfDescription(),
                null
        );
    }
}
