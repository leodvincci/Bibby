package com.penrose.bibby.library.stacks.shelf.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
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

  @Mock private ShelfMapper shelfMapper;
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
    ShelfId shelfId1 = new ShelfId(1L);
    ShelfId shelfId2 = new ShelfId(2L);

    when(shelf1.getShelfId()).thenReturn(shelfId1);
    when(shelf2.getShelfId()).thenReturn(shelfId2);

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of(shelf1, shelf2));
    when(shelfDomainRepository.getBookcaseIdByShelfId(1L)).thenReturn(bookcaseId);
    when(shelfDomainRepository.getBookcaseIdByShelfId(2L)).thenReturn(bookcaseId);

    ShelfDTO dto1 = new ShelfDTO(1L, "Shelf A", bookcaseId, 1, 10, List.of());
    ShelfDTO dto2 = new ShelfDTO(2L, "Shelf B", bookcaseId, 2, 15, List.of());

    when(shelfMapper.toDTO(shelf1, bookcaseId)).thenReturn(dto1);
    when(shelfMapper.toDTO(shelf2, bookcaseId)).thenReturn(dto2);

    List<ShelfDTO> result = shelfService.getAllShelves(bookcaseId);

    assertThat(result).hasSize(2).containsExactly(dto1, dto2);
    verify(shelfDomainRepository).findByBookcaseId(bookcaseId);
    verify(shelfDomainRepository).getBookcaseIdByShelfId(1L);
    verify(shelfDomainRepository).getBookcaseIdByShelfId(2L);
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

    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of());

    List<ShelfDTO> result = shelfService.getAllShelves(bookcaseId);

    assertThat(result).isEmpty();
    verify(shelfDomainRepository).findByBookcaseId(bookcaseId);
    verify(shelfMapper, never()).toDTO(any(), any());
  }

  /**
   * Tests the {@link ShelfService#getAllShelves(Long)} method. Verifies that the mapper is invoked
   * for each shelf with correct parameters.
   */
  @Test
  void getAllShelves_shouldInvokeMapperForEachShelf() {
    Long bookcaseId = 100L;
    Shelf shelf = mock(Shelf.class);
    ShelfId shelfId = new ShelfId(1L);

    when(shelf.getShelfId()).thenReturn(shelfId);
    when(shelfDomainRepository.findByBookcaseId(bookcaseId)).thenReturn(List.of(shelf));
    when(shelfDomainRepository.getBookcaseIdByShelfId(1L)).thenReturn(bookcaseId);

    ShelfDTO dto = new ShelfDTO(1L, "Shelf A", bookcaseId, 1, 10, List.of());
    when(shelfMapper.toDTO(shelf, bookcaseId)).thenReturn(dto);

    shelfService.getAllShelves(bookcaseId);

    verify(shelfDomainRepository).getBookcaseIdByShelfId(1L);
    verify(shelfMapper).toDTO(shelf, bookcaseId);
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

    ShelfOptionResponse option1 = new ShelfOptionResponse(1L, "Shelf A", 10, 5, true);
    ShelfOptionResponse option2 = new ShelfOptionResponse(2L, "Shelf B", 15, 15, false);

    when(shelfMapper.toShelfOption(shelf1)).thenReturn(option1);
    when(shelfMapper.toShelfOption(shelf2)).thenReturn(option2);

    List<ShelfOptionResponse> result = shelfService.getShelfOptions();

    assertThat(result).hasSize(2).containsExactly(option1, option2);
    verify(shelfDomainRepository).findAll();
    verify(shelfMapper).toShelfOption(shelf1);
    verify(shelfMapper).toShelfOption(shelf2);
  }

  /**
   * Tests the {@link ShelfService#getShelfOptions()} method. Verifies that an empty list is
   * returned when no shelves exist.
   */
  @Test
  void getShelfOptions_shouldReturnEmptyListWhenNoShelvesExist() {
    when(shelfDomainRepository.findAll()).thenReturn(List.of());

    List<ShelfOptionResponse> result = shelfService.getShelfOptions();

    assertThat(result).isEmpty();
    verify(shelfDomainRepository).findAll();
    verify(shelfMapper, never()).toShelfOption(any());
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

    ShelfOptionResponse option1 = new ShelfOptionResponse(1L, "Shelf A", 10, 5, true);
    ShelfOptionResponse option2 = new ShelfOptionResponse(2L, "Shelf B", 15, 10, true);

    when(shelfMapper.toShelfOption(shelf1)).thenReturn(option1);
    when(shelfMapper.toShelfOption(shelf2)).thenReturn(option2);

    List<ShelfOptionResponse> result = shelfService.getShelfOptionsByBookcase(bookcaseId);

    assertThat(result).hasSize(2).containsExactly(option1, option2);
    verify(shelfDomainRepository).getShelfShelfOptionResponse(bookcaseId);
    verify(shelfMapper).toShelfOption(shelf1);
    verify(shelfMapper).toShelfOption(shelf2);
  }

  /**
   * Tests the {@link ShelfService#getShelfOptionsByBookcase(Long)} method. Verifies that an empty
   * list is returned when bookcase has no shelves.
   */
  @Test
  void getShelfOptionsByBookcase_shouldReturnEmptyListWhenBookcaseHasNoShelves() {
    Long bookcaseId = 100L;

    when(shelfDomainRepository.getShelfShelfOptionResponse(bookcaseId)).thenReturn(List.of());

    List<ShelfOptionResponse> result = shelfService.getShelfOptionsByBookcase(bookcaseId);

    assertThat(result).isEmpty();
    verify(shelfDomainRepository).getShelfShelfOptionResponse(bookcaseId);
    verify(shelfMapper, never()).toShelfOption(any());
  }
}
