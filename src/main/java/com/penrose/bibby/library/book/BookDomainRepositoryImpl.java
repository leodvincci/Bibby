package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BookDomainRepositoryImpl implements BookDomainRepository{
    private final BookMapperTwo bookMapperTwo;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final AuthorMapperTwo authorMapperTwo;

    public BookDomainRepositoryImpl(BookMapperTwo bookMapperTwo, BookRepository bookRepository, AuthorRepository authorRepository, AuthorMapperTwo authorMapperTwo) {
        this.bookMapperTwo = bookMapperTwo;

        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.authorMapperTwo = authorMapperTwo;
    }

    @Override
    public List<Book> getBooksByShelfId(Long shelfId) {
        List<BookEntity> bookEntities = bookRepository.findByShelfId(shelfId);
        List<Book> books = new ArrayList<>();

        for(BookEntity bookEntity : bookEntities){
            books.add(bookMapperTwo.toDomain(bookEntity,authorRepository.findByBooks_BookId(bookEntity.getBookId())));
        }
        return books;
    }
}
