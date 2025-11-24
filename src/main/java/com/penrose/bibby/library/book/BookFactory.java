package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
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

    public Book createBookDomain(BookEntity bookEntity, HashSet<Author> authors){
        Book book = new Book();
        book.setId(bookEntity.getBookId());
        book.setTitle(bookEntity.getTitle());
        book.setAuthors(authors);
        book.setAvailabilityStatus(AvailabilityStatus.valueOf(bookEntity.getAvailabilityStatus()));
        return book;
    }

}
