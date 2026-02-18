package com.penrose.bibby.library.stacks.shelf.core.application;

import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.api.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.domain.Shelf;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShelfService implements ShelfFacade {

  ShelfJpaRepository shelfJpaRepository;
  BookJpaRepository bookJpaRepository;
  ShelfMapper shelfMapper;
  private final Logger logger = LoggerFactory.getLogger(ShelfService.class);

  public ShelfService(
      ShelfMapper shelfMapper,
      ShelfJpaRepository shelfJpaRepository,
      BookJpaRepository bookJpaRepository) {
    this.shelfJpaRepository = shelfJpaRepository;
    this.bookJpaRepository = bookJpaRepository;
    this.shelfMapper = shelfMapper;
  }

  public List<ShelfEntity> getAllShelves(Long bookCaseId) {
    return shelfJpaRepository.findByBookcaseId(bookCaseId);
  }

  @Transactional
  public Optional<ShelfDTO> findShelfById(Long shelfId) {
    ShelfEntity shelfEntity = shelfJpaRepository.findById(shelfId).orElse(null);

    List<BookEntity> bookEntities = bookJpaRepository.findByShelfId(shelfId);
    List<Long> bookIds = bookEntities.stream().map(BookEntity::getBookId).toList();

    return Optional.of(ShelfDTO.fromEntityWithBookId(shelfEntity, bookIds));
  }

  public List<ShelfDTO> findByBookcaseId(Long bookcaseId) {
    List<ShelfEntity> shelves = shelfJpaRepository.findByBookcaseId(bookcaseId);
    return shelves.stream().map(ShelfDTO::fromEntity).collect(Collectors.toList());
  }

  @Transactional
  @Override
  public List<BookDTO> findBooksByShelf(Long aLong) {
    List<BookEntity> books = bookJpaRepository.findByShelfId(aLong);
    return books.stream().map(BookDTO::fromEntity).collect(Collectors.toList());
  }

  public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
    return shelfJpaRepository.findShelfSummariesByBookcaseId(bookcaseId);
  }

  @Transactional
  @Override
  public void deleteAllShelvesInBookcase(Long bookcaseId) {
    List<ShelfEntity> shelves = shelfJpaRepository.findByBookcaseId(bookcaseId);
    List<Long> shelfIds = shelves.stream().map(ShelfEntity::getShelfId).toList();
    bookJpaRepository.deleteByShelfIdIn(shelfIds);
    logger.info("Deleted {} shelves from bookcase with ID: {}", shelfIds.size(), bookcaseId);
    shelfJpaRepository.deleteByBookcaseId(bookcaseId);
    logger.info("Bookcase with ID: {} has been cleared of shelves", bookcaseId);
  }

  @Override
  public Boolean isFull(ShelfDTO shelfDTO) {
    return shelfJpaRepository
        .findById(shelfDTO.shelfId())
        .map(shelfMapper::toDomain)
        .map(Shelf::isFull)
        .orElseThrow(() -> new RuntimeException("Shelf not found with ID: " + shelfDTO.shelfId()));
  }

  @Override
  public void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    if (bookCapacity <= 0) {
      throw new IllegalArgumentException("Book capacity cannot be negative");
    } else if (shelfLabel == null || shelfLabel.isBlank()) {
      throw new IllegalArgumentException("Shelf label cannot be null or blank");
    } else if (position < 0) {
      throw new IllegalArgumentException("Shelf position cannot be negative");
    }

    ShelfEntity shelfEntity =
        shelfJpaRepository.save(
            new ShelfEntity() {
              {
                setBookcaseId(bookcaseId);
                setShelfPosition(position);
                setShelfLabel(shelfLabel);
                setBookCapacity(bookCapacity);
              }
            });
    logger.info("Shelf created with ID: {} for bookcase: {}", shelfEntity.getShelfId(), bookcaseId);
  }

  public List<ShelfOptionResponse> getShelfOptions() {
    return shelfJpaRepository.findAll().stream()
        .map(this::toShelfOption)
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
    return getAllShelves(bookcaseId).stream().map(ShelfDTO::fromEntity).toList();
  }

  public List<ShelfOptionResponse> getShelfOptionsByBookcase(Long bookcaseId) {
    List<ShelfEntity> shelfEntities = shelfJpaRepository.getShelfEntitiesByBookcaseId(bookcaseId);
    return shelfEntities.stream().map(this::toShelfOption).collect(Collectors.toList());
  }
}
