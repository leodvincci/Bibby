package com.penrose.bibby.library.book.domain;

import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BookFactory {

    public BookEntity createBookEntity(String title, Set<AuthorEntity> authors){
        BookEntity bookEntity = new BookEntity();
        bookEntity.setTitle(title);
        bookEntity.setAuthors(authors);
        bookEntity.setCreatedAt(java.time.LocalDate.now());
        bookEntity.setAvailabilityStatus(AvailabilityStatus.AVAILABLE.name());
        return bookEntity;
    }

    public Book createBookDomain(BookEntity bookEntity, List<String> authors){
        Book book = new Book();
        book.setId(bookEntity.getBookId());
        book.setTitle(bookEntity.getTitle());
        book.setAuthors(authors);
        book.setAvailabilityStatus(AvailabilityStatus.valueOf(bookEntity.getAvailabilityStatus()));
        return book;
    }

    public Book createBookDomainFromJSON(String title, String publisher, String description, String isbn, List<String> authors){
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthors(authors);
        book.setDescription(description);
        book.setPublisher(publisher);
        book.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        return book;
    }

}
