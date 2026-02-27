package com.penrose.bibby.library.stacks.bookcase.core.ports.outbound;

public interface ShelfAccessPort {
  void deleteAllShelvesInBookcase(Long bookcaseId);

  void createShelf(Long bookcaseId, int position, String shelfLabel, int bookCapacity);
}
