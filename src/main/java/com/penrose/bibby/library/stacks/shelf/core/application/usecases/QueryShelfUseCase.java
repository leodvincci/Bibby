package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryShelfUseCase {

  private final ShelfDomainRepository shelfDomainRepository;

  public QueryShelfUseCase(ShelfDomainRepository shelfDomainRepository) {
    this.shelfDomainRepository = shelfDomainRepository;
  }

  public List<Shelf> findAllShelves(Long bookcaseId) {
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

  public List<Shelf> findAll() {
    return shelfDomainRepository.findAll();
  }
}
