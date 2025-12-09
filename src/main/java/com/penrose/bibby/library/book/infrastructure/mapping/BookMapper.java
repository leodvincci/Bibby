package com.penrose.bibby.library.book.infrastructure.mapping;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.author.core.domain.Author;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.author.infrastructure.mapping.AuthorMapper;
import com.penrose.bibby.library.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.book.core.domain.AvailabilityStatus;
import com.penrose.bibby.library.book.core.domain.Book;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.shelf.contracts.ShelfDTO;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.shelf.infrastructure.mapping.ShelfMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Component
public class BookMapper {
    ShelfMapper shelfMapper;

    private BookMapper(ShelfMapper shelfMapper){
        this.shelfMapper = shelfMapper;
    }

    public Book toDomain(BookDTO bookDTO,
                         Set<AuthorDTO> authorDTOs,
                         ShelfDTO shelfDTO){

        HashSet<Author> authors = new HashSet<>();
//        Shelf shelf = shelfMapper.toDomain(shelfEntity);

        for (AuthorDTO authorDTO : authorDTOs) {
            authors.add(AuthorMapper.toDomain(authorDTO.id(), authorDTO.firstName(), authorDTO.lastName()));
        }

        if (bookDTO == null){
            return null;
        }

        Book book = new Book();
        book.setId(bookDTO.id());
        book.setEdition(bookDTO.edition());
        book.setTitle(bookDTO.title());
//        book.setAuthors(authors);
        book.setIsbn(bookDTO.isbn());
        book.setPublisher(bookDTO.publisher());
        book.setPublicationYear(bookDTO.publicationYear());
//        book.setGenre(genre);
        book.setShelfId(shelfDTO.shelfId());
        book.setDescription(bookDTO.description());
        book.setAvailabilityStatus(bookDTO.availabilityStatus());
        book.setCreatedAt(bookDTO.createdAt());
        book.setUpdatedAt(bookDTO.updatedAt());
        return book;
    }


    public Book toDomain(BookEntity e,
                         Set<AuthorDTO> authorDTOs,
                         ShelfEntity shelfDTO){

        List<String> authors = new ArrayList<>();
//        Shelf shelf = shelfMapper.toDomain(shelfEntity);

        for (AuthorDTO authorDTO : authorDTOs) {
            authors.add(authorDTO.firstName() + " " + authorDTO.lastName());
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
        book.setShelfId(shelfDTO.getShelfId());
        book.setDescription(e.getDescription());
        book.setAvailabilityStatus(e.getAvailabilityStatus() != null ? AvailabilityStatus.valueOf(e.getAvailabilityStatus()) : null);
        book.setCreatedAt(e.getCreatedAt());
        book.setUpdatedAt(e.getUpdatedAt());
        return book;
    }


//    public Book toDomain(BookEntity e,
//                         Set<AuthorEntity> authorEntities,
//                         ShelfEntity shelfDTO){
//
//        HashSet<Author> authors = new HashSet<>();
////        Shelf shelf = shelfMapper.toDomain(shelfEntity);
//        Shelf shelf = shelfDomainRepositoryImpl.getById(shelfDTO.getShelfId());
//
//        for (AuthorEntity authorEntity : authorEntities) {
//            authors.add(AuthorMapper.toDomain(authorEntity.getAuthorId(),authorEntity.getFirstName(), authorEntity.getLastName()));
//        }
//
//        if (e == null){
//            return null;
//        }
//
//        Book book = new Book();
//        book.setId(e.getBookId());
//        book.setEdition(e.getEdition());
//        book.setTitle(e.getTitle());
//        book.setAuthors(authors);
//        book.setIsbn(e.getIsbn());
//        book.setPublisher(e.getPublisher());
//        book.setPublicationYear(e.getPublicationYear());
////        book.setGenre(genre);
//        book.setShelfId(shelf.getId());
//        book.setDescription(e.getDescription());
//        book.setAvailabilityStatus(e.getAvailabilityStatus() != null ? AvailabilityStatus.valueOf(e.getAvailabilityStatus()) : null);
//        book.setCreatedAt(e.getCreatedAt());
//        book.setUpdatedAt(e.getUpdatedAt());
//        return book;
//    }



    public BookEntity toEntity(Book book){
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
            for (String author : book.getAuthors()) {
                // Assuming author string is in "First Last" format
                String[] names = author.split(" ");
                String firstName = names[0];
                String lastName = names.length > 1 ? names[1] : "";
                authorEntities.add(new AuthorEntity(firstName, lastName));
            }
            bookEntity.setAuthors(authorEntities);
        }

        bookEntity.setPublisher(book.getPublisher());
        bookEntity.setPublicationYear(book.getPublicationYear());
//        bookEntity.setGenre(book.getGenre() !=null ? book.getGenre().getGenreName() : null);
        bookEntity.setEdition(book.getEdition());
        bookEntity.setDescription(book.getDescription());
        bookEntity.setShelfId(book.getShelfId());
        bookEntity.setAvailabilityStatus(book.getAvailabilityStatus() != null ? book.getAvailabilityStatus().name() : null);
        bookEntity.setCreatedAt(book.getCreatedAt());
        bookEntity.setUpdatedAt(book.getUpdatedAt());

        return bookEntity;
    }

