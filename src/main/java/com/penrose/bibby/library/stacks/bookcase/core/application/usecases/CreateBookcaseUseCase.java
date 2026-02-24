package com.penrose.bibby.library.stacks.bookcase.core.application.usecases;

import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.BookcaseRepository;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.ShelfAccessPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateBookcaseUseCase {

  private static final Logger log = LoggerFactory.getLogger(CreateBookcaseUseCase.class);
  private final BookcaseRepository bookcaseRepository;
  private final ShelfAccessPort shelfAccessPort;
  private final ResponseStatusException existingRecordError =
      new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase with the label already exist");

  public CreateBookcaseUseCase(
      BookcaseRepository bookcaseRepository, ShelfAccessPort shelfAccessPort) {
    this.bookcaseRepository = bookcaseRepository;
    this.shelfAccessPort = shelfAccessPort;
  }

  public CreateBookcaseResult createNewBookCase(
      Long userId,
      String label,
      String bookcaseZone,
      String bookcaseZoneIndex,
      int shelfCapacity,
      int bookCapacity,
      String location) {
    Bookcase existing = bookcaseRepository.findBookcaseByBookcaseLocation(label);
    if (existing != null) {
      log.error("Failed to save Record - Record already exist", existingRecordError);
      throw existingRecordError;
    }

    Bookcase bookcase =
        new Bookcase(
            null,
            shelfCapacity,
            bookCapacity * shelfCapacity,
            location,
            bookcaseZone,
            bookcaseZoneIndex);
    bookcaseRepository.save(bookcase);

    for (int i = 1; i <= bookcase.getShelfCapacity(); i++) {
      shelfAccessPort.createShelf(bookcase.getBookcaseId(), i, "Shelf " + i, bookCapacity);
    }

    CreateBookcaseResult result = new CreateBookcaseResult(bookcase.getBookcaseId());
    log.info("Created new bookcase with Id: {}", result.bookcaseId());

    return result;
  }
}
