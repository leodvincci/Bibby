package com.penrose.bibby.library.book.infrastructure.entity;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.book.domain.AvailabilityStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "books")
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;
    private String title;
    private String isbn;
    private String publisher;
    private int publicationYear;
    private String genre;
    private int edition;
    private String description;
    private Long shelfId;
    private String availabilityStatus;
    private java.time.LocalDate createdAt;
    private java.time.LocalDate updatedAt;

    public BookEntity() {
    }

    public BookEntity(String title, HashSet<AuthorEntity> authors) {
        this.title = title;
        this.authors = authors;
        this.createdAt = LocalDate.now();
        this.availabilityStatus = AvailabilityStatus.AVAILABLE.toString();
    }

    @ManyToMany
    @JoinTable(
            name = "book_authors", // the middle box
            joinColumns = @JoinColumn(name = "book_id"), // link to books
            inverseJoinColumns = @JoinColumn(name = "author_id") // link to authors
    )
    private Set<AuthorEntity> authors = new HashSet<>();

    public Long getBookId() {
        return bookId;
    }

    public Set<AuthorEntity> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<AuthorEntity> authors) {
        this.authors.addAll(authors);
    }

    public void setBookId(Long id) {
        this.bookId = id;
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
    public int getPublicationYear() {
        return publicationYear;
    }
    public void setPublicationYear(int publicationYear) {
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

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }
    public void setAvailabilityStatus(String bookStatus) {
        this.availabilityStatus = bookStatus;
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

    public void checkIn(){
        if(availabilityStatus == null){
            throw new IllegalStateException("Book has no availability status");
        }
        if(availabilityStatus.equals(AvailabilityStatus.CHECKED_OUT.toString())){
            setAvailabilityStatus(AvailabilityStatus.AVAILABLE.toString());
        }else{
            throw new IllegalStateException("Book is not checked out");
        }
    }

    @Override
    public String toString() {
        return "BookEntity{" +
                "bookId=" + bookId +
                ", title='" + title + '\'' +

                '}';
    }
}
