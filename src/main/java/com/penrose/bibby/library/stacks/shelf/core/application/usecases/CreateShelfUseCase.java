package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for handling the creation of shelves.
 * This use case interacts with the ShelfDomainRepository to persist shelf data.
 */
@Service
public class CreateShelfUseCase {

  private final ShelfDomainRepositoryPort shelfDomainRepositoryPort;

  /**
   * Constructor for CreateShelfUseCase.
   *
   * @param shelfDomainRepositoryPort the repository used to persist shelf data
   */
  public CreateShelfUseCase(ShelfDomainRepositoryPort shelfDomainRepositoryPort) {
    this.shelfDomainRepositoryPort = shelfDomainRepositoryPort;
  }

  /**
   * Executes the use case to create and save a shelf.
   *
   * @param shelf the shelf entity to be saved
   */
  public void execute(Shelf shelf) {
    shelfDomainRepositoryPort.save(shelf);
  }
}
