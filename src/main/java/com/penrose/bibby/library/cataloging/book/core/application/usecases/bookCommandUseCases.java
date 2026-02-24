package com.penrose.bibby.library.cataloging.book.core.application.usecases;

import com.penrose.bibby.library.cataloging.book.api.dtos.BookShelfAssignmentRequest;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.BookDomainRepository;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.ShelfAccessPort;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class bookCommandUseCases {

  private static final Logger log = LoggerFactory.getLogger(bookCommandUseCases.class);
  private final BookDomainRepository bookDomainRepository;
  private final ShelfAccessPort shelfAccessPort;

  public bookCommandUseCases(
      BookDomainRepository bookDomainRepository, ShelfAccessPort shelfAccessPort) {
    this.bookDomainRepository = bookDomainRepository;
    this.shelfAccessPort = shelfAccessPort;
  }

  public void placeBookOnShelf(Long bookId, BookShelfAssignmentRequest shelfAssignmentRequest) {
    if (shelfAssignmentRequest == null || shelfAssignmentRequest.shelfId() == null) {
      throw new IllegalStateException("Shelf id is required");
    }

    if (!shelfAccessPort.findShelfById(shelfAssignmentRequest.shelfId()).isPresent()) {
      throw new IllegalStateException(
          "Shelf not found with id: " + shelfAssignmentRequest.shelfId());
    }

    if (shelfAccessPort.isFull(shelfAssignmentRequest.shelfId())) {
      throw new IllegalStateException(
          "Shelf with id " + shelfAssignmentRequest.shelfId() + " is full");
    }

    bookDomainRepository.placeBookOnShelf(bookId, shelfAssignmentRequest.shelfId());
    log.info(
        "Placed book with id {} on shelf with id {}", bookId, shelfAssignmentRequest.shelfId());
  }

  public List<Long> getBookIdsByShelfId(Long shelfId) {
    return bookDomainRepository.getBookIdsByShelfId(shelfId);
  }

  public void deleteByShelfId(List<Long> shelfIds) {
    bookDomainRepository.deleteByShelfId(shelfIds);
    
  }
}
