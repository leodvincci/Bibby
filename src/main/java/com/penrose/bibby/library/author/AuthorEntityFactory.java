package com.penrose.bibby.library.author;

import org.springframework.stereotype.Component;

@Component
public class AuthorEntityFactory {
    public AuthorEntity createEntity(String firstName, String lastName){
        return new AuthorEntity(firstName, lastName);
    }
}
