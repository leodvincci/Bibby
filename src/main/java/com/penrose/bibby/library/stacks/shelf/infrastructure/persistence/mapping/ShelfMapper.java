package com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.mapping;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse;
import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.entity.ShelfEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShelfMapper {

  public ShelfMapper() {}

  public ShelfEntity toEntity(Shelf shelf) {
    ShelfEntity entity = new ShelfEntity();
    entity.setBookcaseId(shelf.getBookcaseId());
    entity.setShelfPosition(shelf.getShelfPosition());
    entity.setShelfLabel(shelf.getShelfLabel());
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
        shelf.getBooks());
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

  public ShelfSummaryResponse toSummaryFromEntity(ShelfEntity shelfEntity) {
    return new ShelfSummaryResponse(
        shelfEntity.getShelfId(), shelfEntity.getShelfLabel(), shelfEntity.getBookCapacity());
  }
}
