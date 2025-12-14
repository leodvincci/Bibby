package com.penrose.bibby.library.catalog.author.infrastructure.entity;

import com.penrose.bibby.library.catalog.book.infrastructure.entity.BookEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "authors")
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long authorId;
    private String firstName;
    private String lastName;

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }

    public AuthorEntity(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public AuthorEntity() {
    }

    public HashSet<BookEntity> getBooks() {
        return (HashSet<BookEntity>) books;
    }

    public void setBooks(HashSet<BookEntity> books) {
        this.books = books;
    }

    @ManyToMany(mappedBy = "authors")
    private Set<BookEntity> books = new HashSet<>();

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long id) {
        this.authorId = id;
    }

}
