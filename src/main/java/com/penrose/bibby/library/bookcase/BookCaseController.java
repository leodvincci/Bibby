package com.penrose.bibby.library.bookcase;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookCaseController {

    BookcaseService bookCaseService;

    @PostMapping("/create/shelf")
    public void createNewBookCase(RequestBody bookcaseLabel){
        bookCaseService.createNewBookCase(bookcaseLabel);
        System.out.println("Controller: Creating new Shelf!");
    }



}
