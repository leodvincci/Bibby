package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Placement;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.PlacementRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceBookOnShelfUseCaseTest {

  @Mock private PlacementRepositoryPort placementRepositoryPort;
  @Mock private BookAccessPort bookAccessPort;
  @InjectMocks private PlaceBookOnShelfUseCase placeBookOnShelfUseCase;
  @Captor private ArgumentCaptor<Placement> placementCaptor;

  @Test
  void execute_shouldPlaceBookOnShelfSuccessfully() {
    Long bookId = 1L;
    Long shelfId = 2L;
    when(bookAccessPort.getBookById(bookId)).thenReturn(bookId);

    placeBookOnShelfUseCase.execute(bookId, shelfId);

    verify(placementRepositoryPort).placeBookOnShelf(placementCaptor.capture());
    Placement captured = placementCaptor.getValue();
    assertThat(captured.getBookId()).isEqualTo(bookId);
    assertThat(captured.getShelfId()).isEqualTo(shelfId);
  }

  @Test
  void execute_shouldThrowWhenBookDoesNotExist() {
    Long bookId = 99L;
    Long shelfId = 2L;
    when(bookAccessPort.getBookById(bookId)).thenReturn(null);

    assertThatThrownBy(() -> placeBookOnShelfUseCase.execute(bookId, shelfId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Book with id 99 does not exist.");

    verify(placementRepositoryPort, never()).placeBookOnShelf(any());
  }

  @Test
  void execute_shouldCheckBookExistenceBeforePlacing() {
    Long bookId = 1L;
    Long shelfId = 2L;
    when(bookAccessPort.getBookById(bookId)).thenReturn(bookId);

    placeBookOnShelfUseCase.execute(bookId, shelfId);

    verify(bookAccessPort).getBookById(bookId);
    verify(placementRepositoryPort).placeBookOnShelf(any(Placement.class));
  }
}
