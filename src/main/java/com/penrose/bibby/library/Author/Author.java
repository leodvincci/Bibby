package com.penrose.bibby.library.Author;

import java.util.Objects;

public class Author {
    private String firstName;
    private String lastName;
    private Character middleInitial;
    private String fullName;

    public Author(String firstName, Character middleInitial, String lastName){
        this.firstName = firstName;
        this.middleInitial = middleInitial;
        this.lastName = lastName;
        this.fullName = String.format("%s %c %s", firstName,middleInitial,lastName);
    }

    public Author() {
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

    public Character getMiddleInitial() {
        return middleInitial;
    }

    public void setMiddleInitial(Character middleInitial) {
        this.middleInitial = middleInitial;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return "Author{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", middleInitial=" + middleInitial +
                ", fullName='" + fullName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Author author = (Author) o;
        return Objects.equals(firstName, author.firstName) && Objects.equals(lastName, author.lastName) && Objects.equals(middleInitial, author.middleInitial) && Objects.equals(fullName, author.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, middleInitial, fullName);
    }
}
