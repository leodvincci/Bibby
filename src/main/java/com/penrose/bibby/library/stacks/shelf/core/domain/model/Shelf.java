package com.penrose.bibby.library.stacks.shelf.core.domain.model;

import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;

public class Shelf {

  private ShelfId shelfId;
  private String shelfLabel;
  private int shelfPosition;
  private int bookCapacity;
  private List<Long> bookIds;

  public Shelf(
      String shelfLabel, int shelfPosition, int bookCapacity, ShelfId shelfId, List<Long> bookIds) {
    if (shelfLabel == null || shelfLabel.isBlank()) {
      throw new IllegalArgumentException("Shelf label cannot be null or blank");
    }
    if (shelfPosition < 1) {
      throw new IllegalArgumentException("Shelf position must be greater than 0");
    }
    if (bookCapacity < 1) {
      throw new IllegalArgumentException("Book capacity cannot be negative");
    }

    this.shelfLabel = shelfLabel;
    this.shelfPosition = shelfPosition;
    this.bookCapacity = bookCapacity;
    this.shelfId = shelfId;
  }

  public boolean isFull() {
    return bookIds != null && bookIds.size() >= bookCapacity;
  }

  public int getBookCount() {
    return bookIds.size();
  }

  public ShelfId getShelfId() {
    return shelfId;
  }

  public void setShelfId(ShelfId shelfId) {
    this.shelfId = shelfId;
  }

  public Long getId() {
    return this.shelfId.shelfId();
  }

  public String getShelfLabel() {
    return shelfLabel;
  }

  public void setShelfLabel(String shelfLabel) {
    this.shelfLabel = shelfLabel;
  }

  public int getShelfPosition() {
    return shelfPosition;
  }

  public void setShelfPosition(int shelfPosition) {
    if (shelfPosition < 1) {
      throw new IllegalArgumentException("Shelf position must be greater than 0");
    }
    this.shelfPosition = shelfPosition;
  }

  public int getBookCapacity() {
    return bookCapacity;
  }

  public void setBookCapacity(int bookCapacity) {
    if (bookCapacity < 1) {
      throw new IllegalArgumentException("Book capacity cannot be negative");
    }
    this.bookCapacity = bookCapacity;
  }

  public List<Long> getBookIds() {
    return bookIds;
  }

  public void setBookIds(List<Long> bookIds) {
    this.bookIds = bookIds;
  }

  public void setBooks(List<Long> books) {
    this.bookIds = books;
  }

  @Override
  public String toString() {
    return "Shelf{"
        + "shelf="
        + shelfId
        + ", shelfLabel='"
        + shelfLabel
        + '\''
        + ", shelfDescription='"
        + '\''
        + ", shelfPosition="
        + shelfPosition
        + ", bookCapacity="
        + bookCapacity
        + ", books="
        + bookIds
        + '}';
  }
}
