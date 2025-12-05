package com.penrose.bibby.library.shelf.domain;

import com.penrose.bibby.library.book.domain.Book;
import java.util.List;

public class Shelf {
    private Long id;
    private String shelfLabel;
    private String shelfDescription;
    private int shelfPosition;
    private int bookCapacity;
    private List<Book> books;


    public Shelf(String shelfLabel, int shelfPosition, int bookCapacity) {
        this.shelfLabel = shelfLabel;
        this.shelfPosition = shelfPosition;
        this.bookCapacity = bookCapacity;
    }

    public boolean isFull(){
        return books.size() >= bookCapacity;
    }

    public List<Book> getBooks() {
        return books;
    }
    public void setBooks(List<Book> books) {
        this.books = books;
    }

    public int getBookCapacity() {
        return bookCapacity;
    }
    public int getBookCount() {
        return books.size();
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

    public String getShelfDescription() {
        return shelfDescription;
    }

    public void setShelfDescription(String shelfDescription) {
        this.shelfDescription = shelfDescription;
    }

    public void setBookCapacity(int bookCapacity) {
        this.bookCapacity = bookCapacity;
    }

    @Override
    public String toString() {
        return "Shelf{" +
                "id=" + id +
                ", shelfLabel='" + shelfLabel + '\'' +
                ", shelfDescription='" + shelfDescription + '\'' +
                ", shelfPosition=" + shelfPosition +
                ", bookCapacity=" + bookCapacity +
                ", books=" + books +
                '}';
    }
}
