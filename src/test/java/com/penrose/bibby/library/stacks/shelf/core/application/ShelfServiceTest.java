package com.penrose.bibby.library.stacks.shelf.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.application.usecases.CreateShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.DeleteShelvesUseCase;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.QueryShelfUseCase;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShelfServiceTest {

  @Mock private QueryShelfUseCase queryShelfUseCase;
  @Mock private CreateShelfUseCase createShelfUseCase;
  @Mock private DeleteShelvesUseCase deleteShelvesUseCase;
  @InjectMocks private ShelfService shelfService;

  @Test
  void findAllShelves_shouldDelegateToQueryUseCase() {
    Long bookcaseId = 100L;
    List<Shelf> expected = List.of(mock(Shelf.class));
    when(queryShelfUseCase.findAllShelves(bookcaseId)).thenReturn(expected);

    List<Shelf> result = shelfService.findAllShelves(bookcaseId);

    assertThat(result).isEqualTo(expected);
    verify(queryShelfUseCase).findAllShelves(bookcaseId);
  }

  @Test
  void findShelfById_shouldDelegateToQueryUseCase() {
    Long shelfId = 1L;
    Shelf shelf = mock(Shelf.class);
    when(queryShelfUseCase.findShelfById(shelfId)).thenReturn(Optional.of(shelf));

    Optional<Shelf> result = shelfService.findShelfById(shelfId);

    assertThat(result).isPresent().contains(shelf);
    verify(queryShelfUseCase).findShelfById(shelfId);
  }

  @Test
  void getShelfSummariesForBookcase_shouldDelegateToQueryUseCase() {
    Long bookcaseId = 100L;
    List<ShelfSummary> expected = List.of(new ShelfSummary(1L, "A", 5));
    when(queryShelfUseCase.getShelfSummariesForBookcase(bookcaseId)).thenReturn(expected);

    List<ShelfSummary> result = shelfService.getShelfSummariesForBookcase(bookcaseId);

    assertThat(result).isEqualTo(expected);
    verify(queryShelfUseCase).getShelfSummariesForBookcase(bookcaseId);
  }

  @Test
  void createShelf_shouldDelegateToCreateUseCase() {
    shelfService.createShelf(100L, 1, "Shelf A", 10);

    verify(createShelfUseCase).execute(100L, 1, "Shelf A", 10);
  }

  @Test
  void deleteAllShelvesInBookcase_shouldDelegateToDeleteUseCase() {
    Long bookcaseId = 100L;

    shelfService.deleteAllShelvesInBookcase(bookcaseId);

    verify(deleteShelvesUseCase).execute(bookcaseId);
  }
}
