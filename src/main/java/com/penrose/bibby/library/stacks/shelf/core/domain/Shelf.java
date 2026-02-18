package com.penrose.bibby.library.stacks.shelf.core.domain;

import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;

public class Shelf {
  // Fields
  private ShelfId shelfId;
  // todo: remove shelfLabel
  private String shelfLabel;
  // todo: remove shelfDescription
  private String shelfDescription;
  private int shelfPosition;
  private int bookCapacity;
  private List<Long> bookIds;

  // Constructor
  public Shelf(String shelfLabel, int shelfPosition, int bookCapacity, ShelfId shelfId) {
    this.shelfLabel = shelfLabel;
    this.shelfPosition = shelfPosition;
    this.bookCapacity = bookCapacity;
    this.shelfId = shelfId;
  }

  // Business Logic Methods
  public boolean isFull() {
    return bookIds != null && bookIds.size() >= bookCapacity;
  }

  public int getBookCount() {
    return bookIds.size();
  }

  // Getters and Setters
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

  public String getLabel() {
    return shelfLabel;
  }

  public void setLabel(String label) {
    this.shelfLabel = label;
  }

  public String getShelfDescription() {
    return shelfDescription;
  }

  public void setShelfDescription(String shelfDescription) {
    this.shelfDescription = shelfDescription;
  }

  public int getShelfPosition() {
    return shelfPosition;
  }

  public void setShelfPosition(int shelfPosition) {
    this.shelfPosition = shelfPosition;
  }

  public int getBookCapacity() {
    return bookCapacity;
  }

  public void setBookCapacity(int bookCapacity) {
    this.bookCapacity = bookCapacity;
  }

  public List<Long> getBookIds() {
    return bookIds;
  }

  public void setBookIds(List<Long> bookIds) {
    this.bookIds = bookIds;
  }

  public List<Long> getBooks() {
    return bookIds;
  }

  public void setBooks(List<Long> books) {
    this.bookIds = books;
  }

  // Object Methods
  @Override
  public String toString() {
    return "Shelf{"
        + "shelf="
        + shelfId
        + ", shelfLabel='"
        + shelfLabel
        + '\''
        + ", shelfDescription='"
        + shelfDescription
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
