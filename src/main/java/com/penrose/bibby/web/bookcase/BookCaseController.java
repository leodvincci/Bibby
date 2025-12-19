package com.penrose.bibby.web.bookcase;

import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.core.application.BookcaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookCaseController {

    BookcaseService bookCaseService;

    public BookCaseController(BookcaseService bookCaseService) {
        this.bookCaseService = bookCaseService;
    }

    @PostMapping("/create/bookcase")
    public ResponseEntity<String> createBookCase(@RequestBody BookcaseDTO bookcaseDTO){
        String message = bookCaseService.createNewBookCase(bookcaseDTO.bookcaseLabel(), "", "", bookcaseDTO.shelfCapacity(), bookcaseDTO.bookCapacity(),"");
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

}
