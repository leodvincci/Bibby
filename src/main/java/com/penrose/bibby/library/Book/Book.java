package com.penrose.bibby.library.Book;

import java.time.LocalDate;
import java.util.Objects;

import com.penrose.bibby.library.Author.Author;
import com.penrose.bibby.library.Genre.Genre;
import com.penrose.bibby.library.Shelf.*;

public class Book {
    private Integer id;
    private String title;
    private Author author;
    private String isbn;
    private String publisher;
    private String publicationYear;
    private Genre genre;
    private Shelf shelf;
    private Enum<BookStatus> status;
    private Integer checkoutCount;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    public Book(){

    }

    public Book(Integer id, String title, Author author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
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

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public Shelf getShelf() {
        return shelf;
    }

    public void setShelf(Shelf shelf) {
        this.shelf = shelf;
    }

    public Enum<BookStatus> getStatus() {
        return status;
    }

    public void setStatus(Enum<BookStatus> status) {
        this.status = status;
    }

    public Integer getCheckoutCount() {
        return checkoutCount;
    }

    public void setCheckoutCount(Integer checkoutCount) {
        this.checkoutCount = checkoutCount;
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


    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author=" + author +
                ", isbn='" + isbn + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publicationYear='" + publicationYear + '\'' +
                ", genre=" + genre +
                ", shelf=" + shelf +
                ", status=" + status +
                ", checkoutCount=" + checkoutCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(id, book.id) && Objects.equals(title, book.title) && Objects.equals(author, book.author) && Objects.equals(isbn, book.isbn) && Objects.equals(publisher, book.publisher) && Objects.equals(publicationYear, book.publicationYear) && Objects.equals(genre, book.genre) && Objects.equals(shelf, book.shelf) && Objects.equals(status, book.status) && Objects.equals(checkoutCount, book.checkoutCount) && Objects.equals(createdAt, book.createdAt) && Objects.equals(updatedAt, book.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, author, isbn, publisher, publicationYear, genre, shelf, status, checkoutCount, createdAt, updatedAt);
    }
}

