package com.penrose.bibby.library.stacks.bookcase.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BookcaseTest {

  @Test
  void constructor_setsShelfCapacity_whenValueIsAtLeast1() {
    Bookcase bookcase = new Bookcase(1L, "Basement", 5);
    assertEquals(5, bookcase.getShelfCapacity());
  }

  @Test
  void constructor_clampsShelfCapacityTo1_whenValueIsLessThan1() {
    Bookcase bookcase = new Bookcase(1L, "Basement", 0);
    assertEquals(1, bookcase.getShelfCapacity());
  }

  @Test
  void constructor_clampsShelfCapacityTo1_whenValueIsNegative() {
    Bookcase bookcase = new Bookcase(1L, "Basement", -42);
    assertEquals(1, bookcase.getShelfCapacity());
  }
}
