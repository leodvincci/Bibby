package com.penrose.bibby.library.stacks.bookcase.core.application.usecases;

import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.BookcaseRepository;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.ShelfAccessPort;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateBookcaseUseCase {

  private static final Logger logger =
      org.slf4j.LoggerFactory.getLogger(CreateBookcaseUseCase.class);
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
      logger.error("Failed to save Record - Record already exist", existingRecordError);
      throw existingRecordError;
    }

    Bookcase bookcase =
        new Bookcase(
            null,
            userId,
            shelfCapacity,
            bookCapacity * shelfCapacity,
            location,
            bookcaseZone,
            bookcaseZoneIndex);

    logger.info("Creating new bookcase: {}", bookcase);

    bookcase = bookcaseRepository.save(bookcase);

    for (int i = 1; i <= bookcase.getShelfCapacity(); i++) {
      shelfAccessPort.createShelf(bookcase.getBookcaseId(), i, "Shelf " + i, bookCapacity);
    }
    logger.info("Shelves created successfully for bookcase ID: {}", bookcase.getBookcaseId());

    return new CreateBookcaseResult(bookcase.getBookcaseId());
  }
}
