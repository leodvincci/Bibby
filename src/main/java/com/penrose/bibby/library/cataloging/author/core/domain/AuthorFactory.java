package com.penrose.bibby.library.cataloging.author.core.domain;

import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthorFactory {

    public Author create(String firstName, String lastName){
        return new Author(firstName, lastName);
    }

    public AuthorEntity createEntity(String firstName, String lastName){
        return new AuthorEntity(firstName, lastName);
    }

    public Author createDomain(AuthorId id, String firstName, String lastName){
        return new Author(id,firstName, lastName);
    }

}
