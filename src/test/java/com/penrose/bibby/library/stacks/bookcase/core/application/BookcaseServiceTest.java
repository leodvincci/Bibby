package com.penrose.bibby.library.stacks.bookcase.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.api.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.repository.BookcaseJpaRepository;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.repository.BookcaseRepository;
import com.penrose.bibby.library.stacks.shelf.api.ports.inbound.ShelfFacade;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Test class for {@link BookcaseService}. Tests the business logic for managing bookcases in the
 * library system.
 */
@ExtendWith(MockitoExtension.class)
class BookcaseServiceTest {

  @Mock private BookcaseRepository bookcaseRepository;

  @Mock private BookcaseJpaRepository bookcaseJpaRepository;

  @Mock private ShelfFacade shelfFacade;

  @InjectMocks private BookcaseService bookcaseService;

  /**
   * Tests the {@link BookcaseService#addShelf(BookcaseEntity, int, int, int)} method. Verifies that
   * a shelf is created with correct parameters.
   */
  @Test
  void addShelf_shouldCallShelfFacadeWithCorrectParameters() {
    BookcaseEntity bookcaseEntity = new BookcaseEntity(1L, "Living Room", "A", "1", 5, 50);
    bookcaseEntity.setBookcaseId(100L);
    int label = 1;
    int position = 1;
    int bookCapacity = 10;

    bookcaseService.addShelf(bookcaseEntity, label, position, bookCapacity);

    verify(shelfFacade).createShelf(eq(100L), eq(position), eq("Shelf " + label), eq(bookCapacity));
  }

  /**
   * Tests the {@link BookcaseService#addShelf(BookcaseEntity, int, int, int)} method. Verifies that
   * a shelf is created with different label and position values.
   */
  @Test
  void addShelf_shouldCallShelfFacadeWithDifferentLabelAndPosition() {
    BookcaseEntity bookcaseEntity = new BookcaseEntity(2L, "Study Room", "B", "2", 3, 30);
    bookcaseEntity.setBookcaseId(200L);
    int label = 3;
    int position = 3;
    int bookCapacity = 15;

    bookcaseService.addShelf(bookcaseEntity, label, position, bookCapacity);

    verify(shelfFacade).createShelf(eq(200L), eq(position), eq("Shelf " + label), eq(bookCapacity));
  }

  /**
   * Tests the {@link BookcaseService#addShelf(BookcaseEntity, int, int, int)} method. Verifies that
   * the shelf label is correctly formatted in the shelf name.
   */
  @Test
  void addShelf_shouldFormatShelfNameCorrectly() {
    BookcaseEntity bookcaseEntity = new BookcaseEntity(3L, "Bedroom", "C", "3", 4, 40);
    bookcaseEntity.setBookcaseId(300L);
    int label = 5;
    int position = 2;
    int bookCapacity = 20;

    bookcaseService.addShelf(bookcaseEntity, label, position, bookCapacity);

    verify(shelfFacade).createShelf(eq(300L), eq(position), eq("Shelf 5"), eq(bookCapacity));
  }

  /**
   * Tests the {@link BookcaseService#addShelf(BookcaseEntity, int, int, int)} method. Verifies that
   * a shelf is created with minimum valid book capacity.
   */
  @Test
  void addShelf_shouldHandleMinimumBookCapacity() {
    BookcaseEntity bookcaseEntity = new BookcaseEntity(4L, "Office", "D", "1", 2, 20);
    bookcaseEntity.setBookcaseId(400L);
    int label = 1;
    int position = 1;
    int bookCapacity = 1;

    bookcaseService.addShelf(bookcaseEntity, label, position, bookCapacity);

    verify(shelfFacade).createShelf(eq(400L), eq(position), eq("Shelf 1"), eq(bookCapacity));
  }

  /**
   * Tests the {@link BookcaseService#addShelf(BookcaseEntity, int, int, int)} method. Verifies that
   * a shelf is created with large book capacity.
   */
  @Test
  void addShelf_shouldHandleLargeBookCapacity() {
    BookcaseEntity bookcaseEntity = new BookcaseEntity(5L, "Library", "E", "5", 10, 1000);
    bookcaseEntity.setBookcaseId(500L);
    int label = 8;
    int position = 8;
    int bookCapacity = 100;

    bookcaseService.addShelf(bookcaseEntity, label, position, bookCapacity);

    verify(shelfFacade).createShelf(eq(500L), eq(position), eq("Shelf 8"), eq(bookCapacity));
  }

