package com.penrose.bibby.library.stacks.bookcase.core.ports.inbound;

import com.penrose.bibby.library.stacks.bookcase.core.ports.portModel.CreateBookcaseResult;

public interface CreateBookcaseUseCasePort {
  CreateBookcaseResult createNewBookCase(
      Long userId,
      String label,
      String bookcaseZone,
      String bookcaseZoneIndex,
      int shelfCapacity,
      int bookCapacity,
      String location);
}
