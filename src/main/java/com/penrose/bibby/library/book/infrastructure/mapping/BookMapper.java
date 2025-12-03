package com.penrose.bibby.library.book.infrastructure.mapping;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorMapper;
import com.penrose.bibby.library.book.domain.AvailabilityStatus;
import com.penrose.bibby.library.book.domain.Book;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.shelf.Shelf;
import com.penrose.bibby.library.shelf.ShelfDomainRepositoryImpl;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfMapper;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
@Component
public class BookMapper {
    private final ShelfDomainRepositoryImpl shelfDomainRepositoryImpl;
    ShelfMapper shelfMapper;

    private BookMapper(ShelfMapper shelfMapper, ShelfDomainRepositoryImpl shelfDomainRepositoryImpl){
        this.shelfMapper = shelfMapper;
        this.shelfDomainRepositoryImpl = shelfDomainRepositoryImpl;
    }

    public Book toDomain(BookEntity e,
                         Set<AuthorEntity> authorEntities,
                         ShelfEntity shelfEntity){

        HashSet<Author> authors = new HashSet<>();
//        Shelf shelf = shelfMapper.toDomain(shelfEntity);
        Shelf shelf = shelfDomainRepositoryImpl.getById(shelfEntity.getShelfId());

        for (AuthorEntity authorEntity : authorEntities) {
            authors.add(AuthorMapper.toDomain(authorEntity.getAuthorId(), authorEntity.getFirstName(), authorEntity.getLastName()));
        }

        if (e == null){
            return null;
        }

        Book book = new Book();
        book.setId(e.getBookId());
        book.setEdition(e.getEdition());
        book.setTitle(e.getTitle());
        book.setAuthors(authors);
        book.setIsbn(e.getIsbn());
        book.setPublisher(e.getPublisher());
        book.setPublicationYear(e.getPublicationYear());
//        book.setGenre(genre);
        book.setShelf(shelf);
        book.setDescription(e.getDescription());
        book.setAvailabilityStatus(e.getAvailabilityStatus() != null ? AvailabilityStatus.valueOf(e.getAvailabilityStatus()) : null);
        book.setCreatedAt(e.getCreatedAt());
        book.setUpdatedAt(e.getUpdatedAt());
        return book;
    }


    public static BookEntity toEntity(Book book){
        if (book == null){
            return null;
        }
        BookEntity bookEntity = new BookEntity();
        bookEntity.setBookId(book.getId());
        bookEntity.setTitle(book.getTitle());
        bookEntity.setIsbn(book.getIsbn());

        // Convert authors back to entities
        if (book.getAuthors() != null) {
            HashSet<AuthorEntity> authorEntities = new HashSet<>();
            for (Author author : book.getAuthors()) {
                AuthorEntity authorEntity = new AuthorEntity();
                authorEntity.setAuthorId(author.getAuthorId());
                authorEntity.setFirstName(author.getFirstName());
                authorEntity.setLastName(author.getLastName());
                authorEntities.add(authorEntity);
            }
            bookEntity.setAuthors(authorEntities);
        }

        bookEntity.setPublisher(book.getPublisher());
        bookEntity.setPublicationYear(book.getPublicationYear());
        bookEntity.setGenre(book.getGenre() !=null ? book.getGenre().getGenreName() : null);
        bookEntity.setEdition(book.getEdition());
        bookEntity.setDescription(book.getDescription());
        bookEntity.setShelfId(book.getShelf() != null ? book.getShelf().getId() : null);
        bookEntity.setAvailabilityStatus(book.getAvailabilityStatus() != null ? book.getAvailabilityStatus().name() : null);
        bookEntity.setCreatedAt(book.getCreatedAt());
        bookEntity.setUpdatedAt(book.getUpdatedAt());

        return bookEntity;
    }


}
