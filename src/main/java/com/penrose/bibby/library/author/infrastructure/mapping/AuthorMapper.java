package com.penrose.bibby.library.author.infrastructure.mapping;

import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;

public class AuthorMapper {


    public static AuthorEntity toEntity(Author author){
        String firstName = author.getFirstName();
        String lastName = author.getLastName();
        AuthorEntity authorEntity = new AuthorEntity();
        authorEntity.setFirstName(firstName);
        authorEntity.setLastName(lastName);
        return authorEntity;
    }

    public static Author toDomain(Long id, String firstName, String lastName){
        return new Author(id,firstName,lastName);
    }

}
