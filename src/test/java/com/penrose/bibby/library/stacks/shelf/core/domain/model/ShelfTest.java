package com.penrose.bibby.library.stacks.shelf.core.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShelfTest {

  private Shelf createValidShelf() {
    return new Shelf("Shelf A", 1, 10, new ShelfId(1L), List.of(), 100L);
  }

  @Test
  void setShelfLabel_shouldThrowWhenNull() {
    Shelf shelf = createValidShelf();

    assertThatThrownBy(() -> shelf.setShelfLabel(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");
  }

  @Test
  void setShelfLabel_shouldThrowWhenEmpty() {
    Shelf shelf = createValidShelf();

    assertThatThrownBy(() -> shelf.setShelfLabel(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");
  }

  @Test
  void setShelfLabel_shouldThrowWhenBlank() {
    Shelf shelf = createValidShelf();

    assertThatThrownBy(() -> shelf.setShelfLabel("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");
  }
}
