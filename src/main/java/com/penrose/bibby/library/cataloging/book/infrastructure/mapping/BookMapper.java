package com.penrose.bibby.library.cataloging.book.infrastructure.mapping;

import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.cataloging.author.core.domain.Author;
import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.cataloging.author.infrastructure.mapping.AuthorMapper;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookRequestDTO;
import com.penrose.bibby.library.cataloging.book.core.domain.*;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.book.core.domain.*;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Component
public class BookMapper {
    private final AuthorFacade authorFacade;
    Logger log = org.slf4j.LoggerFactory.getLogger(BookMapper.class);

    private BookMapper(AuthorFacade authorFacade){
        this.authorFacade = authorFacade;
    }

    public Book toDomain(BookDTO bookDTO,
                         Set<AuthorDTO> authorDTOs,
                         ShelfDTO shelfDTO){

        HashSet<Author> authors = new HashSet<>();

        for (AuthorDTO authorDTO : authorDTOs) {
            authors.add(AuthorMapper.toDomain(authorDTO.id(), authorDTO.firstName(), authorDTO.lastName()));
        }

        if (bookDTO == null){
            return null;
        }

        Book book = new Book();
        book.setBookId(new BookId(bookDTO.id()));
        book.setEdition(bookDTO.edition());
        book.setTitle(new Title(bookDTO.title()));
//        book.setAuthors(authors);
        book.setIsbn(new Isbn(bookDTO.isbn()));
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
                         Set<AuthorRef> authorRefs,
                         ShelfEntity shelfDTO){


        List<AuthorRef> authors = new ArrayList<>(authorRefs);

        if (e == null){
            return null;
        }

        Book book = new Book();
        book.setBookId(new BookId(e.getBookId()));
        book.setEdition(e.getEdition());
        book.setTitle(new Title(e.getTitle()));
        book.setAuthors(authors);
        book.setIsbn(new Isbn(e.getIsbn()));
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
        bookEntity.setBookId(book.getBookId().getId());
        bookEntity.setTitle(book.getTitle().title());
        bookEntity.setIsbn(book.getIsbn().isbn);

        // Convert authors back to entities
        if (book.getAuthors() != null) {
            HashSet<AuthorEntity> authorEntities = new HashSet<>();
            for (AuthorRef author : book.getAuthors()) {
                // Assuming author string is in "First Last" format
                String firstName = author.getAuthorFirstName();
                String lastName = author.getAuthorLastName();
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
        book.setBookId(new BookId(bookEntity.getBookId()));
        book.setEdition(bookEntity.getEdition());
        book.setTitle(new Title(bookEntity.getTitle()));
        book.setIsbn(new Isbn(bookEntity.getIsbn()));
        book.setPublisher(bookEntity.getPublisher());
        book.setPublicationYear(bookEntity.getPublicationYear());
        book.setShelfId(bookEntity.getShelfId());
        book.setDescription(bookEntity.getDescription());
        book.setAvailabilityStatus(bookEntity.getAvailabilityStatus() != null ? AvailabilityStatus.valueOf(bookEntity.getAvailabilityStatus()) : null);
        book.setCreatedAt(bookEntity.getCreatedAt());
        book.setUpdatedAt(bookEntity.getUpdatedAt());
        return book;

    }

    public Set<AuthorEntity> toEntitySetFromAuthorRefs(List<AuthorRef> authors) {
        Set<AuthorEntity> authorEntities = new HashSet<>();
        if (authors != null) {
            for (AuthorRef author : authors) {

                AuthorEntity authorEntity = new AuthorEntity();
                authorEntity.setAuthorId(author.getAuthorId());

                log.info("Looking up AuthorEntity for AuthorRef ID: " + author.getAuthorId());
                log.info("Fetched AuthorEntity from AuthorFacade: " + authorEntity.getFirstName() + " " + authorEntity.getLastName());
//                authorEntity.setFirstName(author.getAuthorFirstName());
//                authorEntity.setLastName(author.getAuthorLastName());
                log.info("Mapping AuthorRef to AuthorEntity: " + author.getAuthorFirstName() + " " + author.getAuthorLastName());
                authorEntities.add(authorEntity);
            }
        }
        return authorEntities;
    }

    public Book toDomainFromDTO(BookDTO bookDTO) {
        if (bookDTO == null){
            return null;
        }
        Book book = new Book();
        book.setBookId(new BookId(bookDTO.id()));
        book.setEdition(bookDTO.edition());
        book.setTitle(new Title(bookDTO.title()));
        book.setIsbn(new Isbn(bookDTO.isbn()));
        book.setPublisher(bookDTO.publisher());
        book.setPublicationYear(bookDTO.publicationYear());
        book.setShelfId(bookDTO.shelfId());
        book.setDescription(bookDTO.description());
        book.setAvailabilityStatus(bookDTO.availabilityStatus());
        book.setCreatedAt(bookDTO.createdAt());
        book.setUpdatedAt(bookDTO.updatedAt());
        return book;
    }

    public Book toDomainFromBookRequestDTO(BookRequestDTO bookRequestDTO) {
        if (bookRequestDTO == null){
            return null;
        }
        List<AuthorRef> authors = new ArrayList<>();
        for(AuthorDTO author : bookRequestDTO.authors()){
            String firstName = author.firstName();
            String lastName = author.lastName();
            Long id = author.id();
            authors.add(new AuthorRef(id,new AuthorName(firstName, lastName)));
        }
        Book book = new Book();
        book.setTitle(new Title(bookRequestDTO.title()));
        book.setIsbn(new Isbn(bookRequestDTO.isbn()));
        book.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        book.setAuthors(authors);
        log.info("Mapped BookRequestDTO to Book domain with title: " + book.getTitle().title());
        return book;
    }

    public BookMetaDataResponse toBookMetaDataResponseFromGoogleBooksResponse(GoogleBooksResponse googleBooksResponse, String isbn) {
       if(googleBooksResponse == null || googleBooksResponse.items() == null || googleBooksResponse.items().isEmpty()){
           throw new RuntimeException("No book found for ISBN: " + isbn);
       }

        List<String> authors = new ArrayList<>(googleBooksResponse.items().get(0).volumeInfo().authors());
        log.info("""
                
                Mapped GoogleBooksResponse to BookMetaDataResponse for ISBN: {}
                Book Title: {}
                Authors: {}
               
                """,
                isbn,
                googleBooksResponse.items().get(0).volumeInfo().title(),
                String.join(", ", authors)
        );

        return new BookMetaDataResponse(
                null,
                googleBooksResponse.items().get(0).volumeInfo().title(),
                isbn,
                authors,
                googleBooksResponse.items().get(0).volumeInfo().publisher(),
                googleBooksResponse.items().get(0).volumeInfo().description()
        );
    }

    public BookEntity toEntityFromBookMetaDataResponse(BookMetaDataResponse bookMetaDataResponse, String isbn, Long shelfId) {
        if (bookMetaDataResponse == null){
            return null;
        }

        authorFacade.createAuthorsIfNotExist(bookMetaDataResponse.authors());

        BookEntity bookEntity = new BookEntity();
        bookEntity.setTitle(bookMetaDataResponse.title());
        bookEntity.setIsbn(isbn);
        log.info("Fetching AuthorEntities for authors: " + String.join(", ", bookMetaDataResponse.authors()));
        bookEntity.setAuthors(authorFacade.getAuthorsById(bookMetaDataResponse.authors()));
        bookEntity.setPublisher(bookMetaDataResponse.publisher());
        bookEntity.setDescription(bookMetaDataResponse.description());
        bookEntity.setShelfId(shelfId);
        log.info("Setting availability status to AVAILABLE for book: " + bookMetaDataResponse.title());
        bookEntity.setAvailabilityStatus(AvailabilityStatus.AVAILABLE.name());
        bookEntity.setCreatedAt(java.time.LocalDate.now());
        bookEntity.setUpdatedAt(java.time.LocalDate.now());
        return bookEntity;
    }

    public List<BookDTO> toDTOListFromEntityList(List<BookEntity> bookEntities) {
        List<BookDTO> bookDTOs = new ArrayList<>();
        for (BookEntity bookEntity : bookEntities) {
            bookDTOs.add(toDTOfromEntity(bookEntity));
        }
        return bookDTOs;
    }
}
