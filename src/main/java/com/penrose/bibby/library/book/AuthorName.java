package com.penrose.bibby.library.book;

public record AuthorName (String firstName, String lastName) {


    @Override
    public String toString() {
        return "AuthorName{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }


}
