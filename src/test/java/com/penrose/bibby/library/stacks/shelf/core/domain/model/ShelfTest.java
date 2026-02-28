package com.penrose.bibby.library.stacks.shelf.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShelfTest {

  private Shelf createValidShelf() {
    return new Shelf("Shelf A", 1, 10, new ShelfId(1L), List.of(), 100L);
  }

  @Test
  void constructor_shouldStoreAllFields() {
    ShelfId id = new ShelfId(42L);
    List<Long> books = List.of(1L, 2L, 3L);
    Shelf shelf = new Shelf("Top Shelf", 3, 20, id, books, 99L);

    assertThat(shelf.getShelfLabel()).isEqualTo("Top Shelf");
    assertThat(shelf.getShelfPosition()).isEqualTo(3);
    assertThat(shelf.getBookCapacity()).isEqualTo(20);
    assertThat(shelf.getShelfId()).isEqualTo(id);
    assertThat(shelf.getBooks()).isEqualTo(books);
    assertThat(shelf.getBookcaseId()).isEqualTo(99L);
  }

  @Test
  void constructor_shouldAcceptNullShelfId() {
    Shelf shelf = new Shelf("Shelf A", 1, 10, null, List.of(), 100L);

    assertThat(shelf.getShelfId()).isNull();
  }

  @Test
  void constructor_shouldThrowWhenLabelIsNull() {
    assertThatThrownBy(() -> new Shelf(null, 1, 10, new ShelfId(1L), List.of(), 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");
  }

  @Test
  void constructor_shouldThrowWhenLabelIsBlank() {
    assertThatThrownBy(() -> new Shelf("   ", 1, 10, new ShelfId(1L), List.of(), 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");
  }

  @Test
  void setShelfLabel_shouldAcceptValidLabel() {
    Shelf shelf = createValidShelf();

    shelf.setShelfLabel("New Label");

    assertThat(shelf.getShelfLabel()).isEqualTo("New Label");
  }

  @Test
  void setShelfPosition_shouldAcceptValidPosition() {
    Shelf shelf = createValidShelf();

    shelf.setShelfPosition(5);

    assertThat(shelf.getShelfPosition()).isEqualTo(5);
  }

  @Test
  void setBookCapacity_shouldAcceptValidCapacity() {
    Shelf shelf = createValidShelf();

    shelf.setBookCapacity(25);

    assertThat(shelf.getBookCapacity()).isEqualTo(25);
  }

  @Test
  void setBookcaseId_shouldAcceptValidId() {
    Shelf shelf = createValidShelf();

    shelf.setBookcaseId(200L);

    assertThat(shelf.getBookcaseId()).isEqualTo(200L);
  }

  @Test
  void setBooks_shouldAcceptValidList() {
    Shelf shelf = createValidShelf();

    shelf.setBooks(List.of(10L, 20L));

    assertThat(shelf.getBooks()).containsExactly(10L, 20L);
  }

  @Test
  void isFull_shouldReturnTrueWhenAtCapacity() {
    Shelf shelf = new Shelf("Shelf A", 1, 2, new ShelfId(1L), List.of(1L, 2L), 100L);

    assertThat(shelf.isFull()).isTrue();
  }

  @Test
  void isFull_shouldReturnFalseWhenBelowCapacity() {
    Shelf shelf = new Shelf("Shelf A", 1, 10, new ShelfId(1L), List.of(1L), 100L);

    assertThat(shelf.isFull()).isFalse();
  }

  @Test
  void getBookCount_shouldReturnNumberOfBooks() {
    Shelf shelf = new Shelf("Shelf A", 1, 10, new ShelfId(1L), List.of(1L, 2L, 3L), 100L);

    assertThat(shelf.getBookCount()).isEqualTo(3);
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

  @Test
  void setShelfPosition_shouldThrowWhenZero() {
    Shelf shelf = createValidShelf();

    assertThatThrownBy(() -> shelf.setShelfPosition(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf position must be greater than 0");
  }

  @Test
  void setShelfPosition_shouldThrowWhenNegative() {
    Shelf shelf = createValidShelf();

    assertThatThrownBy(() -> shelf.setShelfPosition(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf position must be greater than 0");
  }

  @Test
  void setBookCapacity_shouldThrowWhenZero() {
    Shelf shelf = createValidShelf();

    assertThatThrownBy(() -> shelf.setBookCapacity(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Book capacity cannot be negative");
  }

  @Test
  void setBookCapacity_shouldThrowWhenNegative() {
    Shelf shelf = createValidShelf();

    assertThatThrownBy(() -> shelf.setBookCapacity(-5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Book capacity cannot be negative");
  }

  @Test
  void setBookcaseId_shouldThrowWhenNull() {
    Shelf shelf = createValidShelf();

    assertThatThrownBy(() -> shelf.setBookcaseId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Bookcase ID cannot be null");
  }

  @Test
  void setBooks_shouldThrowWhenNull() {
    Shelf shelf = createValidShelf();

    assertThatThrownBy(() -> shelf.setBooks(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Books cannot be null");
  }
}
