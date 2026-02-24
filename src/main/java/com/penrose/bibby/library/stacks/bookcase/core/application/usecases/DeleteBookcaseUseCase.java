package com.penrose.bibby.library.stacks.bookcase.core.application.usecases;

import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.BookcaseRepository;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.ShelfAccessPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteBookcaseUseCase {

  private final BookcaseRepository bookcaseRepository;
  private final ShelfAccessPort shelfAccessPort;

  public DeleteBookcaseUseCase(
      BookcaseRepository bookcaseRepository, ShelfAccessPort shelfAccessPort) {
    this.bookcaseRepository = bookcaseRepository;
    this.shelfAccessPort = shelfAccessPort;
  }

  @Transactional
  public void deleteBookcase(Long bookcaseId) {
    shelfAccessPort.deleteAllShelvesInBookcase(bookcaseId);
    bookcaseRepository.deleteById(bookcaseId);
  }
}
