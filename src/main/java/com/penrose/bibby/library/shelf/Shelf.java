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
    private List<Book> books;


    public Shelf(Bookcase bookCase, String shelfLabel, int shelfPosition, int shelfCapacity) {
        this.bookCase = bookCase;
        this.shelfLabel = shelfLabel;
        this.shelfPosition = shelfPosition;
        this.shelfCapacity = shelfCapacity;
    }

    public List<Book> getBooks() {
        return books;
    }
    public void setBooks(List<Book> books) {
        this.books = books;
    }

    public int getShelfCapacity() {
        return shelfCapacity;
    }
    public int getBookCount() {
        return books.size();
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
