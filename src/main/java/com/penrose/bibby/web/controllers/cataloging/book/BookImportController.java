package com.penrose.bibby.web.controllers.cataloging.book;

import com.penrose.bibby.library.cataloging.book.core.application.IsbnLookupService;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.BookImportRequest;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBookResponseBrief;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.ratelimit.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BookImportController {
  Logger log = org.slf4j.LoggerFactory.getLogger(BookImportController.class);

  private final IsbnLookupService isbnLookupService;
  private final RateLimitService rateLimitService;

  public BookImportController(
      IsbnLookupService isbnLookupService, RateLimitService rateLimitService) {
    this.isbnLookupService = isbnLookupService;
    this.rateLimitService = rateLimitService;
  }

  @PostMapping("/api/v1/books/fetchbookmetadata")
  public ResponseEntity<GoogleBookResponseBrief> importBook(
      @RequestBody BookImportRequest request, HttpServletRequest servletRequest) {
    if (request == null || request.isbn() == null || request.isbn().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ISBN is required");
    }
    String xff = servletRequest.getHeader("X-Forwarded-For");

    String clientIp;
    if (xff != null && !xff.isBlank()) {
      clientIp = xff.split(",")[0].trim();
    } else {
      clientIp = servletRequest.getRemoteAddr();
    }

    log.info("USER IP Address: {} ", clientIp);

    if (!rateLimitService.isAllowed(clientIp)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    }
    log.info("Import request for ISBN {}", request.isbn());

    GoogleBooksResponse lookupResponse = isbnLookupService.lookupBook(request.isbn()).block();
    if (lookupResponse == null
        || lookupResponse.items() == null
        || lookupResponse.items().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "No book found for ISBN " + request.isbn());
    }

    GoogleBookResponseBrief googleBookResponseBrief =
        new GoogleBookResponseBrief(
            request.isbn(),
            lookupResponse.items().get(0).volumeInfo().title(),
            lookupResponse.items().get(0).volumeInfo().authors(),
            lookupResponse.items().get(0).volumeInfo().publisher());
    log.info("Found book metadata for ISBN {}: {}", request.isbn(), googleBookResponseBrief);

    //        BookEntity savedBook = isbnEnrichmentService.enrichIsbn(lookupResponse,
    // request.isbn());
    //        List<String> authors = savedBook.getAuthors().stream()
    //                .map(author -> author.getFirstName() + " " + author.getLastName())
    //                .collect(Collectors.toList());
    //
    //        BookMetaDataResponse response = new BookMetaDataResponse(
    //                savedBook.getBookId(),
    //                savedBook.getTitle(),
    //                savedBook.getIsbn(),
    //                authors,
    //                savedBook.getPublisher(),
    //                Optional.ofNullable(savedBook.getDescription())
    //        );

    return ResponseEntity.ok(googleBookResponseBrief);
  }
}
