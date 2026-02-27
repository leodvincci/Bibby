package com.penrose.bibby.library.stacks.shelf.core.domain.model;

public class Placement {
  Long bookId;
  Long shelfId;

  public Placement(Long bookId, Long shelfId) {
    this.bookId = bookId;
    this.shelfId = shelfId;
  }

  public Long getBookId() {
    if (bookId == null) {
      throw new IllegalStateException("Book ID cannot be null");
    }
    return bookId;
  }

  public void setBookId(Long bookId) {
    if (bookId == null) {
      throw new IllegalArgumentException("Book ID cannot be null");
    }
    this.bookId = bookId;
  }

  public Long getShelfId() {
    return shelfId;
  }

  public void setShelfId(Long shelfId) {
    if (shelfId == null) {
      throw new IllegalArgumentException("Shelf ID cannot be null");
    }
    this.shelfId = shelfId;
  }
}
