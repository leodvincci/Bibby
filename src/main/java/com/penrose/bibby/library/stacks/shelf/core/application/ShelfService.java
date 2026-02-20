package com.penrose.bibby.library.stacks.shelf.core.application;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShelfService implements ShelfFacade {

  private final ShelfDomainRepository shelfDomainRepository;
  private final Logger logger = LoggerFactory.getLogger(ShelfService.class);
  private final BookAccessPort bookAccessPort;

  public ShelfService(ShelfDomainRepository shelfDomainRepository, BookAccessPort bookAccessPort) {
    this.shelfDomainRepository = shelfDomainRepository;
    this.bookAccessPort = bookAccessPort;
  }

  @Override
  public List<Shelf> findAllShelves(Long bookCaseId) {
    return shelfDomainRepository.findByBookcaseId(bookCaseId);
  }

  public List<Shelf> getShelfOptionsByBookcase(Long bookcaseId) {
    return shelfDomainRepository.findByBookcaseId(bookcaseId);
  }

  @Transactional
  public Optional<Shelf> findShelfById(Long shelfId) {
    Shelf shelf = shelfDomainRepository.getById(new ShelfId(shelfId));
    if (shelf == null) {
      return Optional.empty();
    }
    return Optional.of(shelf);
  }



  public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
    return shelfDomainRepository.findByBookcaseId(bookcaseId).stream()
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
    bookAccessPort.deleteBooksOnShelves(shelfIds);
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


}
