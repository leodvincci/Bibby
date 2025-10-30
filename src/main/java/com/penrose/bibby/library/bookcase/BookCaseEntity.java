package com.penrose.bibby.library.bookcase;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class BookCaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookCaseId;
    private String bookCaseLabel;

    public BookCaseEntity(Long bookCaseId, String bookCaseLabel) {
        this.bookCaseId = bookCaseId;
        this.bookCaseLabel = bookCaseLabel;
    }

    public BookCaseEntity() {

    }

    public Long getBookCaseId() {
        return bookCaseId;
    }

    public void setBookCaseId(Long bookCaseId) {
        this.bookCaseId = bookCaseId;
    }

    public String getBookCaseLabel() {
        return bookCaseLabel;
    }

    public void setBookCaseLabel(String bookCaseLabel) {
        this.bookCaseLabel = bookCaseLabel;
    }
}
