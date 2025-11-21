package com.penrose.bibby.library.author;

public class AuthorMapper {


    public static AuthorEntity toEntity(Author author){
        String firstName = author.getFirstName();
        String lastName = author.getLastName();
        AuthorEntity authorEntity = new AuthorEntity();
        authorEntity.setFirstName(firstName);
        authorEntity.setLastName(lastName);
        authorEntity.setFullName(firstName + " " + lastName);
        return authorEntity;
    }

    public static Author toDomain(Long id, String firstName, String lastName){
        return new Author(id,firstName,lastName);
    }

}
