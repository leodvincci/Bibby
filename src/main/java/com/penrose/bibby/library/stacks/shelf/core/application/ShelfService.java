package com.penrose.bibby.library.stacks.shelf.core.application;

import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.api.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.domain.ShelfDomainRepository;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShelfService implements ShelfFacade {

  BookJpaRepository bookJpaRepository;
  ShelfMapper shelfMapper;
  ShelfDomainRepository shelfDomainRepository;
  private final Logger logger = LoggerFactory.getLogger(ShelfService.class);

  public ShelfService(
      ShelfMapper shelfMapper,
      BookJpaRepository bookJpaRepository,
      ShelfDomainRepository shelfDomainRepository) {
    this.bookJpaRepository = bookJpaRepository;
    this.shelfMapper = shelfMapper;
    this.shelfDomainRepository = shelfDomainRepository;
  }

  public List<ShelfDTO> getAllShelves(Long bookCaseId) {
    return shelfDomainRepository.findByBookcaseId(bookCaseId).stream()
        .map(
            shelf -> {
              Long bookcaseId =
                  shelfDomainRepository.getBookcaseIdByShelfId(shelf.getShelfId().shelfId());
              return shelfMapper.toDTO(shelf, bookcaseId);
            })
        .collect(Collectors.toList());
  }

  @Transactional
  public Optional<ShelfDTO> findShelfById(Long shelfId) {
    Shelf shelf = shelfDomainRepository.getById(new ShelfId(shelfId));
    if (shelf == null) {
      return Optional.empty();
    }
    Long bookcaseId = shelfDomainRepository.getBookcaseIdByShelfId(shelfId);
    return Optional.of(shelfMapper.toDTO(shelf, bookcaseId));
  }

  public List<ShelfDTO> findByBookcaseId(Long bookcaseId) {
    List<Shelf> shelves = shelfDomainRepository.findByBookcaseId(bookcaseId);
    return shelves.stream()
        .map(shelf -> shelfMapper.toDTO(shelf, bookcaseId))
        .collect(Collectors.toList());
  }

  @Transactional
  @Override
  public List<BookDTO> findBooksByShelf(Long aLong) {
    List<BookEntity> books = bookJpaRepository.findByShelfId(aLong);
    return books.stream().map(BookDTO::fromEntity).collect(Collectors.toList());
  }

  public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
    return shelfDomainRepository.findShelfSummariesByBookcaseId(bookcaseId);
  }

  @Transactional
  @Override
  public void deleteAllShelvesInBookcase(Long bookcaseId) {
    List<Shelf> shelves = shelfDomainRepository.findByBookcaseId(bookcaseId);
    List<Long> shelfIds = shelves.stream().map(shelf -> shelf.getShelfId().shelfId()).toList();
    bookJpaRepository.deleteByShelfIdIn(shelfIds);
    logger.info("Deleted {} shelves from bookcase with ID: {}", shelfIds.size(), bookcaseId);
    shelfDomainRepository.deleteByBookcaseId(bookcaseId);
    logger.info("Bookcase with ID: {} has been cleared of shelves", bookcaseId);
  }

  @Override
  public Boolean isFull(ShelfDTO shelfDTO) {
    return shelfDomainRepository.findById(shelfDTO.shelfId()).isFull();
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

  public List<ShelfOptionResponse> getShelfOptions() {
    return shelfDomainRepository.findAll().stream()
        .map(shelf -> shelfMapper.toShelfOption(shelf))
        .collect(Collectors.toList());
  }

  private ShelfOptionResponse toShelfOption(ShelfEntity shelf) {
    long bookCount = bookJpaRepository.countByShelfId(shelf.getShelfId());
    //    BookcaseEntity bookcase = bookcaseFacade.findById(shelf.getBookcaseId()).orElse(null);
    //    String bookcaseLabel = bookcase != null ? bookcase.getBookcaseLocation() : "Unknown Case";
    boolean hasSpace = bookCount < shelf.getBookCapacity();
    return new ShelfOptionResponse(
        shelf.getShelfId(), shelf.getShelfLabel(), shelf.getBookCapacity(), bookCount, hasSpace);
  }

  @Override
  public List<ShelfDTO> getAllDTOShelves(Long bookcaseId) {
    return getAllShelves(bookcaseId);
  }

  public List<ShelfOptionResponse> getShelfOptionsByBookcase(Long bookcaseId) {
    return shelfDomainRepository.getShelfShelfOptionResponse(bookcaseId);
  }
}
