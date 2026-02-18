package com.penrose.bibby.library.stacks.shelf.infrastructure.mapping;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShelfMapper {

  public ShelfMapper() {}

  public Shelf toDomain(ShelfEntity shelfEntity) {
    Shelf shelf =
        new Shelf(
            shelfEntity.getShelfLabel(),
            shelfEntity.getShelfPosition(),
            shelfEntity.getBookCapacity(),
            new ShelfId(shelfEntity.getShelfId()));
    return shelf;
  }

  public ShelfEntity toEntity(Shelf shelf) {

    return null;
  }

  public Shelf toDomainFromDTO(ShelfEntity entity, List<Long> bookIds) {
    Shelf shelf = toDomain(entity);
    shelf.setShelfLabel(entity.getShelfLabel());
    shelf.setBookCapacity(entity.getBookCapacity());
    shelf.setShelfId(new ShelfId(entity.getShelfId()));
    shelf.setShelfPosition(entity.getShelfPosition());
    shelf.setBooks(bookIds);
    return shelf;
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

  public Shelf toDomainFromEntity(ShelfEntity shelfEntity) {
    return new Shelf(
        shelfEntity.getShelfLabel(),
        shelfEntity.getShelfPosition(),
        shelfEntity.getBookCapacity(),
        new ShelfId(shelfEntity.getShelfId()));
  }

  public ShelfSummary toSummaryFromEntity(ShelfEntity shelfEntity) {
    return new ShelfSummary(
        shelfEntity.getShelfId(), shelfEntity.getShelfLabel(), shelfEntity.getBookCapacity());
  }

  public ShelfOptionResponse toShelfOption(Shelf shelf) {
    Long shelfId = shelf.getShelfId().shelfId();
    String shelfLabel = shelf.getShelfLabel();
    int bookCapacity = shelf.getBookCapacity();
    long bookCount = shelf.getBookIds().size();
    boolean hasSpace = bookCount < bookCapacity;
    return new ShelfOptionResponse(shelfId, shelfLabel, bookCapacity, bookCount, hasSpace);
  }

  //    public Shelf toDomain(ShelfEntity shelfEntity) {
  //        Shelf shelf = new Shelf();
  //        shelf.setId(shelfEntity.getShelfId());
  //        shelf.setBookCapacity(shelfEntity.getBookCapacity());
  //        shelf.setLabel(shelfEntity.getShelfLabel());
  //        shelf.setShelfLabel(shelfEntity.getShelfLabel());
  //        shelf.setShelfPosition(shelfEntity.getShelfPosition());
  //        shelf.setShelfDescription(shelfEntity.getShelfDescription());
  //        return shelf;
  //    }
}
