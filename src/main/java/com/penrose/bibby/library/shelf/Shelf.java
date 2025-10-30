package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.bookcase.BookCase;

import java.util.List;

public class Shelf {
    private Long id;
    private BookCase bookCase;
    private String shelfLabel;
    private int shelfPosition;


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

}
