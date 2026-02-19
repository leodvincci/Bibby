package com.penrose.bibby.library.stacks.shelf.core.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.book.infrastructure.repository.BookJpaRepository;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
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

  @Mock private ShelfDomainRepository shelfDomainRepository;

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
   * Tests the {@link ShelfService#createShelf(Long, int, String, int)} method. Verifies that a
   * shelf is created with position zero (edge case).
   */
  @Test
  void createShelf_shouldNotCreateShelfWithPositionZero() {
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
    com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf shelf1 =
        mock(com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf.class);
    com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf shelf2 =
        mock(com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf.class);
    com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId shelfId1 =
        new com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId(1L);
    com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId shelfId2 =
        new com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId(2L);

    when(shelf1.getShelfId()).thenReturn(shelfId1);
    when(shelf2.getShelfId()).thenReturn(shelfId2);

    ShelfDomainRepository mockShelfDomainRepository = mock(ShelfDomainRepository.class);
    when(mockShelfDomainRepository.findByBookcaseId(bookcaseId))
        .thenReturn(java.util.List.of(shelf1, shelf2));
    when(mockShelfDomainRepository.getBookcaseIdByShelfId(1L)).thenReturn(bookcaseId);
    when(mockShelfDomainRepository.getBookcaseIdByShelfId(2L)).thenReturn(bookcaseId);

    ShelfService service =
        new ShelfService(shelfMapper, bookJpaRepository, mockShelfDomainRepository);

    com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO dto1 =
        new com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO(
            1L, "Shelf A", bookcaseId, 1, 10, java.util.List.of());
    com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO dto2 =
        new com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO(
            2L, "Shelf B", bookcaseId, 2, 15, java.util.List.of());

    when(shelfMapper.toDTO(shelf1, bookcaseId)).thenReturn(dto1);
    when(shelfMapper.toDTO(shelf2, bookcaseId)).thenReturn(dto2);

    java.util.List<com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO> result =
        service.getAllShelves(bookcaseId);

    org.assertj.core.api.Assertions.assertThat(result).hasSize(2).containsExactly(dto1, dto2);
    verify(mockShelfDomainRepository).findByBookcaseId(bookcaseId);
    verify(shelfMapper).toDTO(shelf1, bookcaseId);
    verify(shelfMapper).toDTO(shelf2, bookcaseId);
  }

  /**
   * Tests the {@link ShelfService#getAllShelves(Long)} method. Verifies that an empty list is
   * returned when bookcase has no shelves.
   */
  @Test
  void getAllShelves_shouldReturnEmptyListWhenBookcaseHasNoShelves() {
    Long bookcaseId = 100L;

    ShelfDomainRepository mockShelfDomainRepository = mock(ShelfDomainRepository.class);
    when(mockShelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(java.util.List.of());

    ShelfService service =
        new ShelfService(shelfMapper, bookJpaRepository, mockShelfDomainRepository);

    java.util.List<com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO> result =
        service.getAllShelves(bookcaseId);

    org.assertj.core.api.Assertions.assertThat(result).isEmpty();
    verify(mockShelfDomainRepository).findByBookcaseId(bookcaseId);
    verify(shelfMapper, never()).toDTO(any(), any());
  }

  /**
   * Tests the {@link ShelfService#getAllShelves(Long)} method. Verifies that the mapper is invoked
   * for each shelf with correct parameters.
   */
  @Test
  void getAllShelves_shouldInvokeMapperForEachShelf() {
    Long bookcaseId = 100L;
    com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf shelf =
        mock(com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf.class);
    com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId shelfId =
        new com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId(1L);

    when(shelf.getShelfId()).thenReturn(shelfId);

    ShelfDomainRepository mockShelfDomainRepository = mock(ShelfDomainRepository.class);
    when(mockShelfDomainRepository.findByBookcaseId(bookcaseId))
        .thenReturn(java.util.List.of(shelf));
    when(mockShelfDomainRepository.getBookcaseIdByShelfId(1L)).thenReturn(bookcaseId);

    ShelfService service =
        new ShelfService(shelfMapper, bookJpaRepository, mockShelfDomainRepository);

    com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO dto =
        new com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO(
            1L, "Shelf A", bookcaseId, 1, 10, java.util.List.of());
    when(shelfMapper.toDTO(shelf, bookcaseId)).thenReturn(dto);

    service.getAllShelves(bookcaseId);

    verify(mockShelfDomainRepository).getBookcaseIdByShelfId(1L);
    verify(shelfMapper).toDTO(shelf, bookcaseId);
  }
}
