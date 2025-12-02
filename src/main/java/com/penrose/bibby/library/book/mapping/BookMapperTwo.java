package com.penrose.bibby.library.book.mapping;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorMapperTwo;
import com.penrose.bibby.library.book.domain.Book;
import com.penrose.bibby.library.book.domain.BookEntity;
import com.penrose.bibby.library.book.domain.BookFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
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
