package com.penrose.bibby.library.author;

import org.springframework.stereotype.Component;

@Component
public class AuthorFactory {

    public AuthorEntity createEntity(String firstName, String lastName){
        return new AuthorEntity(firstName, lastName);
    }

    public Author createDomain(Long id, String firstName, String lastName){
        return new Author(id,firstName, lastName);
    }

}
