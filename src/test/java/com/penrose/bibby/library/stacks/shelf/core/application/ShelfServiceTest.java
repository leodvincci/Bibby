package com.penrose.bibby.library.stacks.shelf.core.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.bookcase.api.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link ShelfService}. Tests the business logic for managing shelves in the library
 * system.
 */
@ExtendWith(MockitoExtension.class)
class ShelfServiceTest {

  @Mock private ShelfJpaRepository shelfJpaRepository;

  @Mock private BookcaseFacade bookcaseFacade;

  @InjectMocks private ShelfService shelfService;

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created successfully with valid parameters.
   */
  @Test
  void createShelf_shouldCreateShelfSuccessfully() {
    Long bookcaseId = 100L;
    int position = 1;
    String shelfLabel = "Shelf A";
    int bookCapacity = 10;

    BookcaseEntity bookcaseEntity = new BookcaseEntity(1L, "Living Room", "A", "1", 5, 50);
    bookcaseEntity.setBookcaseId(bookcaseId);

    ShelfEntity savedShelfEntity = new ShelfEntity();
    savedShelfEntity.setShelfId(1L);
    savedShelfEntity.setBookcaseId(bookcaseId);
    savedShelfEntity.setShelfPosition(position);
    savedShelfEntity.setShelfLabel(shelfLabel);
    savedShelfEntity.setBookCapacity(bookCapacity);

    when(bookcaseFacade.findById(bookcaseId)).thenReturn(Optional.of(bookcaseEntity));
    when(shelfJpaRepository.save(any(ShelfEntity.class))).thenReturn(savedShelfEntity);

    shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity);

