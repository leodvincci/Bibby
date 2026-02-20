package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteShelvesUseCase {

  private final ShelfDomainRepository shelfDomainRepository;
  private final BookAccessPort bookAccessPort;
  private final Logger logger = LoggerFactory.getLogger(DeleteShelvesUseCase.class);

  public DeleteShelvesUseCase(
      ShelfDomainRepository shelfDomainRepository, BookAccessPort bookAccessPort) {
    this.shelfDomainRepository = shelfDomainRepository;
    this.bookAccessPort = bookAccessPort;
  }

  @Transactional
  public void execute(Long bookcaseId) {
    List<Shelf> shelves = shelfDomainRepository.findByBookcaseId(bookcaseId);
    List<Long> shelfIds = shelves.stream().map(shelf -> shelf.getShelfId().shelfId()).toList();
    bookAccessPort.deleteBooksOnShelves(shelfIds);
    logger.info("Deleted {} shelves from bookcase with ID: {}", shelfIds.size(), bookcaseId);
    shelfDomainRepository.deleteByBookcaseId(bookcaseId);
    logger.info("Bookcase with ID: {} has been cleared of shelves", bookcaseId);
  }
}
