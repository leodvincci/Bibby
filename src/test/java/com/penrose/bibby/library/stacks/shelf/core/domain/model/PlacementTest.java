package com.penrose.bibby.library.stacks.shelf.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlacementTest {

  @Test
  void constructor_shouldStoreBookIdAndShelfId() {
    Placement placement = new Placement(1L, 2L);

    assertThat(placement.getBookId()).isEqualTo(1L);
    assertThat(placement.getShelfId()).isEqualTo(2L);
  }

  @Test
  void getBookId_shouldThrowWhenNull() {
    Placement placement = new Placement(null, 2L);

    assertThatThrownBy(placement::getBookId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Book ID cannot be null");
  }

  @Test
  void setBookId_shouldUpdateBookId() {
    Placement placement = new Placement(1L, 2L);

    placement.setBookId(10L);

    assertThat(placement.getBookId()).isEqualTo(10L);
  }

  @Test
  void setBookId_shouldThrowWhenNull() {
    Placement placement = new Placement(1L, 2L);

    assertThatThrownBy(() -> placement.setBookId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Book ID cannot be null");
  }

  @Test
  void setShelfId_shouldUpdateShelfId() {
    Placement placement = new Placement(1L, 2L);

    placement.setShelfId(20L);

    assertThat(placement.getShelfId()).isEqualTo(20L);
  }

  @Test
  void setShelfId_shouldThrowWhenNull() {
    Placement placement = new Placement(1L, 2L);

    assertThatThrownBy(() -> placement.setShelfId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf ID cannot be null");
  }
}
