package com.penrose.bibby.library.stacks.shelf.api.dtos;

import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import java.util.List;

public record ShelfDTO(
    Long shelfId,
    String shelfLabel,
    Long bookcaseId,
    int shelfPosition,
    int bookCapacity,
    String shelfDescription,
    List<Long> bookIds) {
  public static ShelfDTO fromEntity(ShelfEntity shelfEntity) {
    return new ShelfDTO(
        shelfEntity.getShelfId(),
        shelfEntity.getShelfLabel(),
        shelfEntity.getBookcaseId(),
        shelfEntity.getShelfPosition(),
        shelfEntity.getBookCapacity(),
        shelfEntity.getShelfDescription(),
        null);
  }

  public static ShelfDTO fromEntityWithBookId(ShelfEntity shelfEntity, List<Long> bookIds) {
    return new ShelfDTO(
        shelfEntity.getShelfId(),
        shelfEntity.getShelfLabel(),
        shelfEntity.getBookcaseId(),
        shelfEntity.getShelfPosition(),
        shelfEntity.getBookCapacity(),
        shelfEntity.getShelfDescription(),
        bookIds);
  }
}
