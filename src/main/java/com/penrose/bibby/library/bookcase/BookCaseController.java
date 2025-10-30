package com.penrose.bibby.library.bookcase;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookCaseController {

    BookcaseService bookCaseService;

    @PostMapping("/create/bookcase")
    public void createNewBookCase(@RequestBody BookcaseDTO bookcaseDTO){
        bookCaseService.createNewBookCase(bookcaseDTO);
        System.out.println("Controller: Creating new bookcase!");
    }



}
