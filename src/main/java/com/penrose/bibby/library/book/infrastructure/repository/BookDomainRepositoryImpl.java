package com.penrose.bibby.library.book.infrastructure.repository;

import com.penrose.bibby.library.author.infrastructure.repository.AuthorRepository;
import com.penrose.bibby.library.book.domain.Book;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.book.infrastructure.mapping.BookMapperTwo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BookDomainRepositoryImpl implements BookDomainRepository{
    private final BookMapperTwo bookMapperTwo;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookDomainRepositoryImpl(BookMapperTwo bookMapperTwo, BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookMapperTwo = bookMapperTwo;

        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
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
