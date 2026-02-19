package com.penrose.bibby.library.stacks.shelf.core.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.cataloging.book.api.dtos.BriefBibliographicRecord;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrowseShelfUseCaseTest {

  @Test
  void browseShelf_returnsBookBriefsFromBookFacade() {
    // Arrange
    BookFacade bookFacade = mock(BookFacade.class);
    BrowseShelfUseCase useCase = new BrowseShelfUseCase(bookFacade);

    // Adjust this depending on how your ShelfId is defined:
    // - if record ShelfId(Long value) => new ShelfId(10L)
    // - if record ShelfId(Long shelfId) => new ShelfId(10L)
    // - if factory => ShelfId.of(10L)
    ShelfId shelfId = new ShelfId(10L);

    List<BriefBibliographicRecord> expected =
        List.of(mock(BriefBibliographicRecord.class), mock(BriefBibliographicRecord.class));

    when(bookFacade.getBriefBibliographicRecordsByShelfId(10L)).thenReturn(expected);

    // Act
    List<BriefBibliographicRecord> actual = useCase.browseShelf(shelfId);

    // Assert
    assertSame(expected, actual);

    // Verify correct call

    verify(bookFacade).getBriefBibliographicRecordsByShelfId(10L);

    verifyNoMoreInteractions(bookFacade);
  }
}
