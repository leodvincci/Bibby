package com.penrose.bibby.library.stacks.bookcase.infrastructure.adapter.outbound;

import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.bookcase.infrastructure.adapter.ShelfAccessPortAdapter;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.DeleteShelvesUseCasePort;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfCommandFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShelfAccessPortAdapterTest {

  @Mock private ShelfCommandFacade shelfCommandFacade;
  @Mock private DeleteShelvesUseCasePort deleteShelvesUseCasePort;
  @InjectMocks private ShelfAccessPortAdapter shelfAccessPortAdapter;

  @Test
  void deleteAllShelvesInBookcase_shouldDelegateToDeleteShelvesUseCasePort() {
    Long bookcaseId = 1L;

    shelfAccessPortAdapter.deleteAllShelvesInBookcase(bookcaseId);

    verify(deleteShelvesUseCasePort).execute(bookcaseId);
    verifyNoInteractions(shelfCommandFacade);
  }
}
