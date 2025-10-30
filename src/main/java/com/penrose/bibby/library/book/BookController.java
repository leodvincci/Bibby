package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController {

    final BookService bookService;
    final AuthorRepository authorRepository;

    public BookController(BookService bookService, AuthorRepository authorRepository){
        this.bookService = bookService;
        this.authorRepository = authorRepository;
    }

    @PostMapping("/books")
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
        bookService.createNewBook(requestDTO);
        return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
    }




}
