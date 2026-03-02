package com.penrose.bibby.library.stacks.bookcase.infrastructure.adapter.outbound;

import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfCommandFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.deleteShelvesUseCasePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShelfAccessPortAdapterTest {

  @Mock private ShelfCommandFacade shelfCommandFacade;
  @Mock private deleteShelvesUseCasePort deleteShelvesUseCasePort;
  @InjectMocks private ShelfAccessPortAdapter shelfAccessPortAdapter;

  @Test
  void deleteAllShelvesInBookcase_shouldDelegateToDeleteShelvesUseCasePort() {
    Long bookcaseId = 1L;

    shelfAccessPortAdapter.deleteAllShelvesInBookcase(bookcaseId);

    verify(deleteShelvesUseCasePort).execute(bookcaseId);
    verifyNoInteractions(shelfCommandFacade);
  }

  @Test
  void createShelf_shouldDelegateToShelfCommandFacade() {
    Long bookcaseId = 1L;
    int position = 2;
    String shelfLabel = "Fiction";
    int bookCapacity = 25;

    shelfAccessPortAdapter.createShelf(bookcaseId, position, shelfLabel, bookCapacity);

    verify(shelfCommandFacade)
        .createShelfInBookcaseByBookcaseId(bookcaseId, position, shelfLabel, bookCapacity);
    verifyNoInteractions(deleteShelvesUseCasePort);
  }

}
