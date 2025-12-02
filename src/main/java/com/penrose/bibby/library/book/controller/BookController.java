package com.penrose.bibby.library.book.controller;

import com.penrose.bibby.library.author.AuthorRepository;
import com.penrose.bibby.library.book.domain.GoogleBooksResponse;
import com.penrose.bibby.library.book.domain.BookEntity;
import com.penrose.bibby.library.book.dto.BookPlacementResponse;
import com.penrose.bibby.library.book.dto.BookRequestDTO;
import com.penrose.bibby.library.book.dto.BookShelfAssignmentRequest;
import com.penrose.bibby.library.book.service.IsbnEnrichmentService;
import com.penrose.bibby.library.book.service.BookInfoService;
import com.penrose.bibby.library.book.service.BookService;
import com.penrose.bibby.library.bookcase.BookcaseEntity;
import com.penrose.bibby.library.bookcase.BookcaseService;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BookController {

    final BookService bookService;
    final AuthorRepository authorRepository;
    final BookInfoService bookInfoService;
    private final IsbnEnrichmentService isbnEnrichmentService;
    private final ShelfService shelfService;
    private final BookcaseService bookcaseService;

    public BookController(BookService bookService, AuthorRepository authorRepository, BookInfoService bookInfoService, IsbnEnrichmentService isbnEnrichmentService, ShelfService shelfService, BookcaseService bookcaseService){
        this.bookService = bookService;
        this.authorRepository = authorRepository;
        this.bookInfoService = bookInfoService;
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
        return bookInfoService.lookupBook(isbn).doOnNext(body ->{
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
