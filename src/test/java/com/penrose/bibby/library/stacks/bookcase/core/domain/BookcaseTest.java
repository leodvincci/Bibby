package com.penrose.bibby.library.stacks.bookcase.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import org.junit.jupiter.api.Test;

class BookcaseTest {

  @Test
  void constructor_setsShelfCapacity_whenValueIsAtLeast1() {
    Bookcase bookcase = new Bookcase(1L, 5);
    assertEquals(5, bookcase.getShelfCapacity());
  }

  @Test
  void constructor_clampsShelfCapacityTo1_whenValueIsLessThan1() {
    Bookcase bookcase = new Bookcase(1L, 0);
    assertEquals(1, bookcase.getShelfCapacity());
  }

  @Test
  void constructor_clampsShelfCapacityTo1_whenValueIsNegative() {
    Bookcase bookcase = new Bookcase(1L, -42);
    assertEquals(1, bookcase.getShelfCapacity());
  }
}
