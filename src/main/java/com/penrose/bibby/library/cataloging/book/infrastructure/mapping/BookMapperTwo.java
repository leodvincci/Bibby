package com.penrose.bibby.library.cataloging.book.infrastructure.mapping;

import com.penrose.bibby.library.cataloging.author.core.domain.Author;
import com.penrose.bibby.library.cataloging.author.infrastructure.mapping.AuthorMapperTwo;
import com.penrose.bibby.library.cataloging.book.core.domain.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.BookFactory;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BookMapperTwo {
    BookFactory bookFactory;
    AuthorMapperTwo authorMapperTwo;


    public BookMapperTwo(BookFactory bookFactory,AuthorMapperTwo authorMapperTwo){
        this.bookFactory = bookFactory;
        this.authorMapperTwo = authorMapperTwo;
    }

//    public Book toDTOfromEntity(BookEntity bookEntity, Set<AuthorDTO> authorDTOs){
//        HashSet<AuthorDTO> authors = authorMapperTwo.toDomain(authorDTOs);
//        return bookFactory.createBookDomain(bookEntity,authors);
//    }

    public Book toDomainFromJSON(GoogleBooksResponse googleBooksResponse){
        List<String> authors = new ArrayList<>();

        for(String authorName : googleBooksResponse.items().get(0).volumeInfo().authors()) {
            String [] nameParts = authorName.split(" ", 2);
            Author author = new Author();
            author.setFirstName(nameParts[0]);
            author.setLastName(nameParts[1]);
            authors.add(author.getFirstName() + " " + author.getLastName());
        }
        String isbn = "";
        String title = (googleBooksResponse.items().get(0).volumeInfo().title());
        String publisher = (googleBooksResponse.items().get(0).volumeInfo().publisher());
        String description = (googleBooksResponse.items().get(0).volumeInfo().description());
        String publishingDate = (googleBooksResponse.items().get(0).volumeInfo().publishedDate());

        return bookFactory.createBookDomainFromJSON(title,publisher,description,isbn,authors);
    }
}
