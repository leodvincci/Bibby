package com.penrose.bibby.library.stacks.bookcase.api.dtos;

import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import java.util.Optional;

public record BookcaseDTO(
    Long bookcaseId, int shelfCapacity, int bookCapacity, String location) {

  public static Optional<BookcaseDTO> fromEntity(Optional<BookcaseEntity> bookcaseEntity) {

    return bookcaseEntity.map(
        entity ->
            new BookcaseDTO(
                entity.getBookcaseId(),
                entity.getShelfCapacity(),
                entity.getBookCapacity(),
                entity.getBookcaseLocation()));
  }
}
