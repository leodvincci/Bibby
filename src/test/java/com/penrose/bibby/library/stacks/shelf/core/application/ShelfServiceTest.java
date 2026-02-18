package com.penrose.bibby.library.stacks.shelf.core.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
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

  @Mock private BookJpaRepository bookJpaRepository;

  @Mock private ShelfMapper shelfMapper;

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

    ShelfEntity savedShelfEntity = new ShelfEntity();
    savedShelfEntity.setShelfId(1L);
    savedShelfEntity.setBookcaseId(bookcaseId);
    savedShelfEntity.setShelfPosition(position);
    savedShelfEntity.setShelfLabel(shelfLabel);
    savedShelfEntity.setBookCapacity(bookCapacity);

    when(shelfJpaRepository.save(any(ShelfEntity.class))).thenReturn(savedShelfEntity);

    shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity);

    verify(shelfJpaRepository)
        .save(
            argThat(
                shelf ->
                    shelf.getBookcaseId().equals(bookcaseId)
                        && shelf.getShelfPosition() == position
                        && shelf.getShelfLabel().equals(shelfLabel)
                        && shelf.getBookCapacity() == bookCapacity));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when book capacity is zero (business rule: capacity must be
   * at least 1).
   */
  @Test
  void createShelf_shouldThrowExceptionWhenBookCapacityIsZero() {
    assertThatThrownBy(() -> shelfService.createShelf(100L, 1, "Shelf A", 0))
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
    assertThatThrownBy(() -> shelfService.createShelf(100L, 1, "Shelf A", -5))
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
    assertThatThrownBy(() -> shelfService.createShelf(100L, 1, null, 10))
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
    assertThatThrownBy(() -> shelfService.createShelf(100L, 1, "   ", 10))
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
    assertThatThrownBy(() -> shelfService.createShelf(100L, 1, "", 10))
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
    assertThatThrownBy(() -> shelfService.createShelf(100L, -1, "Shelf A", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf position must be greater than 0");

    verify(shelfJpaRepository, never()).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created with position zero (edge case).
   */
  @Test
  void createShelf_shouldNotCreateShelfWithPositionZero() {
    assertThatThrownBy(() -> shelfService.createShelf(100L, 0, "Shelf A", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf position must be greater than 0");

    verify(shelfJpaRepository, never()).save(any(ShelfEntity.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created with minimum valid book capacity (1).
   */
  @Test
  void createShelf_shouldCreateShelfWithMinimumBookCapacity() {
    when(shelfJpaRepository.save(any(ShelfEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    shelfService.createShelf(100L, 1, "Shelf A", 1);

    verify(shelfJpaRepository).save(argThat(shelf -> shelf.getBookCapacity() == 1));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created with large book capacity.
   */
  @Test
  void createShelf_shouldCreateShelfWithLargeBookCapacity() {
    when(shelfJpaRepository.save(any(ShelfEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    shelfService.createShelf(100L, 5, "Shelf Z", 1000);

    verify(shelfJpaRepository)
        .save(
            argThat(
                shelf ->
                    shelf.getBookCapacity() == 1000
                        && shelf.getShelfPosition() == 5
                        && shelf.getShelfLabel().equals("Shelf Z")));
  }
}
