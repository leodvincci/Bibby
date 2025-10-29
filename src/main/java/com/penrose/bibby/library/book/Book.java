package com.penrose.bibby.library.book;

import java.time.LocalDate;
import java.util.Objects;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.genre.Genre;
import com.penrose.bibby.library.shelf.*;

public class Book {
    private Long id;
    private int edition;
    private String title;
    private Author author;
    private String isbn;
    private String publisher;
    private String publicationYear;
    private Genre genre;
    private Shelf shelf;
    private String description;
    private BookStatus status;
    private Integer checkoutCount;
    private LocalDate createdAt;
    private LocalDate updatedAt;


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Book(){

    }

    public Book(Long id, String title, Author author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

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

    public BookStatus getStatus() {
        return status;
    }

    public void setStatus(BookStatus status) {
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

    public int getEdition() {
        return edition;
    }

    public void setEdition(int edition) {
        this.edition = edition;
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
                ", description='" + description + '\'' +
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
        return Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

