package com.penrose.bibby.cli.command.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.ui.BookcardRenderer;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class BookManagementCommandsTest {

  /**
   * Test Description: This test ensures that editing the publisher of a book successfully updates
   * the publisher and displays the updated book information.
   */
  @Test
  void testBookEditCommand_EditPublisherSuccess() {
    // Arrange
    CliPromptService cliPromptServiceMock = mock(CliPromptService.class);
    BookFacade bookFacadeMock = mock(BookFacade.class);
    BookcardRenderer bookcardRendererMock = mock(BookcardRenderer.class);
    BookManagementCommands commands =
        new BookManagementCommands(cliPromptServiceMock, bookFacadeMock);

    // Mock the flow
    BookDTO initialBookDTO =
        new BookDTO(
            1L,
            1,
            "Test Title",
            List.of("Author One"),
            "9781234567897",
            "Fiction",
            "Old Publisher",
            2020,
            1L,
            "Description",
            null,
            null,
            null,
            null);
    BookDTO updatedBookDTO =
        new BookDTO(
            1L,
            1,
            "Test Title",
            List.of("Author One"),
            "9781234567897",
            "Fiction",
            "New Publisher",
            2020,
            1L,
            "Description",
            null,
            null,
            null,
            null);

    when(cliPromptServiceMock.promptForIsbn()).thenReturn("9781234567897");
    when(bookFacadeMock.findBookByIsbn("9781234567897")).thenReturn(initialBookDTO);
    when(cliPromptServiceMock.promptForBookEditSelection()).thenReturn("publisher");
    when(cliPromptServiceMock.promptForEditPublisher()).thenReturn("New Publisher");
    when(cliPromptServiceMock.promptToConfirmChange("New Publisher")).thenReturn(true);
    when(bookFacadeMock.findBookByIsbn("9781234567897")).thenReturn(updatedBookDTO);
    when(bookcardRendererMock.bookImportCard(
            "Test Title", "9781234567897", "[Author One]", "New Publisher"))
        .thenReturn("Updated Book Info Card");

    // Act
    commands.BookEditCommand();

    // Assert
    verify(cliPromptServiceMock, times(1)).promptForBookEditSelection();
    verify(bookFacadeMock, times(1)).updatePublisher("9781234567897", "New Publisher");
    verify(bookFacadeMock, times(2)).findBookByIsbn("9781234567897");
  }

  /**
   * Test Description: This test ensures that when the user does not confirm the change to the
   * publisher, no update is performed.
   */
  @Test
  void testBookEditCommand_EditPublisherCancelledByUser() {
    // Arrange
    CliPromptService cliPromptServiceMock = mock(CliPromptService.class);
    BookFacade bookFacadeMock = mock(BookFacade.class);
    BookManagementCommands commands =
        new BookManagementCommands(cliPromptServiceMock, bookFacadeMock);

    // Mock the flow
    BookDTO bookDTO =
        new BookDTO(
            1L,
            1,
            "Test Title",
            List.of("Author One"),
            "9781234567897",
            "Fiction",
            "Old Publisher",
            2020,
            1L,
            "Description",
            null,
            null,
            null,
            null);

    when(cliPromptServiceMock.promptForIsbn()).thenReturn("9781234567897");
    when(bookFacadeMock.findBookByIsbn("9781234567897")).thenReturn(bookDTO);
    when(cliPromptServiceMock.promptForBookEditSelection()).thenReturn("publisher");
    when(cliPromptServiceMock.promptForEditPublisher()).thenReturn("New Publisher");
    when(cliPromptServiceMock.promptToConfirmChange("New Publisher")).thenReturn(false);

    // Act
    commands.BookEditCommand();

    // Assert
    verify(cliPromptServiceMock, times(1)).promptToConfirmChange("New Publisher");
    verify(bookFacadeMock, never()).updatePublisher(anyString(), anyString());
  }

  /**
   * Test Description: This test ensures that when the ISBN entered by the user does not correspond
   * to an existing book, an exception is thrown.
   */
  @Test
  void testBookEditCommand_BookNotFound() {
    // Arrange
    CliPromptService cliPromptServiceMock = mock(CliPromptService.class);
    BookFacade bookFacadeMock = mock(BookFacade.class);
    BookManagementCommands commands =
        new BookManagementCommands(cliPromptServiceMock, bookFacadeMock);

    when(cliPromptServiceMock.promptForIsbn()).thenReturn("9781234567897");
    when(bookFacadeMock.findBookByIsbn("9781234567897"))
        .thenThrow(new RuntimeException("Book not found"));

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, commands::BookEditCommand);
    assertEquals("Book not found", exception.getMessage());

    verify(bookFacadeMock, times(1)).findBookByIsbn("9781234567897");
  }
}
