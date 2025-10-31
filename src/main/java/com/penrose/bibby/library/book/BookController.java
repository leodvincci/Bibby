package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class BookController {

    final BookService bookService;
    final AuthorRepository authorRepository;

    public BookController(BookService bookService, AuthorRepository authorRepository, BookRepository bookRepository){
        this.bookService = bookService;
        this.authorRepository = authorRepository;
    }

    @PostMapping("api/v1/books")
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
        bookService.createNewBook(requestDTO);
        return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
    }

    @GetMapping("api/v1/books")
    public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
        System.out.println("Controller Search For " + requestDTO.title());
        bookService.findBookByTitle(requestDTO.title());
    }




}
