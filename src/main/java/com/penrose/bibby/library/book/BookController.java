package com.penrose.bibby.library.book;

import com.penrose.bibby.library.Author.Author;
import com.penrose.bibby.library.Author.AuthorRepository;
import org.springframework.stereotype.Controller;
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
    public void addBook(@RequestBody BookRequestDTO requestDTO) {
        bookService.addBook(requestDTO.title(),requestDTO.firstName(),requestDTO.lastName());
    }




}
