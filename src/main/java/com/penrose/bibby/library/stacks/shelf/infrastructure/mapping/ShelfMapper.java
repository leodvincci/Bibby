package com.penrose.bibby.library.stacks.shelf.infrastructure.mapping;

import com.penrose.bibby.library.stacks.shelf.core.domain.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShelfMapper {

  public ShelfMapper() {}

  public Shelf toDomain(ShelfEntity shelfEntity) {
    Shelf shelf = new Shelf();
    shelf.setShelfId(new ShelfId(shelfEntity.getShelfId()));
    shelf.setBookCapacity(shelfEntity.getBookCapacity());
    shelf.setLabel(shelfEntity.getShelfLabel());
    shelf.setShelfPosition(shelfEntity.getShelfPosition());
    shelf.setShelfDescription(shelfEntity.getShelfDescription());
    return shelf;
  }

  public ShelfEntity toEntity(Shelf shelf) {

    return null;
  }

  public ShelfEntity updateEntity(ShelfEntity shelf) {

    return shelf;
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
