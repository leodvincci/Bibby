package com.penrose.bibby.library.stacks.bookcase.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.core.application.usecases.CreateBookcaseUseCase;
import com.penrose.bibby.library.stacks.bookcase.core.application.usecases.DeleteBookcaseUseCase;
import com.penrose.bibby.library.stacks.bookcase.core.application.usecases.QueryBookcaseUseCase;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookcaseServiceTest {

  @Mock private CreateBookcaseUseCase createBookcaseUseCase;

  @Mock private DeleteBookcaseUseCase deleteBookcaseUseCase;

  @Mock private QueryBookcaseUseCase queryBookcaseUseCase;

  @InjectMocks private BookcaseService bookcaseService;

  @Test
  void createNewBookCase_shouldDelegateToCreateBookcaseUseCase() {
    CreateBookcaseResult expected = new CreateBookcaseResult(100L);
    when(createBookcaseUseCase.createNewBookCase(1L, "BC001", "A", "1", 5, 10, "Living Room"))
        .thenReturn(expected);

    CreateBookcaseResult result =
        bookcaseService.createNewBookCase(1L, "BC001", "A", "1", 5, 10, "Living Room");

    assertThat(result).isEqualTo(expected);
    verify(createBookcaseUseCase)
        .createNewBookCase(1L, "BC001", "A", "1", 5, 10, "Living Room");
  }

  @Test
  void findBookCaseById_shouldDelegateToQueryBookcaseUseCase() {
    Bookcase bookcase = new Bookcase(100L, 5, 50, "Living Room", "A", "1");
    when(queryBookcaseUseCase.findBookCaseById(100L)).thenReturn(bookcase);

    Bookcase result = bookcaseService.findBookCaseById(100L);

    assertThat(result).isNotNull();
    assertThat(result.getBookcaseId()).isEqualTo(100L);
    verify(queryBookcaseUseCase).findBookCaseById(100L);
  }

  @Test
  void findBookCaseById_shouldReturnEmptyWhenNotFound() {
    when(queryBookcaseUseCase.findBookCaseById(999L)).thenReturn(null);

    Bookcase result = bookcaseService.findBookCaseById(999L);

    assertThat(result).isNull();
    verify(queryBookcaseUseCase).findBookCaseById(999L);
  }

  @Test
  void getAllBookcases_shouldDelegateToQueryBookcaseUseCase() {
    List<Bookcase> expected = List.of(new Bookcase(100L, 5, 50, "Living Room", "A", "1"));
    when(queryBookcaseUseCase.getAllBookcases()).thenReturn(expected);

    List<Bookcase> result = bookcaseService.getAllBookcases();

    assertThat(result).isEqualTo(expected);
    verify(queryBookcaseUseCase).getAllBookcases();
  }

  @Test
  void getAllBookcaseLocations_shouldDelegateToQueryBookcaseUseCase() {
    List<String> expected = List.of("Living Room", "Study Room");
    when(queryBookcaseUseCase.getAllBookcaseLocations()).thenReturn(expected);

    List<String> result = bookcaseService.getAllBookcaseLocations();

    assertThat(result).isEqualTo(expected);
    verify(queryBookcaseUseCase).getAllBookcaseLocations();
  }

  @Test
  void findById_shouldDelegateToQueryBookcaseUseCase() {
    Bookcase bookcase = new Bookcase(100L, 5, 50, "Living Room", "A", "1");
    when(queryBookcaseUseCase.findById(100L)).thenReturn(bookcase);

    Bookcase result = bookcaseService.findById(100L);

    assertThat(result).isNotNull();
    assertThat(result.getBookcaseId()).isEqualTo(100L);
    verify(queryBookcaseUseCase).findById(100L);
  }

  @Test
  void findById_shouldReturnEmptyWhenNotFound() {
    when(queryBookcaseUseCase.findById(999L)).thenReturn(null);

    Bookcase result = bookcaseService.findById(999L);

    assertThat(result).isNull();
    verify(queryBookcaseUseCase).findById(999L);
  }

  @Test
  void getAllBookcasesByLocation_shouldDelegateToQueryBookcaseUseCase() {
    List<Bookcase> expected = List.of(new Bookcase(100L, 5, 50, "Living Room", "A", "1"));
    when(queryBookcaseUseCase.getAllBookcasesByLocation("Living Room")).thenReturn(expected);

    List<Bookcase> result = bookcaseService.getAllBookcasesByLocation("Living Room");

    assertThat(result).isEqualTo(expected);
    verify(queryBookcaseUseCase).getAllBookcasesByLocation("Living Room");
  }

  @Test
  void getAllBookcasesByUserId_shouldDelegateToQueryBookcaseUseCase() {
    List<Bookcase> expected = List.of(new Bookcase(100L, 5, 50, "Living Room", "A", "1"));
    when(queryBookcaseUseCase.getAllBookcasesByUserId(1L)).thenReturn(expected);

    List<Bookcase> result = bookcaseService.getAllBookcasesByUserId(1L);

    assertThat(result).isEqualTo(expected);
    verify(queryBookcaseUseCase).getAllBookcasesByUserId(1L);
  }

  @Test
  void deleteBookcase_shouldDelegateToDeleteBookcaseUseCase() {
    bookcaseService.deleteBookcase(100L);

    verify(deleteBookcaseUseCase).deleteBookcase(100L);
  }
}
