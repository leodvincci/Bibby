package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Service class responsible for handling the creation of shelves. This use case interacts with the
 * ShelfDomainRepository to persist shelf data.
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
   * @param shelfLabel the label of the shelf to be created
   * @param shelfPosition the position of the shelf to be created
   * @param bookCapacity the capacity of the shelf to be created
   * @param bookcaseId the ID of the bookcase to be created
   */
  public void execute(String shelfLabel, int shelfPosition, int bookCapacity, Long bookcaseId) {
    Shelf shelf = new Shelf(shelfLabel, shelfPosition, bookCapacity, null, List.of(), bookcaseId);
    shelfDomainRepositoryPort.createNewShelfInBookcase(shelf);
  }
}
