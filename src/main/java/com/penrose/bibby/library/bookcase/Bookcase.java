package com.penrose.bibby.library.bookcase;

public class Bookcase {
    private Long bookcaseId;
    private String bookcaseLabel;
    private int shelfCapacity;

    public int getShelfCapacity() {
        return shelfCapacity;
    }

    public void setShelfCapacity(int shelfCapacity) {
        this.shelfCapacity = shelfCapacity;
    }

    public Bookcase(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {
        this.bookcaseId = bookcaseId;
        this.bookcaseLabel = bookcaseLabel;
        this.shelfCapacity = shelfCapacity;
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
