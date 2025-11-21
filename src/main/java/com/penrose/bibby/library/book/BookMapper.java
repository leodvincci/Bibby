package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorMapper;
import com.penrose.bibby.library.genre.Genre;
import com.penrose.bibby.library.shelf.Shelf;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfMapper;

import java.util.HashSet;
import java.util.List;

public class BookMapper {

    public static Book toDomain(BookEntity e, HashSet<AuthorEntity> authorEntities, ShelfEntity shelfEntity){

        HashSet<Author> authors = new HashSet<>();
        Shelf shelf = ShelfMapper.toDomain(shelfEntity);

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
        book.setCheckoutCount(e.getCheckoutCount());
        book.setAvailabilityStatus(e.getBookStatus() != null ? AvailabilityStatus.valueOf(e.getBookStatus()) : null);
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
        bookEntity.setPublisher(book.getPublisher());
        bookEntity.setPublicationYear(book.getPublicationYear());
        bookEntity.setGenre(book.getGenre() !=null ? book.getGenre().getGenreName() : null);
        bookEntity.setEdition(book.getEdition());
        bookEntity.setDescription(book.getDescription());
        bookEntity.setShelfId(book.getShelf() != null ? book.getShelf().getId() : null);
        bookEntity.setCheckoutCount(book.getCheckoutCount());
        bookEntity.setBookStatus(book.getAvailabilityStatus() != null ? book.getAvailabilityStatus().name() : null);
        bookEntity.setCreatedAt(book.getCreatedAt());
        bookEntity.setUpdatedAt(book.getUpdatedAt());

        return bookEntity;
    }


}
