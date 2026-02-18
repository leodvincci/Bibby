package com.penrose.bibby.library.stacks.shelf.infrastructure.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shelves")
public class ShelfEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long shelfId;

  private String shelfLabel;
  private Long bookcaseId;
  private int shelfPosition;
  private int bookCapacity;
  private String shelfDescription;

  public ShelfEntity() {}

  public ShelfEntity(Long bookcaseId, int shelfPosition, String shelfLabel, int bookCapacity) {
    if (shelfLabel == null || shelfLabel.isBlank()) {
      throw new IllegalArgumentException("Shelf label cannot be null or blank");
    }
    if (shelfPosition < 1) {
      throw new IllegalArgumentException("Shelf position must be greater than 0");
    }
    if (bookCapacity < 1) {
      throw new IllegalArgumentException("Book capacity cannot be negative");
    }
    if (bookcaseId == null) {
      throw new IllegalArgumentException("Bookcase ID cannot be null");
    }
    if (bookcaseId < 1) {
      throw new IllegalArgumentException("Bookcase ID must be greater than 0");
    }

    this.bookcaseId = bookcaseId;
    this.shelfPosition = shelfPosition;
    this.shelfLabel = shelfLabel;
    this.bookCapacity = bookCapacity;
  }

  public Long getBookcaseId() {
    return bookcaseId;
  }

  public void setBookcaseId(Long bookCaseLabel) {
    if (bookCaseLabel < 1) {
      throw new IllegalArgumentException("Bookcase ID must be greater than 0");
    }
    this.bookcaseId = bookCaseLabel;
  }

  public String getShelfLabel() {
    return shelfLabel;
  }

  public void setShelfLabel(String label) {
    this.shelfLabel = label;
  }

  public Long getShelfId() {
    return shelfId;
  }

  public void setShelfId(Long id) {

    this.shelfId = id;
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

  public void setBookCapacity(int shelfCapacity) {
    if (shelfCapacity < 1) {
      throw new IllegalArgumentException("Book capacity cannot be negative");
    }
    this.bookCapacity = shelfCapacity;
  }

  public String getShelfDescription() {
    return shelfDescription;
  }

  public void setShelfDescription(String shelfDescription) {
    this.shelfDescription = shelfDescription;
  }
}
