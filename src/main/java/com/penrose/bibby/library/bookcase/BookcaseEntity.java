package com.penrose.bibby.library.bookcase;

import jakarta.persistence.*;

@Entity
@Table(name = "bookcases")
public class BookcaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookcaseId;
    private String bookcaseLabel;
    private String bookcaseDescription;
    private int bookCapacity;


    private int shelfCapacity;


    public BookcaseEntity(String bookcaseLabel, int shelfCapacity, int bookCapacity) {
        this.bookcaseLabel = bookcaseLabel;
        this.shelfCapacity = shelfCapacity;
        this.bookCapacity = bookCapacity;
    }

    public BookcaseEntity(Long bookcaseId, String bookcaseLabel,int shelfCapacity, int bookCapacity) {
        this.bookcaseId = bookcaseId;
        this.bookcaseLabel = bookcaseLabel;
        this.shelfCapacity = shelfCapacity;
    }

    public BookcaseEntity() {

    }

    public int getShelfCapacity() {
        return shelfCapacity;
    }

    public void setShelfCapacity(int shelfCapacity) {
        this.shelfCapacity = shelfCapacity;
    }

    public Long getBookcaseId() {
        return bookcaseId;
    }

    public void setBookcaseId(Long bookCaseId) {
        this.bookcaseId = bookCaseId;
    }

    public String getBookcaseLabel() {
        return bookcaseLabel;
    }

    public void setBookcaseLabel(String bookCaseLabel) {
        this.bookcaseLabel = bookCaseLabel;
    }

    public String getBookcaseDescription() {
        return bookcaseDescription;
    }

    public void setBookcaseDescription(String bookcaseDescription) {
        this.bookcaseDescription = bookcaseDescription;
    }

    public int getBookCapacity() {
        return bookCapacity;
    }

    public void setBookCapacity(int bookCapacity) {
        this.bookCapacity = bookCapacity;
    }
}
