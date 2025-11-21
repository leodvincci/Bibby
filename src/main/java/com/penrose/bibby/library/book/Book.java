package com.penrose.bibby.library.book;

import java.time.LocalDate;
import java.util.Objects;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.genre.Genre;
import com.penrose.bibby.library.shelf.Shelf;

public class Book {
    private Long id;
    private int edition;
    private String title;
    //Why does my Book DOMAIN model not have an Author? But an AuthorEntity?
    //AuthorEntry should probably be a Domain Model and not an Entity.
    //Mixing Layers.
    private AuthorEntity authorEntity;
    private String isbn;
    private String publisher;
    private int publicationYear;
    private Genre genre;
    private Shelf shelf;
    private String description;
    private AvailabilityStatus availabilityStatus;
    private Integer checkoutCount;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    public Book() {
    }

    public Book(Long id, String title, AuthorEntity authorEntity) {
        this.id = id;
        this.title = title;
        this.authorEntity = authorEntity;
    }
    
    public void checkout(){

        //needs to check if a book is already checked out
        if(this.availabilityStatus != AvailabilityStatus.AVAILABLE){
            throw new IllegalStateException("Book is already checked out");
        }

        //change status to check out
        this.availabilityStatus = AvailabilityStatus.CHECKED_OUT;
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

    public AuthorEntity getAuthor() {
        return authorEntity;
    }

    public void setAuthor(AuthorEntity authorEntity) {
        this.authorEntity = authorEntity;
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

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus status) {
        this.availabilityStatus = status;
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
                ", author=" + authorEntity +
                ", isbn='" + isbn + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publicationYear=" + publicationYear +
                ", genre=" + genre +
                ", shelf=" + shelf +
                ", description='" + description + '\'' +
                ", status=" + availabilityStatus +
                ", checkoutCount=" + checkoutCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
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

