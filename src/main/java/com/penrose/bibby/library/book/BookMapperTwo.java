package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorMapperTwo;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BookMapperTwo {
    BookFactory bookFactory;
    AuthorMapperTwo authorMapperTwo;


    public BookMapperTwo(BookFactory bookFactory,AuthorMapperTwo authorMapperTwo){
        this.bookFactory = bookFactory;
        this.authorMapperTwo = authorMapperTwo;
    }

    public Book toDomain(BookEntity bookEntity, Set<AuthorEntity> authorEntities){
        HashSet<Author> authors = authorMapperTwo.toDomain(authorEntities);
        return bookFactory.createBookDomain(bookEntity,authors);
    }
}
