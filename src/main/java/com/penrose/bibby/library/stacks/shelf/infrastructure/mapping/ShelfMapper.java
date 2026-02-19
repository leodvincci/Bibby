package com.penrose.bibby.library.stacks.shelf.infrastructure.mapping;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShelfMapper {

  public ShelfMapper() {}

  public ShelfEntity toEntity(Shelf shelf) {
    ShelfEntity entity = new ShelfEntity();
    entity.setShelfId(shelf.getShelfId().shelfId());
    entity.setShelfLabel(shelf.getShelfLabel());
    entity.setShelfPosition(shelf.getShelfPosition());
    entity.setBookCapacity(shelf.getBookCapacity());
    return entity;
  }

  public ShelfDTO toDTO(Shelf shelf, Long bookcaseId) {
    return new ShelfDTO(
        shelf.getId(),
        shelf.getShelfLabel(),
        bookcaseId,
        shelf.getShelfPosition(),
        shelf.getBookCapacity(),
        shelf.getBookIds());
  }

  public ShelfDTO toDTOFromEntity(ShelfEntity shelfEntity, List<Long> bookIds) {
    return new ShelfDTO(
        shelfEntity.getShelfId(),
        shelfEntity.getShelfLabel(),
        shelfEntity.getBookcaseId(),
        shelfEntity.getShelfPosition(),
        shelfEntity.getBookCapacity(),
        bookIds);
  }

  public Shelf toDomainFromEntity(ShelfEntity shelfEntity, List<Long> bookIds) {
    return new Shelf(
        shelfEntity.getShelfLabel(),
        shelfEntity.getShelfPosition(),
        shelfEntity.getBookCapacity(),
        new ShelfId(shelfEntity.getShelfId()),
        bookIds,
        shelfEntity.getBookcaseId());
  }

  public ShelfSummary toSummaryFromEntity(ShelfEntity shelfEntity) {
    return new ShelfSummary(
        shelfEntity.getShelfId(), shelfEntity.getShelfLabel(), shelfEntity.getBookCapacity());
  }
}
