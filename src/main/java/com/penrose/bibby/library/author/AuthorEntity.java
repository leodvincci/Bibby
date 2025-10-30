package com.penrose.bibby.library.author;

import jakarta.persistence.*;

@Entity
@Table(name = "Authors")
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;

    public AuthorEntity(String firstName, String lastName) {
        this.firstName = firstName;
//        this.middleInitial = middleInitial;
        this.lastName = lastName;
        this.fullName = String.format("%s %s", firstName, lastName);
    }

    public AuthorEntity() {
    }

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



    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
