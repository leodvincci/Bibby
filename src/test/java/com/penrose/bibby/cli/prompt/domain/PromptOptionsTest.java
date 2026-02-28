package com.penrose.bibby.cli.prompt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.penrose.bibby.library.cataloging.author.core.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorName;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
import com.penrose.bibby.library.cataloging.book.core.domain.AvailabilityStatus;
import com.penrose.bibby.library.cataloging.book.core.domain.model.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.BookId;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Isbn;
import com.penrose.bibby.library.cataloging.book.core.domain.valueObject.Title;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.core.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfQueryFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptOptionsTest {

  @Mock private ShelfQueryFacade shelfFacade;

  @Mock private AuthorFacade authorFacade;

  @Mock private BookFacade bookFacade;

  @Mock private BookcaseFacade bookcaseFacade;

  private PromptOptions promptOptions;

  @BeforeEach
  void setUp() {
    promptOptions = new PromptOptions(shelfFacade, authorFacade, bookFacade, bookcaseFacade);
  }

  @Test
  void bookCaseOptions_shouldAlwaysContainCancelAsFirstEntry() {
    when(bookcaseFacade.getAllBookcases()).thenReturn(List.of());

    Map<String, String> options = promptOptions.bookCaseOptions();

    assertThat(options).isNotEmpty();
    String firstKey = options.keySet().iterator().next();
    assertThat(options.get(firstKey)).isEqualTo("cancel");
  }

  @Test
  void bookCaseOptions_shouldReturnOnlyCancelWhenNoBookcasesExist() {
    when(bookcaseFacade.getAllBookcases()).thenReturn(List.of());

    Map<String, String> options = promptOptions.bookCaseOptions();

    assertThat(options).hasSize(1);
    assertThat(options).containsValue("cancel");
  }

  @Test
  void bookCaseOptions_shouldContainOneEntryPerBookcase() {
    Bookcase bookcase1 = new Bookcase(1L, 1L, 5, 20, "Living Room", "A", "1");
    Bookcase bookcase2 = new Bookcase(2L, 1L, 3, 15, "Office", "B", "2");

    when(bookcaseFacade.getAllBookcases()).thenReturn(List.of(bookcase1, bookcase2));
    when(shelfFacade.findShelvesByBookcaseId(1L)).thenReturn(List.of());
    when(shelfFacade.findShelvesByBookcaseId(2L)).thenReturn(List.of());

    Map<String, String> options = promptOptions.bookCaseOptions();

    // 1 cancel + 2 bookcases
    assertThat(options).hasSize(3);
    assertThat(options).containsValue("1");
    assertThat(options).containsValue("2");
  }

  @Test
  void bookCaseOptions_shouldCorrectlyCountBooksAcrossShelves() {
    Bookcase bookcase = new Bookcase(1L, 1L, 2, 10, "Study", "C", "3");

    ShelfResponse shelf1 = new ShelfResponse(10L, 1, "Top Shelf", 10, List.of(100L, 101L), 1L);
    ShelfResponse shelf2 = new ShelfResponse(11L, 2, "Bottom Shelf", 10, List.of(200L), 1L);

    Book book1 = buildBook(100L);
    Book book2 = buildBook(101L);
    Book book3 = buildBook(200L);

    when(bookcaseFacade.getAllBookcases()).thenReturn(List.of(bookcase));
    when(shelfFacade.findShelvesByBookcaseId(1L)).thenReturn(List.of(shelf1, shelf2));
    when(bookFacade.findByShelfId(10L)).thenReturn(List.of(book1, book2));
    when(bookFacade.findByShelfId(11L)).thenReturn(List.of(book3));

    Map<String, String> options = promptOptions.bookCaseOptions();

    // The formatted key should contain the total book count (3)
    String bookcaseKey =
        options.keySet().stream().filter(k -> options.get(k).equals("1")).findFirst().orElseThrow();

    assertThat(bookcaseKey).contains("3");
  }

  @Test
  void bookCaseOptions_shouldFormatKeyWithBookcaseLocationInUppercase() {
    Bookcase bookcase = new Bookcase(1L, 1L, 2, 10, "basement", "A", "1");

    when(bookcaseFacade.getAllBookcases()).thenReturn(List.of(bookcase));
    when(shelfFacade.findShelvesByBookcaseId(1L)).thenReturn(List.of());

    Map<String, String> options = promptOptions.bookCaseOptions();

    String bookcaseKey =
        options.keySet().stream().filter(k -> options.get(k).equals("1")).findFirst().orElseThrow();

    assertThat(bookcaseKey).contains("BASEMENT");
  }

  @Test
  void bookCaseOptions_shouldCountZeroBooksWhenShelvesAreEmpty() {
    Bookcase bookcase = new Bookcase(1L, 1L, 3, 10, "Garage", "D", "4");
    ShelfResponse emptyShelf = new ShelfResponse(20L, 1, "Empty Shelf", 10, new ArrayList<>(), 1L);

    when(bookcaseFacade.getAllBookcases()).thenReturn(List.of(bookcase));
    when(shelfFacade.findShelvesByBookcaseId(1L)).thenReturn(List.of(emptyShelf));
    when(bookFacade.findByShelfId(20L)).thenReturn(List.of());

    Map<String, String> options = promptOptions.bookCaseOptions();

    String bookcaseKey =
        options.keySet().stream().filter(k -> options.get(k).equals("1")).findFirst().orElseThrow();

    // book count is 0, verify the key contains "0"
    assertThat(bookcaseKey).contains("0");
  }

  @Test
  void bookCaseOptions_shouldPreservesInsertionOrder() {
    Bookcase bookcase1 = new Bookcase(1L, 1L, 2, 10, "Alpha", "A", "1");
    Bookcase bookcase2 = new Bookcase(2L, 1L, 2, 10, "Beta", "B", "2");
    Bookcase bookcase3 = new Bookcase(3L, 1L, 2, 10, "Gamma", "C", "3");

    when(bookcaseFacade.getAllBookcases()).thenReturn(List.of(bookcase1, bookcase2, bookcase3));
    when(shelfFacade.findShelvesByBookcaseId(1L)).thenReturn(List.of());
    when(shelfFacade.findShelvesByBookcaseId(2L)).thenReturn(List.of());
    when(shelfFacade.findShelvesByBookcaseId(3L)).thenReturn(List.of());

    Map<String, String> options = promptOptions.bookCaseOptions();

    List<String> values = new ArrayList<>(options.values());
    assertThat(values).containsExactly("cancel", "1", "2", "3");
  }

  private Book buildBook(Long id) {
    return new Book(
        new BookId(id),
        1,
        new Title("Title"),
        List.of(new AuthorRef(1L, new AuthorName("First", "Last"))),
        new Isbn("9780345391803"),
        "genre",
        "publisher",
        2020,
        1L,
        "desc",
        AvailabilityStatus.AVAILABLE,
        null,
        null,
        null);
  }
}
