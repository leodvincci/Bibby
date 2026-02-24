package com.penrose.bibby.web.controllers.stacks.bookcase;

import com.penrose.bibby.identity.infrastructure.AppUserImpl;
import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.api.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.api.dtos.CreateBookcaseRequest;
import com.penrose.bibby.library.stacks.bookcase.core.ports.inbound.BookcaseFacade;
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
  Logger logger = LoggerFactory.getLogger(BookCaseController.class);

  public BookCaseController(BookcaseFacade bookcaseFacade) {
    this.bookcaseFacade = bookcaseFacade;
  }

  @PostMapping("/create")
  public ResponseEntity<CreateBookcaseResult> createBookCase(
      @AuthenticationPrincipal AppUserImpl principal,
      @RequestBody CreateBookcaseRequest createBookcaseRequest) {
    logger.info(
        "Received request to create bookcase at location: {}", createBookcaseRequest.location());
    CreateBookcaseResult createBookcaseResult =
        bookcaseFacade.createNewBookCase(
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
    bookcaseFacade.deleteBookcase(bookcaseId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("/locations")
  public ResponseEntity<Set<String>> getAllBookcaseLocations() {
    logger.info("Received request to get all bookcase locations");
    Set<String> locations = Set.copyOf(bookcaseFacade.getAllBookcaseLocations());
    return ResponseEntity.ok(locations);
  }

  @GetMapping("/location/{location}")
  public ResponseEntity<List<BookcaseDTO>> getBookcaseByLocation(@PathVariable String location) {
    logger.info("Received request to get bookcase at location: {}", location);
    List<BookcaseDTO> bookcases = bookcaseFacade.getAllBookcasesByLocation(location);
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
