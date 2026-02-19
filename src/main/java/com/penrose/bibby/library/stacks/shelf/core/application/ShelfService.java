package com.penrose.bibby.library.stacks.shelf.core.application;

import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShelfService implements ShelfFacade {

  private final BookFacade bookFacade;
  private final ShelfDomainRepository shelfDomainRepository;
  private final Logger logger = LoggerFactory.getLogger(ShelfService.class);

  public ShelfService(ShelfDomainRepository shelfDomainRepository, BookFacade bookFacade) {
    this.shelfDomainRepository = shelfDomainRepository;
    this.bookFacade = bookFacade;
  }

  public List<Shelf> getAllShelves(Long bookCaseId) {
    return shelfDomainRepository.findByBookcaseId(bookCaseId);
  }

  @Override
  public List<Shelf> findAllShelves(Long bookCaseId) {
    return shelfDomainRepository.findByBookcaseId(bookCaseId);
  }

  @Transactional
  public Optional<Shelf> findShelfById(Long shelfId) {
    Shelf shelf = shelfDomainRepository.getById(new ShelfId(shelfId));
    if (shelf == null) {
      return Optional.empty();
    }
    return Optional.of(shelf);
  }

  public List<Shelf> findByBookcaseId(Long bookcaseId) {
    return shelfDomainRepository.findByBookcaseId(bookcaseId);
  }

  public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
    return shelfDomainRepository.findShelfSummariesByBookcaseId(bookcaseId).stream()
        .map(
            shelf ->
                new ShelfSummary(
                    shelf.getShelfId().shelfId(), shelf.getShelfLabel(), shelf.getBookCount()))
        .toList();
  }

  @Transactional
  @Override
  public void deleteAllShelvesInBookcase(Long bookcaseId) {
    List<Shelf> shelves = shelfDomainRepository.findByBookcaseId(bookcaseId);
    List<Long> shelfIds = shelves.stream().map(shelf -> shelf.getShelfId().shelfId()).toList();
    bookFacade.deleteByShelfIdIn(shelfIds);
    logger.info("Deleted {} shelves from bookcase with ID: {}", shelfIds.size(), bookcaseId);
    shelfDomainRepository.deleteByBookcaseId(bookcaseId);
    logger.info("Bookcase with ID: {} has been cleared of shelves", bookcaseId);
  }

  @Override
  public void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    if (bookCapacity <= 0) {
      throw new IllegalArgumentException("Book capacity cannot be negative");
    }
    if (shelfLabel == null || shelfLabel.isBlank()) {
      throw new IllegalArgumentException("Shelf label cannot be null or blank");
    }
    if (bookcaseId == null) {
      throw new IllegalArgumentException("Bookcase ID cannot be null");
    }

    if (position <= 0) {
      throw new IllegalArgumentException("Shelf position must be greater than 0");
    }
    shelfDomainRepository.save(bookcaseId, position, shelfLabel, bookCapacity);
  }

  public List<Shelf> getShelfOptions() {
    return shelfDomainRepository.findAll();
  }

  public List<Shelf> getShelfOptionsByBookcase(Long bookcaseId) {
    return shelfDomainRepository.getShelfShelfOptionResponse(bookcaseId);
  }
}
