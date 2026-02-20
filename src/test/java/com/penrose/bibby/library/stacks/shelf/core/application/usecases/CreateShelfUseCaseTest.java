package com.penrose.bibby.library.stacks.shelf.core.application.usecases;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateShelfUseCaseTest {

  @Mock private ShelfDomainRepository shelfDomainRepository;
  @InjectMocks private CreateShelfUseCase createShelfUseCase;

  @Test
  void execute_shouldCreateShelfSuccessfully() {
    Long bookcaseId = 100L;
    int position = 1;
    String shelfLabel = "Shelf A";
    int bookCapacity = 10;

    createShelfUseCase.execute(bookcaseId, position, shelfLabel, bookCapacity);

    verify(shelfDomainRepository).save(bookcaseId, position, shelfLabel, bookCapacity);
  }

  @Test
  void execute_shouldThrowExceptionWhenBookCapacityIsZero() {
    assertThatThrownBy(() -> createShelfUseCase.execute(100L, 1, "Shelf A", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Book capacity cannot be negative");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  @Test
  void execute_shouldThrowExceptionWhenBookCapacityIsNegative() {
    assertThatThrownBy(() -> createShelfUseCase.execute(100L, 1, "Shelf A", -5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Book capacity cannot be negative");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  @Test
  void execute_shouldThrowExceptionWhenShelfLabelIsNull() {
    assertThatThrownBy(() -> createShelfUseCase.execute(100L, 1, null, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  @Test
  void execute_shouldThrowExceptionWhenShelfLabelIsBlank() {
    assertThatThrownBy(() -> createShelfUseCase.execute(100L, 1, "   ", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  @Test
  void execute_shouldThrowExceptionWhenShelfLabelIsEmpty() {
    assertThatThrownBy(() -> createShelfUseCase.execute(100L, 1, "", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  @Test
  void execute_shouldThrowExceptionWhenBookcaseIdIsNull() {
    assertThatThrownBy(() -> createShelfUseCase.execute(null, 1, "Shelf A", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Bookcase ID cannot be null");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  @Test
  void execute_shouldThrowExceptionWhenPositionIsNegative() {
    assertThatThrownBy(() -> createShelfUseCase.execute(100L, -1, "Shelf A", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf position must be greater than 0");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  @Test
  void execute_shouldThrowExceptionWhenPositionIsZero() {
    assertThatThrownBy(() -> createShelfUseCase.execute(100L, 0, "Shelf A", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf position must be greater than 0");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  @Test
  void execute_shouldCreateShelfWithMinimumBookCapacity() {
    createShelfUseCase.execute(100L, 1, "Shelf A", 1);

    verify(shelfDomainRepository).save(100L, 1, "Shelf A", 1);
  }

  @Test
  void execute_shouldCreateShelfWithLargeBookCapacity() {
    createShelfUseCase.execute(100L, 5, "Shelf Z", 1000);

    verify(shelfDomainRepository).save(100L, 5, "Shelf Z", 1000);
  }
}
