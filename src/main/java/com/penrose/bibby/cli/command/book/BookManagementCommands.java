package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.ui.BookcardRenderer;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.transaction.annotation.Transactional;

@ShellComponent
@Command(command = "book", group = "Book Management Commands")
public class BookManagementCommands {

  private final CliPromptService cliPromptService;
  private final BookFacade bookFacade;
  private final BookcardRenderer bookcardRenderer = new BookcardRenderer();

  public BookManagementCommands(CliPromptService cliPromptService, BookFacade bookFacade) {
    this.cliPromptService = cliPromptService;
    this.bookFacade = bookFacade;
  }

  @Transactional
  @Command(
      command = "edit",
      description =
          """
                            \u001B[38;5;185mEdit existing book details in your library. Update metadata, authors, and other information as needed.
                            \u001B[0m
                            """,
      group = "Book Management Commands")
  public void BookEditCommand() {
    System.out.println("\n\u001B[95mEdit Book\u001B[0m (':q' to quit)");
    BookDTO bookResult = bookLookup();
    String userSelection = cliPromptService.promptForBookEditSelection();
    if (userSelection.equals("cancel")) {
      System.out.println("\u001B[38;5;33mEdit cancelled.\u001B[0m");
      return;
    }

    if (userSelection.equals("publisher")) {
      String newPublisher = editPublisher();
      if (!cliPromptService.promptToConfirmChange(newPublisher)) return;
      bookFacade.updatePublisher(bookResult.isbn(), newPublisher);
      BookDTO newBookDTO = bookFacade.findBookByIsbn(bookResult.isbn());
      String updatedCard =
          bookcardRenderer.bookImportCard(
              newBookDTO.title(),
              newBookDTO.isbn(),
              newBookDTO.authors().toString(),
              newBookDTO.publisher());
      System.out.println(updatedCard);
    }
  }

  @Command(
      command = "alerts",
      description =
          """
                            \u001B[38;5;185mView and manage book alerts in your library.
                            \u001B[0m
                            """,
      group = "Book Management Commands")
  public void bookIssues() {
    System.out.println("\n\u001B[95mBook Alerts\u001B[0m (':q' to quit)");
  }

  public String editPublisher() {
    return cliPromptService.promptForEditPublisher();
  }

  public BookDTO bookLookup() {
    String isbn = cliPromptService.promptForIsbn();

    BookDTO bookDTO = bookFacade.findBookByIsbn(isbn);

    System.out.println(
        bookcardRenderer.bookImportCard(
            bookDTO.title(), bookDTO.isbn(), bookDTO.authors().toString(), bookDTO.publisher()));
    return bookDTO;
  }
}
