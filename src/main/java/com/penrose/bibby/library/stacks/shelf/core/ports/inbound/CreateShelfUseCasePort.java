package com.penrose.bibby.library.stacks.shelf.core.ports.inbound;

public interface CreateShelfUseCasePort {
  void execute(String shelfLabel, int shelfPosition, int bookCapacity, Long bookcaseId);
}
