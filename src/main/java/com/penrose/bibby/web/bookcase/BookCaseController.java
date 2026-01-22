package com.penrose.bibby.web.bookcase;

import com.penrose.bibby.library.stacks.bookcase.contracts.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.CreateBookcaseRequest;
import com.penrose.bibby.library.stacks.bookcase.core.application.BookcaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookcase")
public class BookCaseController {
  Logger logger = LoggerFactory.getLogger(BookCaseController.class);
  BookcaseService bookCaseService;

  public BookCaseController(BookcaseService bookCaseService) {
    this.bookCaseService = bookCaseService;
  }

  @PostMapping("/create")
  public ResponseEntity<CreateBookcaseResult> createBookCase(
      @RequestBody CreateBookcaseRequest createBookcaseRequest) {
    logger.info(
        "Received request to create bookcase at location: {}", createBookcaseRequest.location());
    CreateBookcaseResult createBookcaseResult =
        bookCaseService.createNewBookCase(
            null,
            createBookcaseRequest.zone(),
            createBookcaseRequest.indexId(),
            createBookcaseRequest.shelfCount(),
            createBookcaseRequest.shelfCapacity(),
            createBookcaseRequest.location());

    return ResponseEntity.status(HttpStatus.CREATED).body(createBookcaseResult);
  }
}
