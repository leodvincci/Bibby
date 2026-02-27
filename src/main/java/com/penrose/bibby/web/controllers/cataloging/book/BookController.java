package com.penrose.bibby.web.controllers.cataloging.book;

import com.penrose.bibby.cli.ui.BookcardRenderer;
import com.penrose.bibby.library.cataloging.author.api.dtos.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.core.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookLocationResponse;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookRequestDTO;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookShelfAssignmentRequest;
import com.penrose.bibby.library.cataloging.book.core.application.BookService;
import com.penrose.bibby.library.cataloging.book.core.application.IsbnLookupService;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.cataloging.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(BookController.class);
  final BookService bookService;
  final BookFacade bookFacade;
  final IsbnLookupService isbnLookupService;
  private final AuthorFacade authorFacade;
  private final ShelfFacade shelfFacade;

  public BookController(
      BookService bookService,
      BookFacade bookFacade,
      IsbnLookupService isbnLookupService,
      AuthorFacade authorFacade,
      ShelfFacade shelfFacade) {
    this.bookService = bookService;
    this.bookFacade = bookFacade;
    this.isbnLookupService = isbnLookupService;
    this.authorFacade = authorFacade;
    this.shelfFacade = shelfFacade;
  }

  //  // todo: remove commented code after testing
  //  @PostMapping("/")
  //  public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
  //    bookFacade.createNewBook(requestDTO);
  //    return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
  //  }

  @GetMapping("/lookup/{isbn}")
  public Mono<GoogleBooksResponse> getBookInfo(@PathVariable String isbn) {
    System.out.println("Controller Lookup For " + isbn);
    return isbnLookupService
        .lookupBook(isbn)
        .doOnNext(
            body -> {
              System.out.println("Received response for ISBN " + isbn + ": " + body);
            });
  }

  @GetMapping("/findBookByTitle")
  public void findBookByTitle(@RequestBody BookRequestDTO requestDTO) {
    System.out.println("Controller Search For " + requestDTO.title());
    BookDTO bookDTO = bookService.findBookByTitle(requestDTO.title());
    System.out.println(bookDTO);
    BookcardRenderer bookcardRenderer = new BookcardRenderer();
    System.out.println(
        bookcardRenderer.bookImportCard(
            bookDTO.title(), bookDTO.isbn(), bookDTO.authors().toString(), bookDTO.publisher()));
  }

  @CrossOrigin(origins = "*")
  @GetMapping("/search/{isbn}")
  public ResponseEntity<BookDTO> findBookByIsbn(@PathVariable String isbn) {
    System.out.println("Controller Search For " + isbn);
    System.out.println("Now searching for ISBN in database...");
    BookDTO bookDTO = bookService.findBookByIsbn(isbn);
    System.out.println(bookDTO);

    if (bookDTO == null) {
      System.out.println("Book not found in database. Returning 404 response.");
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    BookcardRenderer bookcardRenderer = new BookcardRenderer();
    System.out.println(
        bookcardRenderer.bookImportCard(
            bookDTO.title(), bookDTO.isbn(), bookDTO.authors().toString(), bookDTO.publisher()));
    System.out.println("Returning book data as response...");
    System.out.println();
    return ResponseEntity.ok(bookDTO);
  }

  @PostMapping("/{bookId}/shelf")
  public ResponseEntity<HttpStatus> placeBookOnShelf(
      @PathVariable Long bookId, @RequestBody BookShelfAssignmentRequest shelfAssignmentRequest) {
    bookFacade.placeBookOnShelf(bookId, shelfAssignmentRequest);
    return ResponseEntity.ok(HttpStatus.OK);
  }

  @CrossOrigin(origins = "*")
  @PostMapping("/addnewbook")
  public ResponseEntity<Map<String, Object>> addNewBook(@RequestBody BookDTO bookDTO) {
    log.info("Received BookDTO: {}", bookDTO);
    List<AuthorDTO> authorDTOS = new ArrayList<>();
    for (String author : bookDTO.authors()) {
      System.out.println(author);
      AuthorDTO authorDTO = new AuthorDTO(null, author.split(" ")[0], author.split(" ")[1]);
      authorDTOS.add(authorFacade.saveAuthor(authorDTO));
    }

    BookRequestDTO bookRequestDTO =
        new BookRequestDTO(
            bookDTO.title(), bookDTO.isbn(), authorDTOS, bookDTO.shelfId(), bookDTO.publisher());
    bookFacade.createNewBook(bookRequestDTO);
    shelfFacade.placeBookOnShelf(
        bookFacade.findBookByIsbn(bookRequestDTO.isbn()).id(), bookDTO.shelfId());

    System.out.println("Book added");
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "Book Added", "title", bookDTO.title(), "isbn", bookDTO.isbn()));
  }

  @GetMapping("/shelf/{shelfId}")
  public ResponseEntity<List<BookDTO>> getBooksByShelf(@PathVariable Long shelfId) {
    List<BookDTO> books = bookService.getBooksByShelfId(shelfId);
    return ResponseEntity.ok(books);
  }

  @GetMapping("/booklocation")
  public ResponseEntity<BookLocationResponse> getBookLocation(@RequestParam Long bookId) {
    BookLocationResponse response = bookService.getBookLocation(bookId);
    if (response == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
    return ResponseEntity.ok(response);
  }
}