  @Test
  void addShelf_shouldNotCreateShelfWithNegativeBookCapacity() {
    BookcaseEntity bookcaseEntity = new BookcaseEntity(6L, "Basement", "F", "1", 3, 30);
    bookcaseEntity.setBookcaseId(600L);
    int label = 2;
    int position = 2;
    int bookCapacity = -5;

    assertThatThrownBy(
            () -> bookcaseService.addShelf(bookcaseEntity, label, position, bookCapacity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Book capacity cannot be negative");

    verify(shelfFacade, never()).createShelf(anyLong(), anyInt(), anyString(), anyInt());
  }

  @Test
  void addShelf_shouldNotCreateShelfWithNullBookcase() {
    BookcaseEntity bookcaseEntity = null;
    // bookcaseId is not set, remains null
    int label = 3;
    int position = 3;
    int bookCapacity = 20;

    assertThatThrownBy(
            () -> bookcaseService.addShelf(bookcaseEntity, label, position, bookCapacity))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Bookcase cannot be null");

    verify(shelfFacade, never()).createShelf(anyLong(), anyInt(), anyString(), anyInt());
  }

  /**
   * Tests the {@link BookcaseService#createNewBookCase(Long, String, String, String, int, int,
   * String)} method. Verifies that a new bookcase is created successfully when no existing bookcase
   * with the same location exists.
   */
  @Test
  void createNewBookCase_shouldCreateNewBookcaseSuccessfully() {
    Long userId = 1L;
    String label = "BC001";
    String bookcaseZone = "A";
    String bookcaseZoneIndex = "1";
    int shelfCapacity = 5;
    int bookCapacity = 10;
    String location = "Living Room";

    BookcaseEntity savedEntity =
        new BookcaseEntity(
            userId,
            location,
            bookcaseZone,
            bookcaseZoneIndex,
            shelfCapacity,
            bookCapacity * shelfCapacity);
    savedEntity.setBookcaseId(100L);

    when(bookcaseRepository.findBookcaseEntityByBookcaseLocation(label)).thenReturn(null);
    when(bookcaseRepository.save(any(BookcaseEntity.class))).thenReturn(savedEntity);

    CreateBookcaseResult result =
        bookcaseService.createNewBookCase(
            userId, label, bookcaseZone, bookcaseZoneIndex, shelfCapacity, bookCapacity, location);

    assertThat(result).isNotNull();
    assertThat(result.bookcaseId()).isEqualTo(100L);
    verify(bookcaseRepository).findBookcaseEntityByBookcaseLocation(label);
    verify(bookcaseRepository).save(any(BookcaseEntity.class));
    verify(shelfFacade, times(shelfCapacity))
        .createShelf(eq(100L), anyInt(), anyString(), eq(bookCapacity));
  }

  /**
   * Tests the {@link BookcaseService#createNewBookCase(Long, String, String, String, int, int,
   * String)} method. Verifies that a ResponseStatusException is thrown when a bookcase with the
   * same location already exists.
   */
  @Test
  void createNewBookCase_shouldThrowExceptionWhenBookcaseAlreadyExists() {
    Long userId = 1L;
    String label = "BC001";
    String bookcaseZone = "A";
    String bookcaseZoneIndex = "1";
    int shelfCapacity = 5;
    int bookCapacity = 10;
    String location = "Living Room";

    BookcaseEntity existingEntity =
        new BookcaseEntity(
            userId,
            location,
            bookcaseZone,
            bookcaseZoneIndex,
            shelfCapacity,
            bookCapacity * shelfCapacity);
    when(bookcaseRepository.findBookcaseEntityByBookcaseLocation(label)).thenReturn(existingEntity);

    assertThatThrownBy(
            () ->
                bookcaseService.createNewBookCase(
                    userId,
                    label,
                    bookcaseZone,
                    bookcaseZoneIndex,
                    shelfCapacity,
                    bookCapacity,
                    location))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Bookcase with the label already exist");

    verify(bookcaseRepository).findBookcaseEntityByBookcaseLocation(label);
    verify(bookcaseRepository, never()).save(any(BookcaseEntity.class));
    verify(shelfFacade, never()).createShelf(anyLong(), anyInt(), anyString(), anyInt());
  }

  /**
   * Tests the {@link BookcaseService#getAllBookcaseLocations()} method. Verifies that all bookcase
   * locations are retrieved successfully.
   */
  @Test
  void getAllBookcaseLocations_shouldReturnAllLocations() {
    List<String> expectedLocations = Arrays.asList("Living Room", "Study Room", "Bedroom");
    when(bookcaseRepository.getAllBookCaseLocations()).thenReturn(expectedLocations);

    List<String> result = bookcaseService.getAllBookcaseLocations();

    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyElementsOf(expectedLocations);
    verify(bookcaseRepository).getAllBookCaseLocations();
  }

  /**
   * Tests the {@link BookcaseService#findById(Long)} method. Verifies that a bookcase is found by
   * its ID.
   */
  @Test
  void findById_shouldReturnBookcaseWhenExists() {
    Long bookcaseId = 100L;
    BookcaseEntity entity = new BookcaseEntity(1L, "Living Room", "A", "1", 5, 50);
    entity.setBookcaseId(bookcaseId);

    when(bookcaseRepository.findById(bookcaseId)).thenReturn(Optional.of(entity));

    Optional<BookcaseEntity> result = bookcaseService.findById(bookcaseId);

    assertThat(result).isPresent();
    assertThat(result.get().getBookcaseId()).isEqualTo(bookcaseId);
    verify(bookcaseRepository).findById(bookcaseId);
  }

  /**
   * Tests the {@link BookcaseService#findById(Long)} method. Verifies that an empty Optional is
   * returned when bookcase does not exist.
   */
  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    Long bookcaseId = 999L;
    when(bookcaseRepository.findById(bookcaseId)).thenReturn(Optional.empty());

    Optional<BookcaseEntity> result = bookcaseService.findById(bookcaseId);

    assertThat(result).isEmpty();
    verify(bookcaseRepository).findById(bookcaseId);
  }

  /**
   * Tests the {@link BookcaseService#getAllBookcasesByLocation(String)} method. Verifies that all
   * bookcases are retrieved by location.
   */
  @Test
  void getAllBookcasesByLocation_shouldReturnBookcasesForLocation() {
    String location = "Living Room";
    BookcaseEntity entity1 = new BookcaseEntity(1L, location, "A", "1", 5, 50);
    entity1.setBookcaseId(100L);
    BookcaseEntity entity2 = new BookcaseEntity(1L, location, "B", "2", 4, 40);
    entity2.setBookcaseId(200L);

    when(bookcaseRepository.findByLocation(location)).thenReturn(Arrays.asList(entity1, entity2));

    List<BookcaseDTO> result = bookcaseService.getAllBookcasesByLocation(location);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).bookcaseId()).isEqualTo(100L);
    assertThat(result.get(1).bookcaseId()).isEqualTo(200L);
    verify(bookcaseRepository).findByLocation(location);
  }

  /**
   * Tests the {@link BookcaseService#getAllBookcasesByLocation(String)} method. Verifies that an
   * empty list is returned when no bookcases exist for the location.
   */
  @Test
  void getAllBookcasesByLocation_shouldReturnEmptyListWhenNoBookcasesExist() {
    String location = "Garage";
    when(bookcaseRepository.findByLocation(location)).thenReturn(Arrays.asList());

    List<BookcaseDTO> result = bookcaseService.getAllBookcasesByLocation(location);

    assertThat(result).isEmpty();
    verify(bookcaseRepository).findByLocation(location);
  }

  /**
   * Tests the {@link BookcaseService#getAllBookcasesByUserId(Long)} method. Verifies that all
   * bookcases are retrieved by user ID.
   */
  @Test
  void getAllBookcasesByUserId_shouldReturnBookcasesForUser() {
    Long userId = 1L;
    BookcaseEntity entity1 = new BookcaseEntity(userId, "Living Room", "A", "1", 5, 50);
    entity1.setBookcaseId(100L);
    BookcaseEntity entity2 = new BookcaseEntity(userId, "Bedroom", "B", "2", 3, 30);
    entity2.setBookcaseId(200L);

    when(bookcaseRepository.findByAppUserId(userId)).thenReturn(Arrays.asList(entity1, entity2));

    List<BookcaseDTO> result = bookcaseService.getAllBookcasesByUserId(userId);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).bookcaseId()).isEqualTo(100L);
    assertThat(result.get(1).bookcaseId()).isEqualTo(200L);
    verify(bookcaseRepository).findByAppUserId(userId);
  }

  /**
   * Tests the {@link BookcaseService#getAllBookcasesByUserId(Long)} method. Verifies that an empty
   * list is returned when user has no bookcases.
   */
  @Test
  void getAllBookcasesByUserId_shouldReturnEmptyListWhenUserHasNoBookcases() {
    Long userId = 999L;
    when(bookcaseRepository.findByAppUserId(userId)).thenReturn(Arrays.asList());

    List<BookcaseDTO> result = bookcaseService.getAllBookcasesByUserId(userId);

    assertThat(result).isEmpty();
    verify(bookcaseRepository).findByAppUserId(userId);
  }

  /**
   * Tests the {@link BookcaseService#deleteBookcase(Long)} method. Verifies that a bookcase is
   * deleted successfully.
   */
  @Test
  void deleteBookcase_shouldDeleteBookcaseSuccessfully() {
    Long bookcaseId = 100L;

    bookcaseService.deleteBookcase(bookcaseId);

    verify(bookcaseJpaRepository).deleteById(bookcaseId);
  }

  /**
   * Tests the {@link BookcaseService#getAllBookcases()} method. Verifies that all bookcases are
   * retrieved.
   */
  @Test
  void getAllBookcases_shouldReturnAllBookcases() {
    BookcaseEntity entity1 = new BookcaseEntity(1L, "Living Room", "A", "1", 5, 50);
    entity1.setBookcaseId(100L);
    BookcaseEntity entity2 = new BookcaseEntity(2L, "Study Room", "B", "2", 4, 40);
    entity2.setBookcaseId(200L);
    BookcaseEntity entity3 = new BookcaseEntity(1L, "Bedroom", "C", "3", 3, 30);
    entity3.setBookcaseId(300L);

    when(bookcaseRepository.findAll()).thenReturn(Arrays.asList(entity1, entity2, entity3));

    List<BookcaseDTO> result = bookcaseService.getAllBookcases();

    assertThat(result).hasSize(3);
    assertThat(result.get(0).bookcaseId()).isEqualTo(100L);
    assertThat(result.get(1).bookcaseId()).isEqualTo(200L);
    assertThat(result.get(2).bookcaseId()).isEqualTo(300L);
    verify(bookcaseRepository).findAll();
  }

  /**
   * Tests the {@link BookcaseService#getAllBookcases()} method. Verifies that an empty list is
   * returned when no bookcases exist.
   */
  @Test
  void getAllBookcases_shouldReturnEmptyListWhenNoBookcasesExist() {
    when(bookcaseRepository.findAll()).thenReturn(Arrays.asList());

    List<BookcaseDTO> result = bookcaseService.getAllBookcases();

    assertThat(result).isEmpty();
    verify(bookcaseRepository).findAll();
  }

  /**
   * Tests the {@link BookcaseService#findBookCaseById(Long)} method. Verifies that a BookcaseDTO is
   * returned when bookcase exists.
   */
  @Test
  void findBookCaseById_shouldReturnBookcaseDTOWhenExists() {
    Long bookcaseId = 100L;
    BookcaseEntity entity = new BookcaseEntity(1L, "Living Room", "A", "1", 5, 50);
    entity.setBookcaseId(bookcaseId);

    when(bookcaseRepository.findById(bookcaseId)).thenReturn(Optional.of(entity));

    Optional<BookcaseDTO> result = bookcaseService.findBookCaseById(bookcaseId);

    assertThat(result).isPresent();
    assertThat(result.get().bookcaseId()).isEqualTo(bookcaseId);
    assertThat(result.get().location()).isEqualTo("Living Room");
    verify(bookcaseRepository).findById(bookcaseId);
  }

  /**
   * Tests the {@link BookcaseService#findBookCaseById(Long)} method. Verifies that an empty
   * Optional is returned when bookcase does not exist.
   */
  @Test
  void findBookCaseById_shouldReturnEmptyWhenNotExists() {
    Long bookcaseId = 999L;
    when(bookcaseRepository.findById(bookcaseId)).thenReturn(Optional.empty());

    Optional<BookcaseDTO> result = bookcaseService.findBookCaseById(bookcaseId);

    assertThat(result).isEmpty();
    verify(bookcaseRepository).findById(bookcaseId);
  }
}
