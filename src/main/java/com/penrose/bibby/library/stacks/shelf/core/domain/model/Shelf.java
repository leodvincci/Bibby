package com.penrose.bibby.library.stacks.shelf.core.domain.model;

import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;

public class Shelf {

  private ShelfId shelfId;
  private String shelfLabel;
  private int shelfPosition;
  private int bookCapacity;
  private List<Long> books;
  private Long bookcaseId;

  public Shelf(
      String shelfLabel,
      int shelfPosition,
      int bookCapacity,
      ShelfId shelfId,
      List<Long> books,
      Long bookcaseId) {

    setShelfLabel(shelfLabel);
    setShelfPosition(shelfPosition);
    setBookCapacity(bookCapacity);
    setBooks(books);
    setBookcaseId(bookcaseId);
    setShelfId(shelfId);
  }

  public boolean isFull() {
    return books.size() >= bookCapacity;
  }

  public int getBookCount() {
    return books.size();
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
    if (shelfLabel == null || shelfLabel.isBlank()) {
      throw new IllegalArgumentException("Shelf label cannot be null or blank");
    }
    this.shelfLabel = shelfLabel;
  }

  public Long getBookcaseId() {
    return bookcaseId;
  }

  public void setBookcaseId(Long bookcaseId) {
    if (bookcaseId == null) {
      throw new IllegalArgumentException("Bookcase ID cannot be null");
    }
    this.bookcaseId = bookcaseId;
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

  public List<Long> getBooks() {
    return books;
  }

  public void setBooks(List<Long> books) {
    if (books == null) {
      throw new IllegalArgumentException("Books cannot be null");
    }
    this.books = books;
  }

  @Override
  public String toString() {
    return "Shelf{"
        + "shelfId="
        + shelfId
        + ", shelfLabel='"
        + shelfLabel
        + '\''
        + ", shelfPosition="
        + shelfPosition
        + ", bookCapacity="
        + bookCapacity
        + ", books="
        + books
        + ", bookcaseId="
        + bookcaseId
        + '}';
  }
}