    public BookEntity toEntityFromDTO(BookDTO bookDTO, Set<AuthorEntity> authorEntities){
        if (bookDTO == null){
            return null;
        }
        BookEntity bookEntity = new BookEntity();
        bookEntity.setBookId(bookDTO.id());
        bookEntity.setTitle(bookDTO.title());
        bookEntity.setIsbn(bookDTO.isbn());

        // Convert authors back to entities
        if (authorEntities != null) {
//            HashSet<AuthorEntity> authorEntities = new HashSet<>();
//            for (Author author : book.getAuthors()) {
//                AuthorEntity authorEntity = new AuthorEntity();
//                authorEntity.setAuthorId(author.getAuthorId());
//                authorEntity.setFirstName(author.getFirstName());
//                authorEntity.setLastName(author.getLastName());
//                authorEntities.add(authorEntity);
//            }
            bookEntity.setAuthors(authorEntities);
        }

        bookEntity.setPublisher(bookDTO.publisher());
        bookEntity.setPublicationYear(bookDTO.publicationYear());
//        bookEntity.setGenre(book.getGenre() !=null ? book.getGenre().getGenreName() : null);
        bookEntity.setEdition(bookDTO.edition());
        bookEntity.setDescription(bookDTO.description());
        bookEntity.setShelfId(bookDTO.shelfId());
        bookEntity.setAvailabilityStatus(bookDTO.availabilityStatus().toString());
        bookEntity.setCreatedAt(bookDTO.createdAt());
        bookEntity.setUpdatedAt(bookDTO.updatedAt());

        return bookEntity;
    }


    public BookDTO toDTOfromEntity(BookEntity bookEntity) {

        return new BookDTO(
                bookEntity.getBookId(),
                bookEntity.getEdition(),
                bookEntity.getTitle(),
                null,
                bookEntity.getIsbn(),
                null,
                bookEntity.getPublisher(),
                bookEntity.getPublicationYear(),
                bookEntity.getShelfId(),
                bookEntity.getDescription(),
                bookEntity.getAvailabilityStatus() != null ? AvailabilityStatus.valueOf(bookEntity.getAvailabilityStatus()) : null,
                bookEntity.getCreatedAt(),
                bookEntity.getUpdatedAt(),
                null
        );
    }


    public Book toDomainFromEntity(BookEntity bookEntity) {
        if (bookEntity == null){
            return null;
        }
        Book book = new Book();
        book.setId(bookEntity.getBookId());
        book.setEdition(bookEntity.getEdition());
        book.setTitle(bookEntity.getTitle());
        book.setIsbn(bookEntity.getIsbn());
        book.setPublisher(bookEntity.getPublisher());
        book.setPublicationYear(bookEntity.getPublicationYear());
        book.setShelfId(bookEntity.getShelfId());
        book.setDescription(bookEntity.getDescription());
        book.setAvailabilityStatus(bookEntity.getAvailabilityStatus() != null ? AvailabilityStatus.valueOf(bookEntity.getAvailabilityStatus()) : null);
        book.setCreatedAt(bookEntity.getCreatedAt());
        book.setUpdatedAt(bookEntity.getUpdatedAt());
        return book;

    }
}
