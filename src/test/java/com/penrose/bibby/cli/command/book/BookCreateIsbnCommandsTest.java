package com.penrose.bibby.cli.command.book;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.cli.ui.BookcardRenderer;
import com.penrose.bibby.library.cataloging.author.core.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.bookcase.core.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BookCreateIsbnCommandsTest {

  @Test
  void createBookScan_whenUserSelectsNo_doesNotCreateBook() {
    // Arrange
    CliPromptService cliPrompt = mock(CliPromptService.class);
    BookcardRenderer bookcardRenderer = mock(BookcardRenderer.class);
    BookFacade bookFacade = mock(BookFacade.class);
    BookcaseFacade bookcaseFacade = mock(BookcaseFacade.class);
    ShelfFacade shelfFacade = mock(ShelfFacade.class);
    AuthorFacade authorFacade = mock(AuthorFacade.class);
    PromptOptions promptOptions = mock(PromptOptions.class);
    BookCreateCommands bookCreateCommands = mock(BookCreateCommands.class);

    BookCreateIsbnCommands commands =
        spy(
            new BookCreateIsbnCommands(
                cliPrompt,
                bookcardRenderer,
                bookFacade,
                bookcaseFacade,
                shelfFacade,
                authorFacade,
                promptOptions,
                bookCreateCommands));

    BookMetaDataResponse meta =
        new BookMetaDataResponse(
            1L,
            "Test Title",
            "9781234567897",
            List.of("Ada Lovelace"),
            "Test Publisher",
            Optional.of("Desc"));

    doReturn(meta).when(commands).scanBook();
    when(cliPrompt.promptToConfirmBookAddition()).thenReturn(false);

    // Act
    commands.createBookScan(false);

    // Assert
    verify(cliPrompt, times(1)).promptToConfirmBookAddition();
    verify(bookFacade, never()).createBookFromMetaData(any(), anyList(), anyString(), any());
    verify(cliPrompt, never()).promptForPlacementDecision();
    verifyNoMoreInteractions(bookFacade);
  }

  @Test
  void createBookScan_whenUserSelectsYes_andNoPlacement_createsBookWithNotSetLocation() {
    // Arrange
    CliPromptService cliPrompt = mock(CliPromptService.class);
    BookcardRenderer bookcardRenderer = mock(BookcardRenderer.class);
    BookFacade bookFacade = mock(BookFacade.class);
    BookcaseFacade bookcaseFacade = mock(BookcaseFacade.class);
    ShelfFacade shelfFacade = mock(ShelfFacade.class);
    AuthorFacade authorFacade = mock(AuthorFacade.class);
    PromptOptions promptOptions = mock(PromptOptions.class);
    BookCreateCommands bookCreateCommands = mock(BookCreateCommands.class);

    BookCreateIsbnCommands commands =
        spy(
            new BookCreateIsbnCommands(
                cliPrompt,
                bookcardRenderer,
                bookFacade,
                bookcaseFacade,
                shelfFacade,
                authorFacade,
                promptOptions,
                bookCreateCommands));

    BookMetaDataResponse meta =
        new BookMetaDataResponse(
            1L,
            "Test Title",
            "9781234567897",
            List.of("Ada Lovelace"),
            "Test Publisher",
            Optional.of("Desc"));

    doReturn(meta).when(commands).scanBook();
    when(cliPrompt.promptToConfirmBookAddition()).thenReturn(true);
    when(bookFacade.isDuplicate("9781234567897")).thenReturn(false);

    doReturn(List.of(10L)).when(commands).createAuthorsFromMetaData(meta.authors());

    when(cliPrompt.promptForPlacementDecision()).thenReturn(false);

    when(bookcardRenderer.createBookCard(
            eq("Test Title"),
            eq("9781234567897"),
            eq(meta.authors().toString()),
            eq("Test Publisher"),
            eq("Not Set"),
            eq("Not Set"),
            eq("Not Set")))
        .thenReturn("BOOK_CARD");

    // Act
    commands.createBookScan(false);

    // Assert
    verify(bookFacade, times(1)).createBookFromMetaData(meta, List.of(10L), "9781234567897", null);
    verify(bookcardRenderer, times(1))
        .createBookCard(
            "Test Title",
            "9781234567897",
            meta.authors().toString(),
            "Test Publisher",
            "Not Set",
            "Not Set",
            "Not Set");

    verify(cliPrompt, times(1)).promptToConfirmBookAddition();
    verify(cliPrompt, times(1)).promptForPlacementDecision();
    verify(cliPrompt, never()).promptForBookcaseLocation();
    verify(cliPrompt, never()).promptForBookcaseSelection(anyMap());
    verify(cliPrompt, never()).promptForShelfSelection(anyLong());
  }
}
