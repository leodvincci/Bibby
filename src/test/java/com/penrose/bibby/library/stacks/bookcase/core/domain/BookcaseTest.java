package com.penrose.bibby.library.stacks.bookcase.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import org.junit.jupiter.api.Test;

class BookcaseTest {

  @Test
  void constructor_setsShelfCapacity_whenValueIsAtLeast1() {
    Bookcase bookcase = new Bookcase(1L, null, 5, 0, null, null, null);
    assertEquals(5, bookcase.getShelfCapacity());
  }

  @Test
  void constructor_clampsShelfCapacityTo1_whenValueIsLessThan1() {
    Bookcase bookcase = new Bookcase(1L, null, 0, 0, null, null, null);
    assertEquals(1, bookcase.getShelfCapacity());
  }

  @Test
  void constructor_clampsShelfCapacityTo1_whenValueIsNegative() {
    Bookcase bookcase = new Bookcase(1L, null, -42, 0, null, null, null);
    assertEquals(1, bookcase.getShelfCapacity());
  }
}
