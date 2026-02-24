package com.penrose.bibby.library.stacks.bookcase.core.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.BookcaseRepository;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.ShelfAccessPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CreateBookcaseUseCaseTest {

  @Mock private BookcaseRepository bookcaseRepository;

  @Mock private ShelfAccessPort shelfAccessPort;

  @InjectMocks private CreateBookcaseUseCase createBookcaseUseCase;

  @Test
  void createNewBookCase_shouldCreateNewBookcaseSuccessfully() {
    Long userId = 1L;
    String label = "BC001";
    String bookcaseZone = "A";
    String bookcaseZoneIndex = "1";
    int shelfCapacity = 5;
    int bookCapacity = 10;
    String location = "Living Room";

    Bookcase savedBookcase =
        new Bookcase(
            100L,
            userId,
            shelfCapacity,
            bookCapacity * shelfCapacity,
            location,
            bookcaseZone,
            bookcaseZoneIndex);
    when(bookcaseRepository.findBookcaseByBookcaseLocation(label)).thenReturn(null);
    when(bookcaseRepository.save(any(Bookcase.class))).thenReturn(savedBookcase);

    CreateBookcaseResult result =
        createBookcaseUseCase.createNewBookCase(
            userId, label, bookcaseZone, bookcaseZoneIndex, shelfCapacity, bookCapacity, location);

    assertThat(result).isNotNull();
    assertThat(result.bookcaseId()).isEqualTo(100L);
    verify(bookcaseRepository).findBookcaseByBookcaseLocation(label);
    verify(bookcaseRepository).save(any(Bookcase.class));
    verify(shelfAccessPort, times(shelfCapacity))
        .createShelf(eq(100L), anyInt(), anyString(), eq(bookCapacity));
  }

  @Test
  void createNewBookCase_shouldThrowExceptionWhenBookcaseAlreadyExists() {
    Long userId = 1L;
    String label = "BC001";
    String bookcaseZone = "A";
    String bookcaseZoneIndex = "1";
    int shelfCapacity = 5;
    int bookCapacity = 10;
    String location = "Living Room";

    Bookcase existingBookcase =
        new Bookcase(
            50L, userId, shelfCapacity, bookCapacity * shelfCapacity, location, null, null);
    when(bookcaseRepository.findBookcaseByBookcaseLocation(label)).thenReturn(existingBookcase);

    assertThatThrownBy(
            () ->
                createBookcaseUseCase.createNewBookCase(
                    userId,
                    label,
                    bookcaseZone,
                    bookcaseZoneIndex,
                    shelfCapacity,
                    bookCapacity,
                    location))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Bookcase with the label already exist");

    verify(bookcaseRepository).findBookcaseByBookcaseLocation(label);
    verify(bookcaseRepository, never()).save(any());
    verify(shelfAccessPort, never()).createShelf(anyLong(), anyInt(), anyString(), anyInt());
  }

  @Test
  void createNewBookCase_shouldCreateCorrectNumberOfShelves() {
    int shelfCapacity = 3;
    int bookCapacity = 10;

    Bookcase savedBookcase =
        new Bookcase(
            100L, 1L, shelfCapacity, bookCapacity * shelfCapacity, "Living Room", "A", "1");
    when(bookcaseRepository.findBookcaseByBookcaseLocation("BC001")).thenReturn(null);
    when(bookcaseRepository.save(any(Bookcase.class))).thenReturn(savedBookcase);

    createBookcaseUseCase.createNewBookCase(
        1L, "BC001", "A", "1", shelfCapacity, bookCapacity, "Living Room");

    verify(shelfAccessPort).createShelf(100L, 1, "Shelf 1", bookCapacity);
    verify(shelfAccessPort).createShelf(100L, 2, "Shelf 2", bookCapacity);
    verify(shelfAccessPort).createShelf(100L, 3, "Shelf 3", bookCapacity);
    verifyNoMoreInteractions(shelfAccessPort);
  }
}
