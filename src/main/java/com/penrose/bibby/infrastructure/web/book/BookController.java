package com.penrose.bibby.infrastructure.web.book;

import com.penrose.bibby.library.author.infrastructure.repository.AuthorRepository;
import com.penrose.bibby.library.book.application.IsbnLookupService;
import com.penrose.bibby.library.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.book.contracts.BookPlacementResponse;
import com.penrose.bibby.library.book.contracts.BookRequestDTO;
import com.penrose.bibby.library.book.contracts.BookShelfAssignmentRequest;
import com.penrose.bibby.library.book.application.IsbnEnrichmentService;
import com.penrose.bibby.library.book.application.BookService;
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
import com.penrose.bibby.library.bookcase.application.BookcaseService;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.shelf.application.ShelfService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BookController {

    final BookService bookService;
    final AuthorRepository authorRepository;
    final IsbnLookupService isbnLookupService;
    private final IsbnEnrichmentService isbnEnrichmentService;
    private final ShelfService shelfService;
    private final BookcaseService bookcaseService;

    public BookController(BookService bookService, AuthorRepository authorRepository, IsbnLookupService isbnLookupService, IsbnEnrichmentService isbnEnrichmentService, ShelfService shelfService, BookcaseService bookcaseService){
        this.bookService = bookService;
        this.authorRepository = authorRepository;
        this.isbnLookupService = isbnLookupService;
        this.isbnEnrichmentService = isbnEnrichmentService;
        this.shelfService = shelfService;
        this.bookcaseService = bookcaseService;
    }

    @PostMapping("api/v1/books")
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
        bookService.createNewBook(requestDTO);
        return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
    }

    @GetMapping("/lookup/{isbn}")
    public Mono<GoogleBooksResponse> getBookInfo(@PathVariable String isbn){
        System.out.println("Controller Lookup For " + isbn);
        return isbnLookupService.lookupBook(isbn).doOnNext(body ->{
            System.out.println("Received response for ISBN " + isbn + ": " + body);
        });
    }

    @GetMapping("api/v1/books")
    public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
        System.out.println("Controller Search For " + requestDTO.title());
        bookService.findBookByTitle(requestDTO.title());
    }

    @PostMapping("api/v1/books/{bookId}/shelf")
    public ResponseEntity<BookPlacementResponse> placeBookOnShelf(@PathVariable Long bookId, @RequestBody BookShelfAssignmentRequest request) {
        if (request == null || request.shelfId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shelf id is required");
        }

        BookEntity updatedBook;
        try {
            updatedBook = bookService.assignBookToShelf(bookId, request.shelfId());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        ShelfEntity shelf = shelfService.findShelfById(request.shelfId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shelf not found"));
        BookcaseEntity bookcase = bookcaseService.findBookCaseById(shelf.getBookcaseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bookcase not found"));

        BookPlacementResponse response = new BookPlacementResponse(
                updatedBook.getBookId(),
                updatedBook.getTitle(),
                shelf.getShelfId(),
                shelf.getShelfLabel(),
                bookcase.getBookcaseLabel()
        );

        return ResponseEntity.ok(response);
    }
}
