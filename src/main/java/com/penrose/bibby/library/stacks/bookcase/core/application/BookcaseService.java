package com.penrose.bibby.library.stacks.bookcase.core.application;

import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.api.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.api.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.bookcase.core.domain.ports.outbound.BookcaseRepository;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import com.penrose.bibby.library.stacks.shelf.api.ports.inbound.ShelfFacade;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookcaseService implements BookcaseFacade {
  private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);
  private final BookcaseRepository bookcaseRepository;
  private final ResponseStatusException existingRecordError =
      new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase with the label already exist");
  private final ShelfFacade shelfFacade;

  public BookcaseService(BookcaseRepository bookcaseRepository, ShelfFacade shelfFacade) {
    this.bookcaseRepository = bookcaseRepository;
    this.shelfFacade = shelfFacade;
  }

  public CreateBookcaseResult createNewBookCase(
      Long userId,
      String label,
      String bookcaseZone,
      String bookcaseZoneIndex,
      int shelfCapacity,
      int bookCapacity,
      String location) {
    BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLocation(label);
    if (bookcaseEntity != null) {
      log.error("Failed to save Record - Record already exist", existingRecordError);
      throw existingRecordError;
    } else {
      bookcaseEntity =
          new BookcaseEntity(
              userId,
              location,
              bookcaseZone,
              bookcaseZoneIndex,
              shelfCapacity,
              bookCapacity * shelfCapacity);
      bookcaseEntity = bookcaseRepository.save(bookcaseEntity);

      for (int i = 1; i <= bookcaseEntity.getShelfCapacity(); i++) {
        addShelf(bookcaseEntity, i, i, bookCapacity);
      }

      CreateBookcaseResult createBookcaseResult =
          new CreateBookcaseResult(bookcaseEntity.getBookcaseId());
      log.info("Created new bookcase with Id: {}", createBookcaseResult.bookcaseId());

      return createBookcaseResult;
    }
  }

  @Override
  public List<String> getAllBookcaseLocations() {
    return bookcaseRepository.getAllBookCaseLocations();
  }

  @Override
  public Optional<BookcaseEntity> findById(Long bookcaseId) {
    return bookcaseRepository.findById(bookcaseId);
  }

  @Override
  public List<BookcaseDTO> getAllBookcasesByLocation(String location) {
    return bookcaseRepository.findByLocation(location).stream()
        .map(
            entity ->
                new BookcaseDTO(
                    entity.getBookcaseId(),
                    entity.getShelfCapacity(),
                    entity.getBookCapacity(),
                    entity.getBookcaseLocation(),
                    entity.getBookcaseZone(),
                    entity.getBookcaseIndex()))
        .toList();
  }

  @Override
  public List<BookcaseDTO> getAllBookcasesByUserId(Long appUserId) {
    return bookcaseRepository.findByAppUserId(appUserId).stream()
        .map(
            entity ->
                new BookcaseDTO(
                    entity.getBookcaseId(),
                    entity.getShelfCapacity(),
                    entity.getBookCapacity(),
                    entity.getBookcaseLocation(),
                    entity.getBookcaseZone(),
                    entity.getBookcaseIndex()))
        .toList();
  }

  @Override
  public void deleteBookcase(Long bookcaseId) {
    bookcaseRepository.deleteById(bookcaseId);
    log.info("Bookcase with Id {} was deleted.", bookcaseId);
  }

  public void addShelf(BookcaseEntity bookcaseEntity, int label, int position, int bookCapacity) {
    if (bookCapacity <= 0) {
      throw new IllegalArgumentException("Book capacity cannot be negative");
    } else if (bookcaseEntity == null) {
      throw new NullPointerException("Bookcase cannot be null");
    } else if (position <= 0 || position > bookcaseEntity.getShelfCapacity()) {
      throw new IllegalArgumentException(
          "Position must be between 1 and " + bookcaseEntity.getShelfCapacity());
    } else if (label <= 0) {
      throw new IllegalArgumentException("Label must be a positive integer");
    }

    shelfFacade.createShelf(
        bookcaseEntity.getBookcaseId(), position, "Shelf " + label, bookCapacity);
  }

  public List<BookcaseDTO> getAllBookcases() {
    List<BookcaseEntity> bookcaseEntities = bookcaseRepository.findAll();
    return bookcaseEntities.stream()
        .map(
            entity ->
                new BookcaseDTO(
                    entity.getBookcaseId(),
                    entity.getShelfCapacity(),
                    entity.getBookCapacity(),
                    entity.getBookcaseLocation(),
                    entity.getBookcaseZone(),
                    entity.getBookcaseIndex()))
        .toList();
  }

  public Optional<BookcaseDTO> findBookCaseById(Long id) {
    Optional<BookcaseEntity> bookcaseEntity = bookcaseRepository.findById(id);

    if (bookcaseEntity.isEmpty()) {
      return Optional.empty();
    }
    return BookcaseDTO.fromEntity(bookcaseEntity);
  }
}
