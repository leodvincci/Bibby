package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfSummaryResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryShelfUseCaseTest {

  @Mock private ShelfDomainRepositoryPort shelfDomainRepositoryPort;
  @InjectMocks private QueryShelfUseCase queryShelfUseCase;

  @Test
  void findShelvesByBookcaseId_shouldReturnAllShelvesForBookcase() {
    Long bookcaseId = 100L;
    Shelf shelf1 = mock(Shelf.class);
    when(shelf1.getId()).thenReturn(1L);
    when(shelf1.getShelfPosition()).thenReturn(1);
    when(shelf1.getShelfLabel()).thenReturn("Shelf A");
    when(shelf1.getBookCapacity()).thenReturn(10);
    when(shelf1.getBooks()).thenReturn(List.of());
    when(shelf1.getBookcaseId()).thenReturn(bookcaseId);

    Shelf shelf2 = mock(Shelf.class);
    when(shelf2.getId()).thenReturn(2L);
    when(shelf2.getShelfPosition()).thenReturn(2);
    when(shelf2.getShelfLabel()).thenReturn("Shelf B");
    when(shelf2.getBookCapacity()).thenReturn(10);
    when(shelf2.getBooks()).thenReturn(List.of());
    when(shelf2.getBookcaseId()).thenReturn(bookcaseId);

    when(shelfDomainRepositoryPort.findByBookcaseId(bookcaseId))
        .thenReturn(List.of(shelf1, shelf2));

    List<ShelfResponse> result = queryShelfUseCase.findShelvesByBookcaseId(bookcaseId);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).id()).isEqualTo(1L);
    assertThat(result.get(1).id()).isEqualTo(2L);
    verify(shelfDomainRepositoryPort).findByBookcaseId(bookcaseId);
  }

  @Test
  void findShelvesByBookcaseId_shouldReturnEmptyListWhenBookcaseHasNoShelves() {
    Long bookcaseId = 100L;

    when(shelfDomainRepositoryPort.findByBookcaseId(bookcaseId)).thenReturn(List.of());

    List<ShelfResponse> result = queryShelfUseCase.findShelvesByBookcaseId(bookcaseId);

    assertThat(result).isEmpty();
    verify(shelfDomainRepositoryPort).findByBookcaseId(bookcaseId);
  }

  @Test
  void findShelvesByBookcaseId_shouldDelegateToRepository() {
    Long bookcaseId = 100L;
    Shelf shelf = mock(Shelf.class);
    when(shelf.getId()).thenReturn(1L);
    when(shelf.getShelfPosition()).thenReturn(1);
    when(shelf.getShelfLabel()).thenReturn("Shelf A");
    when(shelf.getBookCapacity()).thenReturn(10);
    when(shelf.getBooks()).thenReturn(List.of());
    when(shelf.getBookcaseId()).thenReturn(bookcaseId);

    when(shelfDomainRepositoryPort.findByBookcaseId(bookcaseId)).thenReturn(List.of(shelf));

    List<ShelfResponse> result = queryShelfUseCase.findShelvesByBookcaseId(bookcaseId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(1L);
    assertThat(result.get(0).shelfLabel()).isEqualTo("Shelf A");
    verify(shelfDomainRepositoryPort).findByBookcaseId(bookcaseId);
  }

  @Test
  void findShelfById_shouldReturnShelfResponseWhenFound() {
    Long shelfId = 1L;
    Shelf shelf = mock(Shelf.class);
    when(shelf.getId()).thenReturn(1L);
    when(shelf.getShelfPosition()).thenReturn(1);
    when(shelf.getShelfLabel()).thenReturn("Shelf A");
    when(shelf.getBookCapacity()).thenReturn(10);
    when(shelf.getBooks()).thenReturn(List.of());
    when(shelf.getBookcaseId()).thenReturn(100L);

    when(shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId))).thenReturn(shelf);

    Optional<ShelfResponse> result = queryShelfUseCase.findShelfById(shelfId);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(1L);
    assertThat(result.get().shelfLabel()).isEqualTo("Shelf A");
  }

  @Test
  void findShelfById_shouldReturnEmptyWhenNotFound() {
    Long shelfId = 1L;

    when(shelfDomainRepositoryPort.getShelfByShelfId(new ShelfId(shelfId))).thenReturn(null);

    Optional<ShelfResponse> result = queryShelfUseCase.findShelfById(shelfId);

    assertThat(result).isEmpty();
  }

  @Test
  void getShelfSummariesForBookcase_shouldReturnMappedSummaries() {
    Long bookcaseId = 100L;
    Shelf shelf = mock(Shelf.class);
    when(shelf.getShelfId()).thenReturn(new ShelfId(1L));
    when(shelf.getShelfLabel()).thenReturn("Shelf A");
    when(shelf.getBookCount()).thenReturn(5);

    when(shelfDomainRepositoryPort.findByBookcaseId(bookcaseId)).thenReturn(List.of(shelf));

    List<ShelfSummaryResponse> result =
        queryShelfUseCase.getShelfSummariesForBookcaseById(bookcaseId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).shelfId()).isEqualTo(1L);
    assertThat(result.get(0).label()).isEqualTo("Shelf A");
    assertThat(result.get(0).bookCount()).isEqualTo(5);
  }

  @Test
  void getShelfSummariesForBookcase_shouldReturnEmptyListWhenNoShelves() {
    Long bookcaseId = 100L;

    when(shelfDomainRepositoryPort.findByBookcaseId(bookcaseId)).thenReturn(List.of());

    List<ShelfSummaryResponse> result =
        queryShelfUseCase.getShelfSummariesForBookcaseById(bookcaseId);

    assertThat(result).isEmpty();
  }

  @Test
  void findAll_shouldReturnAllShelves() {
    Shelf shelf1 = mock(Shelf.class);
    when(shelf1.getId()).thenReturn(1L);
    when(shelf1.getShelfPosition()).thenReturn(1);
    when(shelf1.getShelfLabel()).thenReturn("Shelf A");
    when(shelf1.getBookCapacity()).thenReturn(10);
    when(shelf1.getBooks()).thenReturn(List.of());
    when(shelf1.getBookcaseId()).thenReturn(100L);

    Shelf shelf2 = mock(Shelf.class);
    when(shelf2.getId()).thenReturn(2L);
    when(shelf2.getShelfPosition()).thenReturn(2);
    when(shelf2.getShelfLabel()).thenReturn("Shelf B");
    when(shelf2.getBookCapacity()).thenReturn(10);
    when(shelf2.getBooks()).thenReturn(List.of());
    when(shelf2.getBookcaseId()).thenReturn(100L);

    when(shelfDomainRepositoryPort.findAll()).thenReturn(List.of(shelf1, shelf2));

    List<ShelfResponse> result = queryShelfUseCase.findAll();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).id()).isEqualTo(1L);
    assertThat(result.get(1).id()).isEqualTo(2L);
    verify(shelfDomainRepositoryPort).findAll();
  }

  @Test
  void findAll_shouldReturnEmptyListWhenNoShelvesExist() {
    when(shelfDomainRepositoryPort.findAll()).thenReturn(List.of());

    List<ShelfResponse> result = queryShelfUseCase.findAll();

    assertThat(result).isEmpty();
    verify(shelfDomainRepositoryPort).findAll();
  }
}
