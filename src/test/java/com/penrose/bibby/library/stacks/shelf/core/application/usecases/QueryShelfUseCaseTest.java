package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryShelfUseCaseTest {

  @Mock private ShelfDomainRepository shelfDomainRepository;
  @InjectMocks private QueryShelfUseCase queryShelfUseCase;

  @Test
  void findAllShelves_shouldReturnAllShelvesForBookcase() {
    Long bookcaseId = 100L;
    Shelf shelf1 = mock(Shelf.class);
    Shelf shelf2 = mock(Shelf.class);

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of(shelf1, shelf2));

    List<Shelf> result = queryShelfUseCase.findAllShelves(bookcaseId);

    assertThat(result).hasSize(2).containsExactly(shelf1, shelf2);
    verify(shelfDomainRepository).findByBookcaseId(bookcaseId);
  }

  @Test
  void findAllShelves_shouldReturnEmptyListWhenBookcaseHasNoShelves() {
    Long bookcaseId = 100L;

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of());

    List<Shelf> result = queryShelfUseCase.findAllShelves(bookcaseId);

    assertThat(result).isEmpty();
    verify(shelfDomainRepository).findByBookcaseId(bookcaseId);
  }

  @Test
  void findAllShelves_shouldDelegateToRepository() {
    Long bookcaseId = 100L;
    Shelf shelf = mock(Shelf.class);

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of(shelf));

    List<Shelf> result = queryShelfUseCase.findAllShelves(bookcaseId);

    assertThat(result).containsExactly(shelf);
    verify(shelfDomainRepository).findByBookcaseId(bookcaseId);
  }

  @Test
  void findShelfById_shouldReturnShelfWhenFound() {
    Long shelfId = 1L;
    Shelf shelf = mock(Shelf.class);

    when(shelfDomainRepository.getById(new ShelfId(shelfId))).thenReturn(shelf);

    Optional<Shelf> result = queryShelfUseCase.findShelfById(shelfId);

    assertThat(result).isPresent().contains(shelf);
  }

  @Test
  void findShelfById_shouldReturnEmptyWhenNotFound() {
    Long shelfId = 1L;

    when(shelfDomainRepository.getById(new ShelfId(shelfId))).thenReturn(null);

    Optional<Shelf> result = queryShelfUseCase.findShelfById(shelfId);

    assertThat(result).isEmpty();
  }

  @Test
  void getShelfSummariesForBookcase_shouldReturnMappedSummaries() {
    Long bookcaseId = 100L;
    Shelf shelf = mock(Shelf.class);
    when(shelf.getShelfId()).thenReturn(new ShelfId(1L));
    when(shelf.getShelfLabel()).thenReturn("Shelf A");
    when(shelf.getBookCount()).thenReturn(5);

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of(shelf));

    List<ShelfSummary> result = queryShelfUseCase.getShelfSummariesForBookcase(bookcaseId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).shelfId()).isEqualTo(1L);
    assertThat(result.get(0).label()).isEqualTo("Shelf A");
    assertThat(result.get(0).bookCount()).isEqualTo(5);
  }

  @Test
  void getShelfSummariesForBookcase_shouldReturnEmptyListWhenNoShelves() {
    Long bookcaseId = 100L;

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of());

    List<ShelfSummary> result = queryShelfUseCase.getShelfSummariesForBookcase(bookcaseId);

    assertThat(result).isEmpty();
  }

  @Test
  void findAll_shouldReturnAllShelves() {
    Shelf shelf1 = mock(Shelf.class);
    Shelf shelf2 = mock(Shelf.class);

    when(shelfDomainRepository.findAll()).thenReturn(List.of(shelf1, shelf2));

    List<Shelf> result = queryShelfUseCase.findAll();

    assertThat(result).hasSize(2).containsExactly(shelf1, shelf2);
    verify(shelfDomainRepository).findAll();
  }

  @Test
  void findAll_shouldReturnEmptyListWhenNoShelvesExist() {
    when(shelfDomainRepository.findAll()).thenReturn(List.of());

    List<Shelf> result = queryShelfUseCase.findAll();

    assertThat(result).isEmpty();
    verify(shelfDomainRepository).findAll();
  }
}
