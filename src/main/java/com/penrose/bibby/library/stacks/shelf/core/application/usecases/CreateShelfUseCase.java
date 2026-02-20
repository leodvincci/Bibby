package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateShelfUseCase {

  private final ShelfDomainRepository shelfDomainRepository;

  public CreateShelfUseCase(ShelfDomainRepository shelfDomainRepository) {
    this.shelfDomainRepository = shelfDomainRepository;
  }

  /**
   * Creates a new shelf in the specified bookcase with the provided position, label, and book
   * capacity.
   *
   * @param bookcaseId the unique identifier of the bookcase where the shelf will be created. Cannot
   *     be null.
   * @param position the position of the shelf within the bookcase. Must be greater than 0.
   * @param shelfLabel the label or name assigned to the shelf. Cannot be null or blank.
   * @param bookCapacity the maximum number of books the shelf can accommodate. Must be greater than
   *     0.
   * @throws IllegalArgumentException if the bookCapacity is less than or equal to 0, if the
   *     shelfLabel is null or blank, if the bookcaseId is null, or if the position is less than or
   *     equal to 0.
   */
  public void execute(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
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
}
