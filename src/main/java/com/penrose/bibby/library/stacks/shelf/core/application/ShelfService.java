package com.penrose.bibby.library.stacks.shelf.core.application;

import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.mappers.ShelfDTOMapper;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShelfService implements ShelfFacade {

  private final BookFacade bookFacade;
  ShelfDTOMapper shelfDTOMapper;
  ShelfDomainRepository shelfDomainRepository;
  private final Logger logger = LoggerFactory.getLogger(ShelfService.class);

  public ShelfService(
      ShelfDomainRepository shelfDomainRepository,
      BookFacade bookFacade,
      ShelfDTOMapper shelfDTOMapper) {
    this.shelfDTOMapper = shelfDTOMapper;
    this.shelfDomainRepository = shelfDomainRepository;
    this.bookFacade = bookFacade;
  }

  public List<ShelfDTO> getAllShelves(Long bookCaseId) {
    return shelfDomainRepository.findByBookcaseId(bookCaseId).stream()
        .map(
            shelf -> {
              Long bookcaseId =
                  shelfDomainRepository.getBookcaseIdByShelfId(shelf.getShelfId().shelfId());
              return shelfDTOMapper.toDTO(shelf, bookcaseId);
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
    return Optional.of(shelfDTOMapper.toDTO(shelf, bookcaseId));
  }

  public List<ShelfDTO> findByBookcaseId(Long bookcaseId) {
    List<Shelf> shelves = shelfDomainRepository.findByBookcaseId(bookcaseId);
    return shelves.stream()
        .map(shelf -> shelfDTOMapper.toDTO(shelf, bookcaseId))
        .collect(Collectors.toList());
  }

  @Transactional
  @Override
  public List<BookDTO> findBooksByShelf(Long shelfId) {
    return bookFacade.findByShelfId(shelfId);
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
        .map(shelf -> shelfDTOMapper.toShelfOption(shelf))
        .collect(Collectors.toList());
  }

  @Override
  public List<ShelfDTO> getAllDTOShelves(Long bookcaseId) {
    return getAllShelves(bookcaseId);
  }

  public List<ShelfOptionResponse> getShelfOptionsByBookcase(Long bookcaseId) {
    return shelfDomainRepository.getShelfShelfOptionResponse(bookcaseId).stream()
        .map(shelf -> shelfDTOMapper.toShelfOption(shelf))
        .toList();
  }
}
