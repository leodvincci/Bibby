package com.penrose.bibby.library.book.service;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorFactory;
import com.penrose.bibby.library.book.domain.Book;
import com.penrose.bibby.library.book.domain.BookFactory;
import com.penrose.bibby.library.book.domain.BookMetaData;
import com.penrose.bibby.library.book.domain.GoogleBooksResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class BookEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(BookEnrichmentService.class);
    AuthorFactory authorFactory;



    public BookEnrichmentService(AuthorFactory authorFactory) {
        this.authorFactory = authorFactory;
    }

    public void enrichBookData(GoogleBooksResponse bookMetaData, String isbn){
        // Placeholder for future implementation
        HashSet<Author> authors = new HashSet<>();

        Book book = new Book();
        for(String authorName : bookMetaData.items().get(0).volumeInfo().authors()) {
            String [] nameParts = authorName.split(" ", 2);
            Author author = new Author();
            author.setFirstName(nameParts[0]);
            author.setLastName(nameParts[1]);
            authors.add(author);
            book.setAuthors(authors);
        }
        book.setTitle(bookMetaData.items().get(0).volumeInfo().title());
        book.setPublisher(bookMetaData.items().get(0).volumeInfo().publisher());
        book.setDescription(bookMetaData.items().get(0).volumeInfo().description());
        book.setIsbn(isbn);
        book.setPublishedDate(bookMetaData.items().get(0).volumeInfo().publishedDate());

        log.info("\nEnriched book data for: {}", book.getTitle());
        log.info(String.valueOf(book));

    }



}
