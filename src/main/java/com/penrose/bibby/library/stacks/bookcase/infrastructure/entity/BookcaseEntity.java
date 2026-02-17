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
  private int shelfCapacity;
  private int bookCapacity;

  public BookcaseEntity() {}

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
    this.shelfCapacity = shelfCapacity;
    this.bookCapacity = bookCapacity;
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

  public int getShelfCapacity() {
    return shelfCapacity;
  }

  public void setShelfCapacity(int shelfCapacity) {
    this.shelfCapacity = shelfCapacity;
  }

  public int getBookCapacity() {
    return bookCapacity;
  }

  public void setBookCapacity(int bookCapacity) {
    this.bookCapacity = bookCapacity;
  }
}
