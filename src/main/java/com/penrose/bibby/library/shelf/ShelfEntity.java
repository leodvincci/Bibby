package com.penrose.bibby.library.shelf;

import jakarta.persistence.*;

@Entity
@Table(name = "shelves")
public class ShelfEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;
    private String shelfLabel;
    private String bookcaseId;
    private int shelfPosition;

    public ShelfEntity() {

    }
    public String getBookcaseId() {
        return bookcaseId;
    }
    public void setBookcaseId(String bookCaseLabel) {
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
        this.shelfPosition = shelfPosition;
    }
}
