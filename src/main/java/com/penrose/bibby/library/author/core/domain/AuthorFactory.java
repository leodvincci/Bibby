package com.penrose.bibby.library.author.domain;

import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthorFactory {

    public Author create(String firstName, String lastName){
        return new Author(firstName, lastName);
    }

    public AuthorEntity createEntity(String firstName, String lastName){
        return new AuthorEntity(firstName, lastName);
    }

    public Author createDomain(Long id, String firstName, String lastName){
        return new Author(id,firstName, lastName);
    }

}
