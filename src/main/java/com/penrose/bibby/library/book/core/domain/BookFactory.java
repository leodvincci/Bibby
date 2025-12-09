package com.penrose.bibby.library.book.core.domain;

import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.book.AuthorName;
import com.penrose.bibby.library.book.AuthorRef;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import org.springframework.stereotype.Component;

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

    public Book createBookDomain(BookEntity bookEntity, List<AuthorRef> authors){
        Book book = new Book();
        BookId bookId = new BookId(bookEntity.getBookId());
        Title title = new Title(bookEntity.getTitle());
        book.setBookId(bookId);
        book.setTitle(title);
        book.setAuthors(authors);
        book.setAvailabilityStatus(AvailabilityStatus.valueOf(bookEntity.getAvailabilityStatus()));
        return book;
    }

    public Book createBookDomainFromJSON(String title, String publisher, String description, String isbn, List<String> authors){
        List<AuthorRef> authorRefs = authors.stream()
                .map(name -> {
                    String[] parts = name.split(" ", 2);
                    String firstName = parts.length > 0 ? parts[0] : "";
                    String lastName = parts.length > 1 ? parts[1] : "";
                    return new AuthorRef(null ,new AuthorName(firstName,lastName));
                })
                .toList();
        Book book = new Book();
        book.setIsbn(new Isbn(isbn));
        book.setTitle(new Title(title));
        book.setAuthors(authorRefs);
        book.setDescription(description);
        book.setPublisher(publisher);
        book.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        return book;
    }

}
