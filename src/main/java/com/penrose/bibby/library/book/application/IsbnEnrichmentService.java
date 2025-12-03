package com.penrose.bibby.library.book.application;

import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.author.domain.AuthorFactory;
import com.penrose.bibby.library.author.application.AuthorService;
import com.penrose.bibby.library.book.domain.*;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.book.infrastructure.mapping.BookMapperTwo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class IsbnEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(IsbnEnrichmentService.class);
    AuthorFactory authorFactory;
    BookMapperTwo bookMapper;
    BookService bookService;
    AuthorService authorService;

    public IsbnEnrichmentService( AuthorService authorService, BookService bookService, AuthorFactory authorFactory, BookMapperTwo bookMapper) {
        this.authorFactory = authorFactory;
        this.bookMapper = bookMapper;
        this.bookService = bookService;
        this.authorService = authorService;
    }

    public BookEntity enrichIsbn(GoogleBooksResponse googleBooksResponse, String isbn){
        if (googleBooksResponse == null || googleBooksResponse.items() == null || googleBooksResponse.items().isEmpty()) {
            throw new IllegalArgumentException("No book data found for ISBN: " + isbn);
        }

        Book book = bookMapper.toDomainFromJSON(googleBooksResponse);
        var pretty = """
        -------------------------------------------
        📚  Enriched Book Details
        -------------------------------------------
        Title       : %s
        Author(s)   : %s
        Publisher   : %s
        ISBN-13     : %s

        Description :
        %s
        -------------------------------------------
        """.formatted(
                book.getTitle(),
                book.getAuthors()
                        .stream()
                        .map(a -> a.getFirstName() + " " + a.getLastName())
                        .collect(Collectors.joining(", ")),
                book.getPublisher(),
                isbn,
                wrapText(book.getDescription(), 80)
        );

        log.info("\n{}", pretty);
        HashSet<AuthorEntity> authors = new HashSet<>();
        for(Author author : book.getAuthors()){
            AuthorEntity authorEntity = authorFactory.createEntity(author.getFirstName(),author.getLastName());
            authorService.saveAuthor(authorEntity);
            authors.add(authorEntity);
        }

        BookEntity bookEntity = new BookEntity();
        bookEntity.setAuthors(authors);
        bookEntity.setIsbn(isbn);
        bookEntity.setAvailabilityStatus(String.valueOf(AvailabilityStatus.AVAILABLE));
        bookEntity.setCreatedAt(LocalDate.now());
        bookEntity.setTitle(book.getTitle());
        bookEntity.setDescription(book.getDescription());
        bookEntity.setPublisher(book.getPublisher());
//        bookEntity.setPublicationYear(Integer.parseInt(book.getPublishedDate().split("-")[0]));
        System.out.println("book = " + bookEntity);
        bookService.saveBook(bookEntity);
        return bookEntity;
    }




    private String wrapText(String text, int width) {
        if (text == null) {
            return ""; // or null, or some placeholder
        }

        return text.replaceAll("(.{1," + width + "})(\\s+|$)", "$1\n");
    }
}
