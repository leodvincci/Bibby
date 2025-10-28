package com.penrose.bibby.library.book;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "books")
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String title;

    private String isbn;

    private String publisher;

    private String publicationYear;

    private String genre;

    private int edition;

    private String description;

    private Long  authorId;

    private Long shelfId;

//    private boolean checkedOut;

    private Integer checkoutCount;

    private String bookStatus;

    private java.time.LocalDate createdAt;

    private java.time.LocalDate updatedAt;




    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getEdition() {
        return edition;
    }

    public void setEdition(int edition) {
        this.edition = edition;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getShelfId() {
        return shelfId;
    }

    public void setShelfId(Long shelfId) {
        this.shelfId = shelfId;
    }

//    public boolean isCheckedOut() {
//        return checkedOut;
//    }
//
//    public void setCheckedOut(boolean checkedOut) {
//        this.checkedOut = checkedOut;
//    }

    public Integer getCheckoutCount() {
        return checkoutCount;
    }

    public void setCheckoutCount(Integer checkoutCount) {
        this.checkoutCount = checkoutCount;
    }

    public String getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }


}
