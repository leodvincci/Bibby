package com.penrose.bibby.library.bookcase;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.shelf.Shelf;

import java.util.List;

public class BookCase {
    private Long bookCaseId;
    private String bookCaseLabel;

    public BookCase(Long bookCaseId, String bookCaseLabel) {
        this.bookCaseId = bookCaseId;
        this.bookCaseLabel = bookCaseLabel;
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
