package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateShelfUseCaseTest {

  @Mock private ShelfDomainRepositoryPort shelfDomainRepositoryPort;
  @InjectMocks private CreateShelfUseCase createShelfUseCase;

  @Test
  void execute_shouldSaveShelfSuccessfully() {
    Shelf shelf = new Shelf("Shelf A", 1, 10, null, List.of(), 100L);

    createShelfUseCase.execute(shelf);

    verify(shelfDomainRepositoryPort).save(shelf);
  }

  @Test
  void execute_shouldSaveShelfWithMinimumBookCapacity() {
    Shelf shelf = new Shelf("Shelf A", 1, 1, null, List.of(), 100L);

    createShelfUseCase.execute(shelf);

    verify(shelfDomainRepositoryPort).save(shelf);
  }

  @Test
  void execute_shouldSaveShelfWithLargeBookCapacity() {
    Shelf shelf = new Shelf("Shelf Z", 5, 1000, null, List.of(), 100L);

    createShelfUseCase.execute(shelf);

    verify(shelfDomainRepositoryPort).save(shelf);
  }
}
