package com.penrose.bibby.library.book;

import com.penrose.bibby.library.Author.Author;
import com.penrose.bibby.library.genre.Genre;
import com.penrose.bibby.library.shelf.Shelf;

public class BookMapper {

    public static Book toDomain(BookEntity e, Author author, Shelf shelf, Genre genre){
        if (e == null){
            return null;
        }
        Book book = new Book();
        book.setId(e.getId());
        book.setEdition(e.getEdition());
        book.setTitle(e.getTitle());
        book.setAuthor(author);
        book.setIsbn(e.getIsbn());
        book.setPublisher(e.getPublisher());
        book.setPublicationYear(e.getPublicationYear());
        book.setGenre(genre);
        book.setShelf(shelf);
        book.setDescription(e.getDescription());
        book.setCheckoutCount(e.getCheckoutCount());
        book.setStatus(e.getBookStatus() != null ? BookStatus.valueOf(e.getBookStatus()) : null);
        book.setCreatedAt(e.getCreatedAt());
        book.setUpdatedAt(e.getUpdatedAt());
        return book;
    }


    public static BookEntity toEntity(Book book){
        if (book == null){
            return null;
        }
        BookEntity bookEntity = new BookEntity();
        bookEntity.setId(book.getId());
        bookEntity.setTitle(book.getTitle());
        bookEntity.setIsbn(book.getIsbn());
        bookEntity.setPublisher(book.getPublisher());
        bookEntity.setPublicationYear(book.getPublicationYear());
        bookEntity.setGenre(book.getGenre() !=null ? book.getGenre().getGenreName() : null);
        bookEntity.setEdition(book.getEdition());
        bookEntity.setDescription(book.getDescription());
        bookEntity.setAuthorId(book.getAuthor() != null ? book.getAuthor().getId() : null);
        bookEntity.setShelfId(book.getShelf() != null ? book.getShelf().getId() : null);
//        bookEntity.setCheckedOut(book.isCheckedOut());
        bookEntity.setCheckoutCount(book.getCheckoutCount());
        bookEntity.setBookStatus(book.getStatus() != null ? book.getStatus().name() : null);
        bookEntity.setCreatedAt(book.getCreatedAt());
        bookEntity.setUpdatedAt(book.getUpdatedAt());

        return bookEntity;
    }


}
