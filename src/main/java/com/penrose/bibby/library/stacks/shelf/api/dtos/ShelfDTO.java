package com.penrose.bibby.library.stacks.shelf.api.dtos;

import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.entity.ShelfEntity;
import java.util.List;

public record ShelfDTO(
    Long shelfId,
    String shelfLabel,
    Long bookcaseId,
    int shelfPosition,
    int bookCapacity,
    List<Long> bookIds) {
  public static ShelfDTO fromEntity(ShelfEntity shelfEntity) {
    return new ShelfDTO(
        shelfEntity.getShelfId(),
        shelfEntity.getShelfLabel(),
        shelfEntity.getBookcaseId(),
        shelfEntity.getShelfPosition(),
        shelfEntity.getBookCapacity(),
        null);
  }

  public static ShelfDTO fromEntityWithBookId(ShelfEntity shelfEntity, List<Long> bookIds) {
    return new ShelfDTO(
        shelfEntity.getShelfId(),
        shelfEntity.getShelfLabel(),
        shelfEntity.getBookcaseId(),
        shelfEntity.getShelfPosition(),
        shelfEntity.getBookCapacity(),
        bookIds);
  }
}
