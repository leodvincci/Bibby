package com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShelfPortModelMapperTest {

  @Test
  void toShelfResponse_shouldMapAllFieldsCorrectly() {
    ShelfId shelfId = new ShelfId(7L);
    List<Long> books = List.of(10L, 20L, 30L);
    Shelf shelf = new Shelf("Fiction", 2, 15, shelfId, books, 42L);

    ShelfResponse response = ShelfPortModelMapper.toShelfResponse(shelf);

    assertThat(response.id()).isEqualTo(7L);
    assertThat(response.shelfPosition()).isEqualTo(2);
    assertThat(response.shelfLabel()).isEqualTo("Fiction");
    assertThat(response.bookCapacity()).isEqualTo(15);
    assertThat(response.bookIds()).containsExactly(10L, 20L, 30L);
    assertThat(response.bookcaseId()).isEqualTo(42L);
  }

  @Test
  void toShelfResponse_shouldMapEmptyBookList() {
    Shelf shelf = new Shelf("Empty Shelf", 1, 10, new ShelfId(1L), List.of(), 100L);

    ShelfResponse response = ShelfPortModelMapper.toShelfResponse(shelf);

    assertThat(response.bookIds()).isEmpty();
    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.shelfLabel()).isEqualTo("Empty Shelf");
  }
}
