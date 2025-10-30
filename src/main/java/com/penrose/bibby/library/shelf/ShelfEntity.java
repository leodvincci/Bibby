package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.Book;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ShelfEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
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

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }


}
