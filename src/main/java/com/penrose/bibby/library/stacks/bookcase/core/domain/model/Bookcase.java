package com.penrose.bibby.library.stacks.bookcase.core.domain.model;

public class Bookcase {
  private Long bookcaseId;
  private String bookcaseLocation;
  private int shelfCapacity;

  public Bookcase(Long bookcaseId, int shelfCapacity) {
    this.bookcaseId = bookcaseId;
    setShelfCapacity(shelfCapacity);
  }

  public Long getBookcaseId() {
    return bookcaseId;
  }

  public void setBookcaseId(Long bookcaseId) {
    this.bookcaseId = bookcaseId;
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
}
