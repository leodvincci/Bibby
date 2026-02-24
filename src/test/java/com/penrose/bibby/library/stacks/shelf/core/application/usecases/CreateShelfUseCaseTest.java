package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateShelfUseCaseTest {

  @Mock private ShelfDomainRepositoryPort shelfDomainRepositoryPort;
  @InjectMocks private CreateShelfUseCase createShelfUseCase;
  @Captor private ArgumentCaptor<Shelf> shelfCaptor;

  @Test
  void execute_shouldSaveShelfSuccessfully() {
    createShelfUseCase.execute("Shelf A", 1, 10, 100L);

    verify(shelfDomainRepositoryPort).createNewShelfInBookcase(shelfCaptor.capture());
    Shelf captured = shelfCaptor.getValue();
    assertThat(captured.getShelfLabel()).isEqualTo("Shelf A");
    assertThat(captured.getShelfPosition()).isEqualTo(1);
    assertThat(captured.getBookCapacity()).isEqualTo(10);
    assertThat(captured.getBookcaseId()).isEqualTo(100L);
    assertThat(captured.getBookIds()).isEmpty();
    assertThat(captured.getShelfId()).isNull();
  }

  @Test
  void execute_shouldSaveShelfWithMinimumBookCapacity() {
    createShelfUseCase.execute("Shelf A", 1, 1, 100L);

    verify(shelfDomainRepositoryPort).createNewShelfInBookcase(shelfCaptor.capture());
    Shelf captured = shelfCaptor.getValue();
    assertThat(captured.getShelfLabel()).isEqualTo("Shelf A");
    assertThat(captured.getBookCapacity()).isEqualTo(1);
  }

  @Test
  void execute_shouldSaveShelfWithLargeBookCapacity() {
    createShelfUseCase.execute("Shelf Z", 5, 1000, 100L);

    verify(shelfDomainRepositoryPort).createNewShelfInBookcase(shelfCaptor.capture());
    Shelf captured = shelfCaptor.getValue();
    assertThat(captured.getShelfLabel()).isEqualTo("Shelf Z");
    assertThat(captured.getShelfPosition()).isEqualTo(5);
    assertThat(captured.getBookCapacity()).isEqualTo(1000);
  }
}
