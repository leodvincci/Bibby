package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryShelfUseCase {

  private final ShelfDomainRepositoryPort shelfDomainRepositoryPort;

  public QueryShelfUseCase(ShelfDomainRepositoryPort shelfDomainRepositoryPort) {
    this.shelfDomainRepositoryPort = shelfDomainRepositoryPort;
  }

  public List<Shelf> findShelvesByBookcaseId(Long bookcaseId) {
    return shelfDomainRepositoryPort.findByBookcaseId(bookcaseId);
  }

  @Transactional
  public Optional<Shelf> findShelfById(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getById(new ShelfId(shelfId));
    if (shelf == null) {
      return Optional.empty();
    }
    return Optional.of(shelf);
  }

  public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
    return shelfDomainRepositoryPort.findByBookcaseId(bookcaseId).stream()
        .map(
            shelf ->
                new ShelfSummary(
                    shelf.getShelfId().shelfId(), shelf.getShelfLabel(), shelf.getBookCount()))
        .toList();
  }

  public List<Shelf> findAll() {
    return shelfDomainRepositoryPort.findAll();
  }

  public boolean isFull(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getById(new ShelfId(shelfId));
    if (shelf == null) {
      throw new IllegalStateException("Shelf not found with id: " + shelfId);
    }
    return shelf.isFull();
  }

  public boolean isEmpty(Long shelfId) {
    Shelf shelf = shelfDomainRepositoryPort.getById(new ShelfId(shelfId));
    if (shelf == null) {
      throw new IllegalStateException("Shelf not found with id: " + shelfId);
    }
    return shelf.getBookCount() == 0;
  }
}
