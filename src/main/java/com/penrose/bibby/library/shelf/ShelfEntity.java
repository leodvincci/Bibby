package com.penrose.bibby.library.shelf;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "shelves")
public class ShelfEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;
    private String shelfLabel;
    private Long bookcaseId;
    private int shelfPosition;

    public ShelfEntity() {

    }
    public Long getBookcaseId() {
        return bookcaseId;
    }
    public void setBookcaseId(Long bookCaseLabel) {
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
