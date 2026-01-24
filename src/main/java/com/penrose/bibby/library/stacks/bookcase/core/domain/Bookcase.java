package com.penrose.bibby.library.stacks.bookcase.core.domain;

public class Bookcase {
    private Long bookcaseId;
    private String bookcaseLocation;
    private String bookcaseLabel;
    private int shelfCapacity;

    public int getShelfCapacity() {
        return shelfCapacity;
    }

    public void setShelfCapacity(int shelfCapacity) {
        //must have at least one shelf
        if (shelfCapacity < 1) {
            shelfCapacity = 1;
        }
        this.shelfCapacity = shelfCapacity;
    }

    public Bookcase(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {
        this.bookcaseId = bookcaseId;
        this.bookcaseLabel = bookcaseLabel;
        setShelfCapacity(shelfCapacity);
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

    public void setBookcaseLocation(String bookcaseLocation) {
        this.bookcaseLocation = bookcaseLocation;
    }

    public String getBookcaseLocation() {
        return bookcaseLocation;
    }

}
