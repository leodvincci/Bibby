package com.penrose.bibby.library.book.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;

import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.shelf.domain.Shelf;
import org.stringtemplate.v4.ST;

public class Book {
    private Long id;
    private int edition;
    private String title;
    private HashSet<Author> authors;
    private String isbn;
    private String genre;
    private String publisher;
    private int publicationYear;
    private Shelf shelf;
    private String description;
    private AvailabilityStatus availabilityStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String publishedDate;

    public Book() {
    }

    public Book(Long id, String title, HashSet<Author> authors) {
        this.id = id;
        this.title = title;
        this.authors = authors;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public boolean checkout(){

        //needs to check if a book is already checked out
        if(this.availabilityStatus != AvailabilityStatus.AVAILABLE){
            throw new IllegalStateException("Book is already checked out");
        }

        //change status to check out
        this.availabilityStatus = AvailabilityStatus.CHECKED_OUT;
        return true;
    }

    public boolean isCheckedOut(){
        return availabilityStatus == AvailabilityStatus.CHECKED_OUT;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public void setAuthors(HashSet<Author> authors) {
        this.authors = authors;
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

    public HashSet<Author> getAuthors() {
        return authors;
    }

    public void addAuthors(Author author) {
        authors.add(author);
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

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public Shelf getShelf() {
        return shelf;
    }

    public void setShelf(Shelf shelf) {
        this.shelf = shelf;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus status) {
        this.availabilityStatus = status;
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
                "title='" + title + '\'' +
                ", authors=" + authors +
                ", isbn='" + isbn + '\'' +
                ", publisher='" + publisher + '\'' +
                ", description='" + description + '\'' +
                ", availabilityStatus=" + availabilityStatus +
                ", publishedDate='" + publishedDate + '\'' +
                ", updatedAt=" + updatedAt +
                ", shelf=" + shelf +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