    verify(bookcaseFacade).findById(bookcaseId);
    verify(shelfJpaRepository).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when book capacity is zero.
   */
  @Test
  void createShelf_shouldThrowExceptionWhenBookCapacityIsZero() {
    Long bookcaseId = 100L;
    int position = 1;
    String shelfLabel = "Shelf A";
    int bookCapacity = 0;

    assertThatThrownBy(
            () -> shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Book capacity cannot be negative");

    verify(shelfJpaRepository, never()).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when book capacity is negative.
   */
  @Test
  void createShelf_shouldThrowExceptionWhenBookCapacityIsNegative() {
    Long bookcaseId = 100L;
    int position = 1;
    String shelfLabel = "Shelf A";
    int bookCapacity = -5;

    assertThatThrownBy(
            () -> shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Book capacity cannot be negative");

    verify(shelfJpaRepository, never()).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when shelf label is null.
   */
  @Test
  void createShelf_shouldThrowExceptionWhenShelfLabelIsNull() {
    Long bookcaseId = 100L;
    int position = 1;
    String shelfLabel = null;
    int bookCapacity = 10;

    assertThatThrownBy(
            () -> shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");

    verify(shelfJpaRepository, never()).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when shelf label is blank.
   */
  @Test
  void createShelf_shouldThrowExceptionWhenShelfLabelIsBlank() {
    Long bookcaseId = 100L;
    int position = 1;
    String shelfLabel = "   ";
    int bookCapacity = 10;

    assertThatThrownBy(
            () -> shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");

    verify(shelfJpaRepository, never()).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when shelf label is empty.
   */
  @Test
  void createShelf_shouldThrowExceptionWhenShelfLabelIsEmpty() {
    Long bookcaseId = 100L;
    int position = 1;
    String shelfLabel = "";
    int bookCapacity = 10;

    assertThatThrownBy(
            () -> shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf label cannot be null or blank");

    verify(shelfJpaRepository, never()).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when position is negative.
   */
  @Test
  void createShelf_shouldThrowExceptionWhenPositionIsNegative() {
    Long bookcaseId = 100L;
    int position = -1;
    String shelfLabel = "Shelf A";
    int bookCapacity = 10;

    assertThatThrownBy(
            () -> shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf position cannot be negative");

    verify(shelfJpaRepository, never()).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when bookcase does not exist.
   */
  @Test
  void createShelf_shouldThrowExceptionWhenBookcaseDoesNotExist() {
    Long bookcaseId = 999L;
    int position = 1;
    String shelfLabel = "Shelf A";
    int bookCapacity = 10;

    when(bookcaseFacade.findById(bookcaseId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Bookcase with ID: " + bookcaseId + " does not exist");

    verify(bookcaseFacade).findById(bookcaseId);
    verify(shelfJpaRepository, never()).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created with position zero (edge case).
   */
  @Test
  void createShelf_shouldCreateShelfWithPositionZero() {
    Long bookcaseId = 100L;
    int position = 0;
    String shelfLabel = "Shelf A";
    int bookCapacity = 10;

    BookcaseEntity bookcaseEntity = new BookcaseEntity(1L, "Living Room", "A", "1", 5, 50);
    bookcaseEntity.setBookcaseId(bookcaseId);

    ShelfEntity savedShelfEntity = new ShelfEntity();
    savedShelfEntity.setShelfId(1L);
    savedShelfEntity.setBookcaseId(bookcaseId);
    savedShelfEntity.setShelfPosition(position);
    savedShelfEntity.setShelfLabel(shelfLabel);
    savedShelfEntity.setBookCapacity(bookCapacity);

    when(bookcaseFacade.findById(bookcaseId)).thenReturn(Optional.of(bookcaseEntity));
    when(shelfJpaRepository.save(any(ShelfEntity.class))).thenReturn(savedShelfEntity);

    shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity);

    verify(bookcaseFacade).findById(bookcaseId);
    verify(shelfJpaRepository).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created with minimum valid book capacity (1).
   */
  @Test
  void createShelf_shouldCreateShelfWithMinimumBookCapacity() {
    Long bookcaseId = 100L;
    int position = 1;
    String shelfLabel = "Shelf A";
    int bookCapacity = 1;

    BookcaseEntity bookcaseEntity = new BookcaseEntity(1L, "Living Room", "A", "1", 5, 50);
    bookcaseEntity.setBookcaseId(bookcaseId);

    ShelfEntity savedShelfEntity = new ShelfEntity();
    savedShelfEntity.setShelfId(1L);
    savedShelfEntity.setBookcaseId(bookcaseId);
    savedShelfEntity.setShelfPosition(position);
    savedShelfEntity.setShelfLabel(shelfLabel);
    savedShelfEntity.setBookCapacity(bookCapacity);

    when(bookcaseFacade.findById(bookcaseId)).thenReturn(Optional.of(bookcaseEntity));
    when(shelfJpaRepository.save(any(ShelfEntity.class))).thenReturn(savedShelfEntity);

    shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity);

    verify(bookcaseFacade).findById(bookcaseId);
    verify(shelfJpaRepository).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created with large book capacity.
   */
  @Test
  void createShelf_shouldCreateShelfWithLargeBookCapacity() {
    Long bookcaseId = 100L;
    int position = 5;
    String shelfLabel = "Shelf Z";
    int bookCapacity = 1000;

    BookcaseEntity bookcaseEntity = new BookcaseEntity(1L, "Library", "E", "5", 10, 10000);
    bookcaseEntity.setBookcaseId(bookcaseId);

    ShelfEntity savedShelfEntity = new ShelfEntity();
    savedShelfEntity.setShelfId(1L);
    savedShelfEntity.setBookcaseId(bookcaseId);
    savedShelfEntity.setShelfPosition(position);
    savedShelfEntity.setShelfLabel(shelfLabel);
    savedShelfEntity.setBookCapacity(bookCapacity);

    when(bookcaseFacade.findById(bookcaseId)).thenReturn(Optional.of(bookcaseEntity));
    when(shelfJpaRepository.save(any(ShelfEntity.class))).thenReturn(savedShelfEntity);

    shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity);

    verify(bookcaseFacade).findById(bookcaseId);
    verify(shelfJpaRepository).save(any(ShelfEntity.class));
  }
}
