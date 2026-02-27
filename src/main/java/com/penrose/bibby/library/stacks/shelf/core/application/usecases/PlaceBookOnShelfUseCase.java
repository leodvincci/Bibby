package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Placement;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.PlacementRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class PlaceBookOnShelfUseCase {

  private final PlacementRepositoryPort placementRepositoryPort;
  private final BookAccessPort bookAccessPort;

  public PlaceBookOnShelfUseCase(
      PlacementRepositoryPort placementRepositoryPort, BookAccessPort bookAccessPort) {
    this.placementRepositoryPort = placementRepositoryPort;
    this.bookAccessPort = bookAccessPort;
  }

  public void execute(Long bookId, Long shelfId) {
    if (bookAccessPort.getBookById(bookId) == null) {
      throw new IllegalArgumentException("Book with id " + bookId + " does not exist.");
    }
    Placement placement = new Placement(bookId, shelfId);
    placementRepositoryPort.placeBookOnShelf(placement);
  }
}
