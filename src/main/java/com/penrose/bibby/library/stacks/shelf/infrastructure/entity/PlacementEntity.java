package com.penrose.bibby.library.stacks.shelf.infrastructure.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "placements")
public class PlacementEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long bookId;
  private Long shelfId;

  public PlacementEntity() {}

  public PlacementEntity(Long bookId, Long shelfId) {
    this.bookId = bookId;
    this.shelfId = shelfId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBookId() {
    return bookId;
  }

  public void setBookId(Long bookId) {
    this.bookId = bookId;
  }

  public Long getShelfId() {
    return shelfId;
  }

  public void setShelfId(Long shelfId) {
    this.shelfId = shelfId;
  }
}
