package com.penrose.bibby.library.stacks.bookcase.core.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.BookcaseRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryBookcaseUseCaseTest {

  @Mock private BookcaseRepository bookcaseRepository;

  @InjectMocks private QueryBookcaseUseCase queryBookcaseUseCase;

  @Test
  void findBookCaseById_shouldReturnBookcaseWhenExists() {
    Bookcase bookcase = new Bookcase(100L, 5, 50, "Living Room", null, null);
    when(bookcaseRepository.findById(100L)).thenReturn(bookcase);

    Bookcase result = queryBookcaseUseCase.findBookCaseById(100L);

    assertThat(result).isNotNull();
    assertThat(result.getBookcaseId()).isEqualTo(100L);
    assertThat(result.getBookcaseLocation()).isEqualTo("Living Room");
    verify(bookcaseRepository).findById(100L);
  }

  @Test
  void findBookCaseById_shouldReturnEmptyWhenNotExists() {
    when(bookcaseRepository.findById(999L)).thenReturn(null);

    Bookcase result = queryBookcaseUseCase.findBookCaseById(999L);

    assertThat(result).isNull();
    verify(bookcaseRepository).findById(999L);
  }

  @Test
  void getAllBookcases_shouldReturnAllBookcases() {
    Bookcase bookcase1 = new Bookcase(100L, 5, 50, "Living Room", null, null);
    Bookcase bookcase2 = new Bookcase(200L, 4, 40, "Study Room", null, null);
    when(bookcaseRepository.findAll()).thenReturn(List.of(bookcase1, bookcase2));

    List<Bookcase> result = queryBookcaseUseCase.getAllBookcases();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getBookcaseId()).isEqualTo(100L);
    assertThat(result.get(1).getBookcaseId()).isEqualTo(200L);
    verify(bookcaseRepository).findAll();
  }

  @Test
  void getAllBookcases_shouldReturnEmptyListWhenNoBookcasesExist() {
    when(bookcaseRepository.findAll()).thenReturn(List.of());

    List<Bookcase> result = queryBookcaseUseCase.getAllBookcases();

    assertThat(result).isEmpty();
    verify(bookcaseRepository).findAll();
  }

  @Test
  void getAllBookcaseLocations_shouldReturnAllLocations() {
    List<String> expectedLocations = List.of("Living Room", "Study Room", "Bedroom");
    when(bookcaseRepository.getAllBookCaseLocations()).thenReturn(expectedLocations);

    List<String> result = queryBookcaseUseCase.getAllBookcaseLocations();

    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyElementsOf(expectedLocations);
    verify(bookcaseRepository).getAllBookCaseLocations();
  }

  @Test
  void findById_shouldReturnBookcaseWhenExists() {
    Bookcase bookcase = new Bookcase(100L, 5, 50, "Living Room", null, null);
    when(bookcaseRepository.findById(100L)).thenReturn(bookcase);

    Bookcase result = queryBookcaseUseCase.findById(100L);

    assertThat(result).isNotNull();
    assertThat(result.getBookcaseId()).isEqualTo(100L);
    verify(bookcaseRepository).findById(100L);
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    when(bookcaseRepository.findById(999L)).thenReturn(null);

    Bookcase result = queryBookcaseUseCase.findById(999L);

    assertThat(result).isNull();
    verify(bookcaseRepository).findById(999L);
  }

  @Test
  void getAllBookcasesByLocation_shouldReturnBookcasesForLocation() {
    Bookcase bookcase1 = new Bookcase(100L, 5, 50, "Living Room", null, null);
    Bookcase bookcase2 = new Bookcase(200L, 4, 40, "Living Room", null, null);
    when(bookcaseRepository.findByLocation("Living Room"))
        .thenReturn(List.of(bookcase1, bookcase2));

    List<Bookcase> result = queryBookcaseUseCase.getAllBookcasesByLocation("Living Room");

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getBookcaseId()).isEqualTo(100L);
    assertThat(result.get(1).getBookcaseId()).isEqualTo(200L);
    verify(bookcaseRepository).findByLocation("Living Room");
  }

  @Test
  void getAllBookcasesByLocation_shouldReturnEmptyListWhenNoBookcasesExist() {
    when(bookcaseRepository.findByLocation("Garage")).thenReturn(List.of());

    List<Bookcase> result = queryBookcaseUseCase.getAllBookcasesByLocation("Garage");

    assertThat(result).isEmpty();
    verify(bookcaseRepository).findByLocation("Garage");
  }

  @Test
  void getAllBookcasesByUserId_shouldReturnBookcasesForUser() {
    Bookcase bookcase1 = new Bookcase(100L, 5, 50, "Living Room", null, null);
    Bookcase bookcase2 = new Bookcase(200L, 3, 30, "Bedroom", null, null);
    when(bookcaseRepository.findByAppUserId(1L)).thenReturn(List.of(bookcase1, bookcase2));

    List<Bookcase> result = queryBookcaseUseCase.getAllBookcasesByUserId(1L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getBookcaseId()).isEqualTo(100L);
    assertThat(result.get(1).getBookcaseId()).isEqualTo(200L);
    verify(bookcaseRepository).findByAppUserId(1L);
  }

  @Test
  void getAllBookcasesByUserId_shouldReturnEmptyListWhenUserHasNoBookcases() {
    when(bookcaseRepository.findByAppUserId(999L)).thenReturn(List.of());

    List<Bookcase> result = queryBookcaseUseCase.getAllBookcasesByUserId(999L);

    assertThat(result).isEmpty();
    verify(bookcaseRepository).findByAppUserId(999L);
  }
}
