package com.penrose.bibby.library.bookcase;

import jakarta.persistence.*;

@Entity
@Table(name = "bookcases")
public class BookcaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookcaseId;
    private String bookcaseLabel;


    public BookcaseEntity(String bookcaseLabel) {
        this.bookcaseLabel = bookcaseLabel;
    }

    public BookcaseEntity(Long bookcaseId, String bookcaseLabel) {
        this.bookcaseId = bookcaseId;
        this.bookcaseLabel = bookcaseLabel;
    }

    public BookcaseEntity() {

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
