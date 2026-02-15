package com.penrose.bibby.library.cataloging.book.core.application;

import com.penrose.bibby.library.cataloging.author.api.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.api.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.author.core.domain.AuthorFactory;
import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
import com.penrose.bibby.library.cataloging.book.core.domain.AvailabilityStatus;
import com.penrose.bibby.library.cataloging.book.core.domain.Book;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.cataloging.book.infrastructure.mapping.BookMapperTwo;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IsbnEnrichmentService {

  private static final Logger log = LoggerFactory.getLogger(IsbnEnrichmentService.class);
  AuthorFactory authorFactory;
  BookMapperTwo bookMapper;
  AuthorFacade authorFacade;

  public IsbnEnrichmentService(
      AuthorFactory authorFactory, BookMapperTwo bookMapper, AuthorFacade authorFacade) {
    this.authorFactory = authorFactory;
    this.bookMapper = bookMapper;
    this.authorFacade = authorFacade;
  }

  public BookEntity enrichIsbn(GoogleBooksResponse googleBooksResponse, String isbn) {
    if (googleBooksResponse == null
        || googleBooksResponse.items() == null
        || googleBooksResponse.items().isEmpty()) {
      throw new IllegalArgumentException("No book data found for ISBN: " + isbn);
    }

    Book book = bookMapper.toDomainFromJSON(googleBooksResponse);
    var pretty =
        """
        -------------------------------------------
        📚  Enriched Book Details
        -------------------------------------------
        Title       : %s
        Author(s)   : %s
        Publisher   : %s
        ISBN-13     : %s

        Description :
        %s
        -------------------------------------------
        """
            .formatted(
                book.getTitle(),
                book.getAuthors().stream()
                    .map(a -> a.getAuthorFirstName() + " " + a.getAuthorLastName())
                    .collect(Collectors.joining(", ")),
                book.getPublisher(),
                isbn,
                wrapText(book.getDescription(), 80));

    log.info("\n{}", pretty);
    HashSet<AuthorEntity> authors = new HashSet<>();
    for (AuthorRef author : book.getAuthors()) {
      AuthorDTO authorDTO =
          new AuthorDTO(null, author.getAuthorFirstName(), author.getAuthorLastName());
      authorFacade.updateAuthor(authorDTO);
      authors.add(AuthorDTO.AuthorDTOtoEntity(authorDTO));
    }

    BookEntity bookEntity = new BookEntity();
    bookEntity.setAuthors(authors);
    bookEntity.setIsbn(isbn);
    bookEntity.setAvailabilityStatus(String.valueOf(AvailabilityStatus.AVAILABLE));
    bookEntity.setCreatedAt(LocalDate.now());
    bookEntity.setTitle(book.getTitle().title());
    bookEntity.setDescription(book.getDescription());
    bookEntity.setPublisher(book.getPublisher());
    //
    // bookEntity.setPublicationYear(Integer.parseInt(book.getPublishedDate().split("-")[0]));
    System.out.println("book = " + bookEntity);
    //        bookRepository.save(bookEntity);
    return bookEntity;
  }

  private String wrapText(String text, int width) {
    if (text == null) {
      return ""; // or null, or some placeholder
    }

    return text.replaceAll("(.{1," + width + "})(\\s+|$)", "$1\n");
  }
}
