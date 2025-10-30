package com.penrose.bibby.library.bookcase;

public class Bookcase {
    private Long bookcaseId;
    private String bookcaseLabel;

    public Bookcase(Long bookcaseId, String bookcaseLabel) {
        this.bookcaseId = bookcaseId;
        this.bookcaseLabel = bookcaseLabel;
    }

    public Long getBookcaseId() {
        return bookcaseId;
    }

    public void setBookcaseId(Long bookcaseId) {
        this.bookcaseId = bookcaseId;
    }

    public String getBookcaseLabel() {
        return bookcaseLabel;
    }

    public void setBookcaseLabel(String bookcaseLabel) {
        this.bookcaseLabel = bookcaseLabel;
    }
}
