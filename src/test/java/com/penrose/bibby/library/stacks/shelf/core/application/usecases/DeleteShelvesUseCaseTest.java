package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteShelvesUseCaseTest {

  @Mock private ShelfDomainRepositoryPort shelfDomainRepositoryPort;
  @Mock private BookAccessPort bookAccessPort;
  @InjectMocks private DeleteShelvesUseCase deleteShelvesUseCase;

  @Test
  void execute_shouldDeleteBooksBeforeShelves() {
    Long bookcaseId = 100L;
    Shelf shelf1 = mock(Shelf.class);
    Shelf shelf2 = mock(Shelf.class);
    when(shelf1.getShelfId()).thenReturn(new ShelfId(1L));
    when(shelf2.getShelfId()).thenReturn(new ShelfId(2L));

    when(shelfDomainRepositoryPort.findByBookcaseId(bookcaseId))
        .thenReturn(List.of(shelf1, shelf2));

    deleteShelvesUseCase.execute(bookcaseId);

    InOrder inOrder = inOrder(bookAccessPort, shelfDomainRepositoryPort);
    inOrder.verify(bookAccessPort).deleteBooksOnShelves(List.of(1L, 2L));
    inOrder.verify(shelfDomainRepositoryPort).deleteByBookcaseId(bookcaseId);
  }

  @Test
  void execute_shouldHandleEmptyBookcase() {
    Long bookcaseId = 100L;

    when(shelfDomainRepositoryPort.findByBookcaseId(bookcaseId)).thenReturn(List.of());

    deleteShelvesUseCase.execute(bookcaseId);

    verify(bookAccessPort).deleteBooksOnShelves(List.of());
    verify(shelfDomainRepositoryPort).deleteByBookcaseId(bookcaseId);
  }

  @Test
  void execute_shouldCollectAllShelfIdsForBookDeletion() {
    Long bookcaseId = 100L;
    Shelf shelf1 = mock(Shelf.class);
    Shelf shelf2 = mock(Shelf.class);
    Shelf shelf3 = mock(Shelf.class);
    when(shelf1.getShelfId()).thenReturn(new ShelfId(10L));
    when(shelf2.getShelfId()).thenReturn(new ShelfId(20L));
    when(shelf3.getShelfId()).thenReturn(new ShelfId(30L));

    when(shelfDomainRepositoryPort.findByBookcaseId(bookcaseId))
        .thenReturn(List.of(shelf1, shelf2, shelf3));

    deleteShelvesUseCase.execute(bookcaseId);

    verify(bookAccessPort).deleteBooksOnShelves(List.of(10L, 20L, 30L));
    verify(shelfDomainRepositoryPort).deleteByBookcaseId(bookcaseId);
  }

}
