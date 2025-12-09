package com.penrose.bibby.library.book.core.domain;

import com.penrose.bibby.library.author.core.application.AuthorService;
import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.book.infrastructure.mapping.BookMapper;
import com.penrose.bibby.library.book.infrastructure.repository.BookDomainRepository;
import com.penrose.bibby.library.book.infrastructure.repository.BookRepository;
import com.penrose.bibby.library.shelf.contracts.dtos.ShelfDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class BookDomainRepositoryImpl implements BookDomainRepository {
    private final BookMapper  bookMapper;
    private final BookRepository bookRepository;

    public BookDomainRepositoryImpl(
            BookMapper bookMapper,
            BookRepository bookRepository,
            AuthorService authorService) {

        this.bookMapper = bookMapper;
        this.bookRepository = bookRepository;

    }

    @Override
    public List<Book> getBooksByShelfId(Long shelfId) {
        List<BookEntity> bookEntities = bookRepository.findByShelfId(shelfId);
        List<Book> books = new ArrayList<>();

        Set<AuthorDTO> authorDTO = null;
        ShelfDTO shelfDTO = null;

        for(BookEntity bookEntity : bookEntities){
            Book book = bookMapper.toDomainFromEntity(bookEntity);
            books.add(book);
        }
        return books;
    }
}
