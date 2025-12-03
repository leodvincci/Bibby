package com.penrose.bibby.library.author.infrastructure.mapping;

import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.author.domain.AuthorFactory;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AuthorMapperTwo {
    AuthorFactory authorFactory;

    public AuthorMapperTwo(AuthorFactory authorFactory){
        this.authorFactory = authorFactory;
    }

    public HashSet<Author> toDomain(Set<AuthorEntity> authorEntity){
//        List<Author> authors = authorEntity.stream().map(author -> new Author(author.getFirstName(), author.getLastName())).toList();
        HashSet<Author> authors = new HashSet<>();
        for(AuthorEntity author : authorEntity){
            authors.add(authorFactory.createDomain(author.getAuthorId(), author.getFirstName(), author.getLastName()));
        }
        return authors;
    }


}
