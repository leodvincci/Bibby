package com.penrose.bibby.library.stacks.shelf.core.ports.inbound;

public interface PlaceBookOnShelfUseCasePort {
  void execute(Long bookId, Long shelfId);
}
