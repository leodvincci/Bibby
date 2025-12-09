package com.penrose.bibby.library.author.infrastructure.mapping;

import com.penrose.bibby.library.author.core.domain.AuthorFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthorMapperTwo {
    AuthorFactory authorFactory;

    public AuthorMapperTwo(AuthorFactory authorFactory){
        this.authorFactory = authorFactory;
    }

}
