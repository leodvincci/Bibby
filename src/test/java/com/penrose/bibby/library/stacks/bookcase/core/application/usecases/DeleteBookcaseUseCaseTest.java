package com.penrose.bibby.library.stacks.bookcase.core.application.usecases;

import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.BookcaseRepository;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.ShelfAccessPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteBookcaseUseCaseTest {

  @Mock private BookcaseRepository bookcaseRepository;

  @Mock private ShelfAccessPort shelfAccessPort;

  @InjectMocks private DeleteBookcaseUseCase deleteBookcaseUseCase;

  @Test
  void deleteBookcase_shouldDeleteShelvesThenBookcase() {
    Long bookcaseId = 100L;

    deleteBookcaseUseCase.deleteBookcase(bookcaseId);

    InOrder inOrder = Mockito.inOrder(shelfAccessPort, bookcaseRepository);
    inOrder.verify(shelfAccessPort).deleteAllShelvesInBookcase(bookcaseId);
    inOrder.verify(bookcaseRepository).deleteById(bookcaseId);
  }
}
