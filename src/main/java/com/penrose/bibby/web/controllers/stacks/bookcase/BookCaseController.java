package com.penrose.bibby.web.controllers.stacks.bookcase;

import com.penrose.bibby.library.registration.infrastructure.AppUserImpl;
import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.api.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.api.dtos.CreateBookcaseRequest;
import com.penrose.bibby.library.stacks.bookcase.api.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.bookcase.core.application.BookcaseService;
import com.penrose.bibby.library.stacks.shelf.api.ports.inbound.ShelfFacade;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookcase")
public class BookCaseController {

  private final BookcaseFacade bookcaseFacade;
  private final ShelfFacade bookshelfFacade;
  Logger logger = LoggerFactory.getLogger(BookCaseController.class);
  BookcaseService bookCaseService;

  public BookCaseController(
      BookcaseService bookCaseService, BookcaseFacade bookcaseFacade, ShelfFacade bookshelfFacade) {
    this.bookCaseService = bookCaseService;
    this.bookcaseFacade = bookcaseFacade;
    this.bookshelfFacade = bookshelfFacade;
  }

  @PostMapping("/create")
  public ResponseEntity<CreateBookcaseResult> createBookCase(
      @AuthenticationPrincipal AppUserImpl principal,
      @RequestBody CreateBookcaseRequest createBookcaseRequest) {
    logger.info(
        "Received request to create bookcase at location: {}", createBookcaseRequest.location());
    CreateBookcaseResult createBookcaseResult =
        bookCaseService.createNewBookCase(
            principal.getAppUserId(),
            createBookcaseRequest.zone() + "-" + createBookcaseRequest.indexId(),
            createBookcaseRequest.zone(),
            createBookcaseRequest.indexId(),
            createBookcaseRequest.shelfCount(),
            createBookcaseRequest.shelfCapacity(),
            createBookcaseRequest.location());

    return ResponseEntity.status(HttpStatus.CREATED).body(createBookcaseResult);
  }

  @DeleteMapping("/delete/{bookcaseId}")
  public ResponseEntity<HttpStatus> deleteBookcase(@PathVariable Long bookcaseId) {
    bookshelfFacade.deleteAllShelvesInBookcase(bookcaseId);
    bookcaseFacade.deleteBookcase(bookcaseId);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("/locations")
  public ResponseEntity<Set<String>> getAllBookcaseLocations() {
    logger.info("Received request to get all bookcase locations");
    Set<String> locations = Set.copyOf(bookCaseService.getAllBookcaseLocations());
    return ResponseEntity.ok(locations);
  }

  @GetMapping("/location/{location}")
  public ResponseEntity<List<BookcaseDTO>> getBookcaseByLocation(@PathVariable String location) {
    logger.info("Received request to get bookcase at location: {}", location);
    List<BookcaseDTO> bookcases = bookCaseService.getAllBookcasesByLocation(location);
    return ResponseEntity.ok(bookcases);
  }

  @GetMapping("/all")
  public ResponseEntity<List<BookcaseDTO>> getAllBookcases(
      @AuthenticationPrincipal AppUserImpl principal) {

    logger.info(
        "Received request to get all bookcases for user with ID: {}", principal.getAppUserId());
    List<BookcaseDTO> bookcases = bookcaseFacade.getAllBookcasesByUserId(principal.getAppUserId());
    return ResponseEntity.ok(bookcases);
  }
}
