package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteShelvesUseCase {

  private final ShelfDomainRepositoryPort shelfDomainRepositoryPort;
  private final BookAccessPort bookAccessPort;
  private final Logger logger = LoggerFactory.getLogger(DeleteShelvesUseCase.class);

  public DeleteShelvesUseCase(
      ShelfDomainRepositoryPort shelfDomainRepositoryPort, BookAccessPort bookAccessPort) {
    this.shelfDomainRepositoryPort = shelfDomainRepositoryPort;
    this.bookAccessPort = bookAccessPort;
  }

  @Transactional
  public void execute(Long bookcaseId) {
    List<Shelf> shelves = shelfDomainRepositoryPort.findByBookcaseId(bookcaseId);
    List<Long> shelfIds = shelves.stream().map(shelf -> shelf.getShelfId().shelfId()).toList();
    bookAccessPort.deleteBooksOnShelves(shelfIds);
    logger.info("Deleted {} shelves from bookcase with ID: {}", shelfIds.size(), bookcaseId);
    shelfDomainRepositoryPort.deleteByBookcaseId(bookcaseId);
    logger.info("Bookcase with ID: {} has been cleared of shelves", bookcaseId);
  }
}
