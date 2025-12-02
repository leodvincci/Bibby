package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorRepository;
import com.penrose.bibby.library.book.dto.BookRequestDTO;
import com.penrose.bibby.library.book.repository.BookRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class BookController {

    final BookService bookService;
    final AuthorRepository authorRepository;
    final BookInfoService bookInfoService;

    public BookController(BookService bookService, AuthorRepository authorRepository, BookRepository bookRepository, BookInfoService bookInfoService){
        this.bookService = bookService;
        this.authorRepository = authorRepository;
        this.bookInfoService = bookInfoService;
    }

    @PostMapping("api/v1/books")
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
        bookService.createNewBook(requestDTO);
        return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
    }

    @GetMapping("/lookup/{isbn}")
    public Mono<String> getBookInfo(@PathVariable String isbn){
        System.out.println("Controller Lookup For " + isbn);
        return bookInfoService.lookupBook(isbn).doOnNext( body -> System.out.println(body));
    }

    @GetMapping("api/v1/books")
    public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
        System.out.println("Controller Search For " + requestDTO.title());
        bookService.findBookByTitle(requestDTO.title());
    }




}
