package com.penrose.bibby.library.bookcase;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class BookcaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookcaseId;
    private String bookcaseLabel;


    public BookcaseEntity() {

    }

    public BookcaseEntity(Long bookcaseId, String bookcaseLabel) {
        this.bookcaseId = bookcaseId;
        this.bookcaseLabel = bookcaseLabel;
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
}
