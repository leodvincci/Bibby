package com.penrose.bibby.library.stacks.bookcase.infrastructure.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "bookcases")
public class BookcaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long bookcaseId;
  private Long userId;
  private String bookcaseLocation;
  private String bookcaseZone;
  private String bookcaseIndex;
  private int bookCapacity;
  private int shelfCapacity;

  public BookcaseEntity(
      Long userId,
      String bookcaseLocation,
      String bookcaseZone,
      String bookcaseIndex,
      int shelfCapacity,
      int bookCapacity) {
    this.userId = userId;
    this.bookcaseLocation = bookcaseLocation;
    this.bookcaseZone = bookcaseZone;
    this.bookcaseIndex = bookcaseIndex;
    this.bookCapacity = bookCapacity;
    setShelfCapacity(shelfCapacity);
  }

  public BookcaseEntity() {}

  public int getShelfCapacity() {
    return shelfCapacity;
  }

  public void setShelfCapacity(int shelfCapacity) {
    // must have at least one shelf
    if (shelfCapacity < 1) {
      shelfCapacity = 1;
    }
    this.shelfCapacity = shelfCapacity;
  }

  public Long getBookcaseId() {
    return bookcaseId;
  }

  public void setBookcaseId(Long bookCaseId) {
    this.bookcaseId = bookCaseId;
  }


  public int getBookCapacity() {
    return bookCapacity;
  }

  public void setBookCapacity(int bookCapacity) {
    this.bookCapacity = bookCapacity;
  }

  public String getBookcaseLocation() {
    return bookcaseLocation;
  }

  public void setBookcaseLocation(String bookcaseLocation) {
    this.bookcaseLocation = bookcaseLocation;
  }

  public String getBookcaseIndex() {
    return bookcaseIndex;
  }

  public void setBookcaseIndex(String bookcaseZoneIndex) {
    this.bookcaseIndex = bookcaseZoneIndex;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getBookcaseZone() {
    return bookcaseZone;
  }

  public void setBookcaseZone(String bookcaseZone) {
    this.bookcaseZone = bookcaseZone;
  }
}
