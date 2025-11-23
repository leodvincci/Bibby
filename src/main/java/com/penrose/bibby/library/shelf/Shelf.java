package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.bookcase.Bookcase;

import java.util.List;

public class Shelf {
    private Long id;
    private Bookcase bookCase;
    private String shelfLabel;
    private String shelfDescription;
    private int shelfPosition;
    private int shelfCapacity;



    public Shelf(Bookcase bookCase, String shelfLabel, int shelfPosition, int shelfCapacity) {
        this.bookCase = bookCase;
        this.shelfLabel = shelfLabel;
        this.shelfPosition = shelfPosition;
        this.shelfCapacity = shelfCapacity;
    }


    public int getShelfCapacity() {
        return shelfCapacity;
    }

    public void setShelfCapacity(int shelfCapacity) {
        this.shelfCapacity = shelfCapacity;
    }

    public Bookcase getBookCase() {
        return bookCase;
    }

    public void setBookCase(Bookcase bookCase) {
        this.bookCase = bookCase;
    }

    public String getShelfLabel() {
        return shelfLabel;
    }

    public void setShelfLabel(String shelfLabel) {
        this.shelfLabel = shelfLabel;
    }

    public int getShelfPosition() {
        return shelfPosition;
    }

    public void setShelfPosition(int shelfPosition) {
        this.shelfPosition = shelfPosition;
    }

    public Shelf() {

    }

    public String getLabel() {
        return shelfLabel;
    }

    public void setLabel(String label) {
        this.shelfLabel = label;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public void addToShelf(Book book){

    }

    public String getShelfDescription() {
        return shelfDescription;
    }

    public void setShelfDescription(String shelfDescription) {
        this.shelfDescription = shelfDescription;
    }
}
