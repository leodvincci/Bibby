package com.penrose.bibby.library.stacks.bookcase.core.domain.model;

public class Bookcase {
  @Override
  public String toString() {
    return String.format(
        "Bookcase{id=%d, location='%s', zone='%s', index='%s', shelfCapacity=%d, bookCapacity=%d}",
        bookcaseId, bookcaseLocation, bookcaseZone, bookcaseIndex, shelfCapacity, bookCapacity);
  }

  private Long bookcaseId;
  private Long userId;
  private String bookcaseLocation;
  private String bookcaseZone;
  private String bookcaseIndex;

  private int shelfCapacity;
  private int bookCapacity;

  public Bookcase(
      Long bookcaseId,
      Long userId,
      int shelfCapacity,
      int bookCapacity,
      String bookcaseLocation,
      String bookcaseZone,
      String bookcaseIndex) {
    this.bookcaseId = bookcaseId;
    this.userId = userId;
    setShelfCapacity(shelfCapacity);
    this.bookCapacity = bookCapacity;
    this.bookcaseLocation = bookcaseLocation;
    this.bookcaseZone = bookcaseZone;
    this.bookcaseIndex = bookcaseIndex;
  }

  public Long getBookcaseId() {
    return bookcaseId;
  }

  public void setBookcaseId(Long bookcaseId) {
    this.bookcaseId = bookcaseId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getBookcaseLocation() {
    return bookcaseLocation;
  }

  public void setBookcaseLocation(String bookcaseLocation) {
    this.bookcaseLocation = bookcaseLocation;
  }

  public int getShelfCapacity() {
    return shelfCapacity;
  }

  public void setShelfCapacity(int shelfCapacity) {
    if (shelfCapacity < 1) {
      shelfCapacity = 1;
    }
    this.shelfCapacity = shelfCapacity;
  }

  public int getBookCapacity() {
    return bookCapacity;
  }

  public void setBookCapacity(int bookCapacity) {
    this.bookCapacity = bookCapacity;
  }

  public String getBookcaseZone() {
    return bookcaseZone;
  }

  public void setBookcaseZone(String bookcaseZone) {
    this.bookcaseZone = bookcaseZone;
  }

  public String getBookcaseIndex() {
    return bookcaseIndex;
  }

  public void setBookcaseIndex(String bookcaseIndex) {
    this.bookcaseIndex = bookcaseIndex;
  }
}

