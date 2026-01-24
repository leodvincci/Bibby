package com.penrose.bibby.library.stacks.bookcase.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BookcaseTest {

  @Test
  void setShelfCapacity_equal_5() {
    Bookcase bookcase = new Bookcase(1L, "Basement", 5);
    assertEquals(5, bookcase.getShelfCapacity());
  }

  @Test
  void setShelfCapacity_not_equal_0() {
    Bookcase bookcase = new Bookcase(1L, "Basement", 0);
    assertNotEquals(0, bookcase.getShelfCapacity());
  }
}
