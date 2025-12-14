package com.penrose.bibby.library.placement.bookcase.contracts.dtos;

import com.penrose.bibby.library.placement.bookcase.infrastructure.BookcaseEntity;

import java.util.Optional;

public record BookcaseDTO(Long bookcaseId, String bookcaseLabel, int shelfCapacity, int bookCapacity) {

    public static Optional<BookcaseDTO> fromEntity(Optional<BookcaseEntity> bookcaseEntity) {

        return bookcaseEntity.map(entity -> new BookcaseDTO(
                entity.getBookcaseId(),
                entity.getBookcaseLabel(),
                entity.getShelfCapacity(),
                entity.getBookCapacity()
        ));
    }
}
