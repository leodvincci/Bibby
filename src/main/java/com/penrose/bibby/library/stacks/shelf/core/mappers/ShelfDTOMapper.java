package com.penrose.bibby.library.stacks.shelf.core.mappers;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShelfDTOMapper {

  public Shelf toDomainFromDTO(Shelf shelf, List<Long> bookIds) {
    return new Shelf(
        shelf.getShelfLabel(),
        shelf.getShelfPosition(),
        shelf.getBookCapacity(),
        new ShelfId(shelf.getId()),
        bookIds);
  }

  public ShelfDTO toDTOFromDomain(Shelf shelf, Long bookcaseId) {
    return new ShelfDTO(
        shelf.getId(),
        shelf.getShelfLabel(),
        bookcaseId,
        shelf.getShelfPosition(),
        shelf.getBookCapacity(),
        shelf.getBookIds());
  }

  public ShelfOptionResponse toShelfOption(Shelf shelf) {
    Long shelfId = shelf.getShelfId().shelfId();
    String shelfLabel = shelf.getShelfLabel();
    int bookCapacity = shelf.getBookCapacity();
    long bookCount = shelf.getBookIds().size();
    boolean hasSpace = bookCount < bookCapacity;
    return new ShelfOptionResponse(shelfId, shelfLabel, bookCapacity, bookCount, hasSpace);
  }
}
