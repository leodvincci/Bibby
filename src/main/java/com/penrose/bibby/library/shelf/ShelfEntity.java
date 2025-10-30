package com.penrose.bibby.library.shelf;

import jakarta.persistence.*;

@Entity
@Table(name = "Shelves")
public class ShelfEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;


    private String shelfLabel;
    private String bookCaseLabel;
   private String book;
   private String author;

    public ShelfEntity() {

    }

    public String getBookCaseLabel() {
        return bookCaseLabel;
    }

    public void setBookCaseLabel(String bookCaseLabel) {
        this.bookCaseLabel = bookCaseLabel;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public String getShelfLabel() {
        return shelfLabel;
    }

    public void setShelfLabel(String label) {
        this.shelfLabel = label;
    }

    public Long getShelfId() {
        return shelfId;
    }
    public void setShelfId(Long id) {
        this.shelfId = id;
    }


}
