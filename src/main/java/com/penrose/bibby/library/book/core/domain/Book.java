package com.penrose.bibby.library.book.core.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class Book {
    private BookId bookId;
    private int edition;
    private Title title;
    private List<AuthorRef> authors;
    private Isbn isbn;
    private String genre;
    private String publisher;
    private int publicationYear;
    private Long shelfId;
    private String description;
    private AvailabilityStatus availabilityStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String publishedDate;

    public Book() {
    }

    public Book(BookId bookId, Title title, List<AuthorRef> authors) {
        this.bookId = bookId;
        this.title = title;
        this.authors = authors;
    }

    public Book(BookId bookId, int edition, Title title, List<AuthorRef> authors, Isbn isbn, String genre, String publisher, int publicationYear, Long shelfId, String description, AvailabilityStatus availabilityStatus, LocalDate createdAt, LocalDate updatedAt, String publishedDate) {
        this.bookId = bookId;
        this.edition = edition;
        this.title = title;
        this.authors = authors;
        this.isbn = isbn;
        this.genre = genre;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.shelfId = shelfId;
        this.description = description;
        this.availabilityStatus = availabilityStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedDate = publishedDate;
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


    public BookId getBookId() {
        return bookId;
    }

    public void setBookId(BookId bookId) {
        this.bookId = bookId;
    }

    public Title getTitle() {
        return title;
    }

    public void setTitle(Title title) {
        this.title = title;
    }

    public List<AuthorRef> getAuthors() {
        return authors;
    }

    public void setAuthors(List<AuthorRef> authors) {
        this.authors = authors;
    }

    public Isbn getIsbn() {
        return isbn;
    }

    public void setIsbn(Isbn isbn) {
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
    public Long getShelfId() {
        return shelfId;
    }
    public void setShelfId(Long shelfId) {
        this.shelfId = shelfId;
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
                "id=" + bookId +
                ", edition=" + edition +
                ", title='" + title + '\'' +
                ", authors=" + authors +
                ", isbn='" + isbn + '\'' +
                ", genre='" + genre + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publicationYear=" + publicationYear +
                ", shelfId=" + shelfId +
                ", description='" + description + '\'' +
                ", availabilityStatus=" + availabilityStatus +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", publishedDate='" + publishedDate + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(bookId, book.bookId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bookId);
    }
}

