package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case responsible for deleting all shelves and their associated books from a given bookcase.
 *
 * <p>This operation first removes all books on the shelves via {@link BookAccessPort}, then deletes
 * the shelves themselves from the repository.
 */
@Service
public class DeleteShelvesUseCase {

  private final ShelfDomainRepositoryPort shelfDomainRepositoryPort;
  private final BookAccessPort bookAccessPort;
  private final Logger logger = LoggerFactory.getLogger(DeleteShelvesUseCase.class);

  /**
   * Constructs a {@code DeleteShelvesUseCase} with the required ports.
   *
   * @param shelfDomainRepositoryPort the repository port for shelf persistence operations
   * @param bookAccessPort the port for book-related operations across shelf boundaries
   */
  public DeleteShelvesUseCase(
      ShelfDomainRepositoryPort shelfDomainRepositoryPort, BookAccessPort bookAccessPort) {
    this.shelfDomainRepositoryPort = shelfDomainRepositoryPort;
    this.bookAccessPort = bookAccessPort;
  }

  /**
   * Deletes all shelves belonging to the specified bookcase, including their books.
   *
   * <p>Executes within a transaction to ensure atomicity: books are deleted first, followed by the
   * shelves.
   *
   * @param bookcaseId the ID of the bookcase whose shelves should be deleted
   */
  @Transactional
  public void execute(Long bookcaseId) {
    List<Shelf> shelves = shelfDomainRepositoryPort.findByBookcaseId(bookcaseId);
    List<Long> shelfIds = shelves.stream().map(shelf -> shelf.getShelfId().shelfId()).toList();
    bookAccessPort.deleteBooksOnShelves(shelfIds);
    logger.info("Deleted all shelves in bookcase with ID: {}", bookcaseId);
    shelfDomainRepositoryPort.deleteByBookcaseId(bookcaseId);
    logger.info("Bookcase with ID: {} has been cleared of shelves", bookcaseId);
  }
}
