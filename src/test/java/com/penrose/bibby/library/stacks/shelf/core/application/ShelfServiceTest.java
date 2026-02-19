package com.penrose.bibby.library.stacks.shelf.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import java.util.List;
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

  @Mock private ShelfDomainRepository shelfDomainRepository;
  @Mock private BookFacade bookFacade;
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

    shelfService.createShelf(bookcaseId, position, shelfLabel, bookCapacity);

    verify(shelfDomainRepository).save(bookcaseId, position, shelfLabel, bookCapacity);
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

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
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

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
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

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
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

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
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

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when bookcase ID is null.
   */
  @Test
  void createShelf_shouldThrowExceptionWhenBookcaseIdIsNull() {
    assertThatThrownBy(() -> shelfService.createShelf(null, 1, "Shelf A", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Bookcase ID cannot be null");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
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

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that an
   * IllegalArgumentException is thrown when position is zero.
   */
  @Test
  void createShelf_shouldThrowExceptionWhenPositionIsZero() {
    assertThatThrownBy(() -> shelfService.createShelf(100L, 0, "Shelf A", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Shelf position must be greater than 0");

    verify(shelfDomainRepository, never())
        .save(any(), any(Integer.class), any(), any(Integer.class));
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created with minimum valid book capacity (1).
   */
  @Test
  void createShelf_shouldCreateShelfWithMinimumBookCapacity() {
    shelfService.createShelf(100L, 1, "Shelf A", 1);

    verify(shelfDomainRepository).save(100L, 1, "Shelf A", 1);
  }

  /**
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created with large book capacity.
   */
  @Test
  void createShelf_shouldCreateShelfWithLargeBookCapacity() {
    shelfService.createShelf(100L, 5, "Shelf Z", 1000);

    verify(shelfDomainRepository).save(100L, 5, "Shelf Z", 1000);
  }

  /**
   * Tests the {@link ShelfService#getAllShelves(Long)} method. Verifies that all shelves for a
   * bookcase are retrieved successfully.
   */
  @Test
  void getAllShelves_shouldReturnAllShelvesForBookcase() {
    Long bookcaseId = 100L;
    Shelf shelf1 = mock(Shelf.class);
    Shelf shelf2 = mock(Shelf.class);

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of(shelf1, shelf2));

    List<Shelf> result = shelfService.getAllShelves(bookcaseId);

    assertThat(result).hasSize(2).containsExactly(shelf1, shelf2);
    verify(shelfDomainRepository).findByBookcaseId(bookcaseId);
  }

  /**
   * Tests the {@link ShelfService#getAllShelves(Long)} method. Verifies that an empty list is
   * returned when bookcase has no shelves.
   */
  @Test
  void getAllShelves_shouldReturnEmptyListWhenBookcaseHasNoShelves() {
    Long bookcaseId = 100L;

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of());

    List<Shelf> result = shelfService.getAllShelves(bookcaseId);

    assertThat(result).isEmpty();
    verify(shelfDomainRepository).findByBookcaseId(bookcaseId);
  }

  /**
   * Tests the {@link ShelfService#getAllShelves(Long)} method. Verifies that the repository is
   * invoked with the correct bookcase ID.
   */
  @Test
  void getAllShelves_shouldDelegateToRepository() {
    Long bookcaseId = 100L;
    Shelf shelf = mock(Shelf.class);

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of(shelf));

    List<Shelf> result = shelfService.getAllShelves(bookcaseId);

    assertThat(result).containsExactly(shelf);
    verify(shelfDomainRepository).findByBookcaseId(bookcaseId);
  }

  /**
   * Tests the {@link ShelfService#getShelfOptions()} method. Verifies that all shelf options are
   * retrieved and mapped correctly.
   */
  @Test
  void getShelfOptions_shouldReturnAllShelfOptionsWithCorrectMapping() {
    Shelf shelf1 = mock(Shelf.class);
    Shelf shelf2 = mock(Shelf.class);

    when(shelfDomainRepository.findAll()).thenReturn(List.of(shelf1, shelf2));

    List<Shelf> result = shelfService.getShelfOptions();

    assertThat(result).hasSize(2).containsExactly(shelf1, shelf2);
    verify(shelfDomainRepository).findAll();
  }

  /**
   * Tests the {@link ShelfService#getShelfOptions()} method. Verifies that an empty list is
   * returned when no shelves exist.
   */
  @Test
  void getShelfOptions_shouldReturnEmptyListWhenNoShelvesExist() {
    when(shelfDomainRepository.findAll()).thenReturn(List.of());

    List<Shelf> result = shelfService.getShelfOptions();

    assertThat(result).isEmpty();
    verify(shelfDomainRepository).findAll();
  }

  /**
   * Tests the {@link ShelfService#getShelfOptionsByBookcase(Long)} method. Verifies that shelf
   * options for a specific bookcase are retrieved correctly.
   */
  @Test
  void getShelfOptionsByBookcase_shouldReturnShelfOptionsForSpecificBookcase() {
    Long bookcaseId = 100L;
    Shelf shelf1 = mock(Shelf.class);
    Shelf shelf2 = mock(Shelf.class);

    when(shelfDomainRepository.getShelfShelfOptionResponse(bookcaseId))
        .thenReturn(List.of(shelf1, shelf2));

    List<Shelf> result = shelfService.getShelfOptionsByBookcase(bookcaseId);

    assertThat(result).hasSize(2).containsExactly(shelf1, shelf2);
    verify(shelfDomainRepository).getShelfShelfOptionResponse(bookcaseId);
  }

  /**
   * Tests the {@link ShelfService#getShelfOptionsByBookcase(Long)} method. Verifies that an empty
   * list is returned when bookcase has no shelves.
   */
  @Test
  void getShelfOptionsByBookcase_shouldReturnEmptyListWhenBookcaseHasNoShelves() {
    Long bookcaseId = 100L;

    when(shelfDomainRepository.getShelfShelfOptionResponse(bookcaseId)).thenReturn(List.of());

    List<Shelf> result = shelfService.getShelfOptionsByBookcase(bookcaseId);

    assertThat(result).isEmpty();
    verify(shelfDomainRepository).getShelfShelfOptionResponse(bookcaseId);
  }
}
