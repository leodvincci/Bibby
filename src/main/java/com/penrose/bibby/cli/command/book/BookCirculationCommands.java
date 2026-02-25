package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.library.cataloging.author.api.dtos.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.core.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.bookcase.api.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.core.domain.BookcaseMapper;
import com.penrose.bibby.library.stacks.bookcase.core.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels.ShelfResponse;
import java.util.*;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
@Command(command = "book", group = "Book Circulation Commands")
public class BookCirculationCommands extends AbstractShellComponent {

  private final AuthorFacade authorFacade;
  private final BookFacade bookFacade;
  private final BookcaseFacade bookcaseFacade;
  private final ShelfFacade shelfFacade;
  private final CliPromptService cliPrompt;
  private final ComponentFlow.Builder componentFlowBuilder;
  private final PromptOptions promptOptions;

  public BookCirculationCommands(
      ComponentFlow.Builder componentFlowBuilder,
      AuthorFacade authorFacade,
      ShelfFacade shelfFacade,
      CliPromptService cliPrompt,
      BookFacade bookFacade,
      BookcaseFacade bookcaseFacade,
      PromptOptions promptOptions) {
    this.componentFlowBuilder = componentFlowBuilder;
    this.authorFacade = authorFacade;
    this.shelfFacade = shelfFacade;
    this.cliPrompt = cliPrompt;
    this.bookFacade = bookFacade;
    this.bookcaseFacade = bookcaseFacade;
    this.promptOptions = promptOptions;
  }

  @Command(command = "check-out", description = "Check-Out a book from the library")
  public void checkOutBook() {
    String bookTitle = cliPrompt.promptForBookTitle();
    BookDTO bookDTO = bookFacade.findBookByTitle(bookTitle);
    String bookcaseName = "N.A";
    String shelfName = "N.A";
    if (bookDTO == null) {
      System.out.println("Book Not Found.");
    } else if (bookDTO.shelfId() != null) {
      Optional<ShelfResponse> shelf = shelfFacade.findShelfById(bookDTO.shelfId());
      BookcaseDTO bookcase =
          BookcaseMapper.toDTO(bookcaseFacade.findBookCaseById(shelf.get().bookcaseId()));
      bookcaseName = bookcase.location();
      shelfName = shelf.get().shelfLabel();
    }
    if (bookDTO.availabilityStatus().toString().equals("CHECKED_OUT")) {
      System.out.println(
          """

                    \u001B[38;5;63m  .---.
                    \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "This one’s already off the shelf. No double-dipping on checkouts."
                    \u001B[38;5;63m  \\|=|/

                    """);
    } else {
      Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());
      System.out.println(
          String.format(
              """
                    \n\u001B[32mConfirm Checkout\n\u001B[0m
                            \033[31mTitle\u001B[0m %s
                            \033[31mAuthor/s\u001B[0m %s

                            \033[31mStatus %s

                            \033[31mBookcase\u001B[0m %s
                            \033[31mShelf\u001B[0m %s
                    """,
              bookDTO.title(), authors, bookDTO.availabilityStatus(), bookcaseName, shelfName));
      ComponentFlow confirmationFlow =
          componentFlowBuilder
              .clone()
              .withStringInput("isConfirmed")
              .name("y or n:_ ")
              .and()
              .build();
      ComponentFlow.ComponentFlowResult confirmationResult = confirmationFlow.run();

      if (confirmationResult.getContext().get("isConfirmed").equals("y")) {
        bookFacade.checkOutBook(bookDTO);
        System.out.println(
            String.format(
                """

                        \u001B[38;5;63m  .---.
                        \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "All set — \u001B[38;5;42m %s \u001B[38;5;15m is checked out and ready to go with you. \033[36m"
                        \u001B[38;5;63m  \\|=|/

                        """,
                bookTitle));
      } else {
        System.out.println(
            """

                        \u001B[38;5;63m  .---.
                        \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "Cool, I’ll just… put this back by myself...and whisper *maybe next time* to the shelves... Again."
                        \u001B[38;5;63m  \\|=|/

                        """);
      }
    }
  }

  @Command(
      command = "check-in",
      description = "Return a borrowed book to the library and update its shelf placement.")
  public void checkInBook() {
    String bookTitle = cliPrompt.promptForBookTitle();
    BookDTO bookDTO = bookFacade.findBookByTitle(bookTitle);

    String bookcaseLabel = "No Assigned Bookcase";
    String bookshelfLabel = "No Assigned Bookshelf";
    if (bookDTO == null) {
      System.out.println("Book Not Found");
    } else if (bookDTO.shelfId() != null) {
      Optional<ShelfResponse> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
      BookcaseDTO bookcaseDTO =
          BookcaseMapper.toDTO(bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId()));
      bookcaseLabel = bookcaseDTO.location();
      bookshelfLabel = shelfDTO.get().shelfLabel();
    }
    Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());

    System.out.println(
        String.format(
            """
                    \n\u001B[32mConfirm Checkin\n\u001B[0m
                            \033[31mTitle\u001B[0m %s
                            \033[31mAuthor/s\u001B[0m %s

                            \033[31mStatus %s

                            \033[31mBookcase\u001B[0m %s
                            \033[31mShelf\u001B[0m %s
                    """,
            bookDTO.title(), authors, bookDTO.availabilityStatus(), bookcaseLabel, bookshelfLabel));

    ComponentFlow flow;
    flow =
        componentFlowBuilder.clone().withStringInput("isConfirmed").name("y or n:_ ").and().build();
    ComponentFlow.ComponentFlowResult result;
    result = flow.run();

    if (result.getContext().get("isConfirmed").equals("y")) {
      bookFacade.checkInBook(bookTitle);
      System.out.println(
          """

                    \u001B[38;5;63m  .---.
                    \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "Check-in complete. Book state updated to \u001B[38;5;42mAVAILABLE."
                    \u001B[38;5;63m  \\|=|/

                    """);
    }
  }

  public void checkOutBookByID() throws InterruptedException {
    ComponentFlow componentFlow;
    componentFlow =
        componentFlowBuilder.clone().withStringInput("bookID").name("Book ID#:_ ").and().build();

    componentFlow.run();
    System.out.println(
        "\n\u001B[36m</>\u001B[0m:Don’t forget to check it back in… or at least feel guilty about it.\n");
  }

  public void askBookCheckOut() throws InterruptedException {
    ComponentFlow flow;
    flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("checkOutDecision")
            .name("Want to check one out, or just window-shopping the shelves again?")
            .selectItems(promptOptions.yesNoOptions())
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    String checkOutResponse = result.getContext().get("checkOutDecision", String.class);
    if (checkOutResponse.equalsIgnoreCase("yes")) {
      checkOutBookByID();
    } else {
      System.out.println(
          "\n\u001B[36m</>\u001B[0m:Cool, I’ll just… put all these back by myself...and whisper *maybe next time* to the shelves... Again.\n");
    }
  }
}
