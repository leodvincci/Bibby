package com.penrose.bibby.cli.command.bookcase;

import com.penrose.bibby.cli.command.book.BookCirculationCommands;
import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDetailView;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookSummary;
import com.penrose.bibby.library.cataloging.book.core.port.inbound.BookFacade;
import com.penrose.bibby.library.stacks.bookcase.core.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfFacade;
import java.util.*;
import org.slf4j.Logger;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;

@Component
@Command(command = "bookcase", group = "Bookcase Commands")
public class BookcaseCommands extends AbstractShellComponent {

  private final ComponentFlow.Builder componentFlowBuilder;
  private final ShelfFacade shelfFacade;
  private final BookFacade bookFacade;
  private final BookcaseFacade bookcaseFacade;
  private final CliPromptService cliPromptService;
  private final PromptOptions promptOptions;
  Logger log = org.slf4j.LoggerFactory.getLogger(BookcaseCommands.class);

  public BookcaseCommands(
      ComponentFlow.Builder componentFlowBuilder,
      ShelfFacade shelfFacade,
      BookFacade bookFacade,
      BookcaseFacade bookcaseFacade,
      CliPromptService cliPromptService,
      PromptOptions promptOptions) {
    this.componentFlowBuilder = componentFlowBuilder;
    this.shelfFacade = shelfFacade;
    this.bookFacade = bookFacade;
    this.bookcaseFacade = bookcaseFacade;
    this.cliPromptService = cliPromptService;
    this.promptOptions = promptOptions;
  }

  @Command(command = "create", description = "Create a new bookcase in the library.")
  public void createBookcase() throws InterruptedException {

    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withStringInput("bookcaseZone")
            .name("Bookcase Zone (e.g. NorthWall,Desk,Corner,BackWall,CouchSide,Main,WindowSide):_")
            .and()
            .withStringInput("zoneIndex")
            .name("Zone Index (e.g. A, B, C):_")
            .and()
            .withStringInput("shelfCount")
            .name("Shelf Count:_")
            .and()
            .withStringInput("bookCapacity")
            .name("Books per Shelf:_")
            .and()
            .build();

    String bookcaseLocation = cliPromptService.promptForBookcaseLocation();
    if (bookcaseLocation == null) {
      return;
    }

    log.info("Selected Bookcase Location: {}", bookcaseLocation);
    ComponentFlow.ComponentFlowResult result = flow.run();

    String bookcaseZone = result.getContext().get("bookcaseZone");
    String zoneIndex = result.getContext().get("zoneIndex");
    int shelfCount = Integer.parseInt(result.getContext().get("shelfCount"));
    int bookCapacity = Integer.parseInt(result.getContext().get("bookCapacity"));

    String confirmationMsg =
        """

            -----------------------------------
                    NEW BOOKCASE SUMMARY
            -----------------------------------
            Bookcase Location:       %s
            Bookcase Zone:          %s
            Bookcase Zone Index:    %s
            Shelf Count:    %s
            Capacity/Shelf: %s

            Total Storage:  %d books
            -----------------------------------

    """
            .formatted(
                bookcaseLocation,
                bookcaseZone,
                zoneIndex,
                shelfCount,
                bookCapacity,
                (shelfCount * bookCapacity));

    System.out.println(confirmationMsg);

    flow =
        componentFlowBuilder
            .clone()
            .withStringInput("confirmation")
            .name("Are these details correct? (y/n):_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult res = flow.run();
    if (res.getContext().get("confirmation").equals("Y")
        | res.getContext().get("confirmation").equals("y")) {
      bookcaseFacade.createNewBookCase(
          null, bookcaseZone, bookcaseZone, zoneIndex, shelfCount, bookCapacity, bookcaseLocation);
      System.out.println("Created");
    } else {
      System.out.println("Not Created");
    }
  }

  @Command(
      command = "browse",
      description =
          "Display all bookcases currently in the library, along with their labels, total shelves")
  public void listBookcaseByLocation() {
    BookCirculationCommands bookCirculationCommands;

    String location = cliPromptService.promptForBookcaseLocation();

    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("bookcaseSelected")
            .name("Select a Bookcase")
            .selectItems(promptOptions.bookCaseOptionsByLocation(location))
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();

    selectShelf(Long.parseLong(result.getContext().get("bookcaseSelected")));
  }

  public void selectShelf(Long bookCaseId) {
    List<ShelfSummary> shelfSummaries = shelfFacade.getShelfSummariesForBookcaseByBookcaseId(bookCaseId);

    Map<String, String> bookShelfOptions = new LinkedHashMap<>();
    for (ShelfSummary s : shelfSummaries) {
      bookShelfOptions.put(
          String.format(
              "%-10s    \u001B[38;5;197m%-2d\u001B[22m\u001B[38;5;38m Books \u001B[0m",
              s.label(), s.bookCount()),
          s.shelfId().toString());
    }

    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("shelfSelected")
            .name("Select a Bookshelf")
            .selectItems(bookShelfOptions)
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();

    selectBookFromShelf(Long.parseLong(result.getContext().get("shelfSelected")));
  }

  public void selectBookFromShelf(Long shelfId) {
    Map<String, String> bookOptions = new LinkedHashMap<>();

    for (BookSummary bookSummary : bookFacade.getBooksForShelf(shelfId)) {
      bookOptions.put(
          String.format("\u001B[38;5;197m%-10s  \u001B[0m", bookSummary.title()),
          String.valueOf(bookSummary.bookId()));
    }

    if (bookOptions.isEmpty()) {
      getTerminal().writer().println("No books found on this shelf .");
      getTerminal().writer().flush();
      return; // end flow for now
    }

    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("shelfSelected")
            .name("Select a Book")
            .selectItems(bookOptions)
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();

    System.out.println();

    getBookDetailsView(Long.parseLong(result.getContext().get("shelfSelected")));
  }

  public void getBookDetailsView(Long bookId) {
    BookDetailView bookDetails = bookFacade.getBookDetails(bookId);
    String res =
        String.format(
            """
                    Title \u001B[38;5;197m%-10s  \u001B[0m
                    Authors \u001B[38;5;197m%-10s  \u001B[0m
                    Bookcase \u001B[38;5;197m%-10s  \u001B[0m
                    Bookshelf \u001B[38;5;197m%-10s  \u001B[0m
                    Book Status \u001B[38;5;197m%-10s  \u001B[0m
                """,
            bookDetails.title(),
            bookDetails.authors(),
            bookDetails.bookcaseLocation(),
            bookDetails.shelfLabel(),
            bookDetails.bookStatus());

    System.out.println(res);
    Map<String, String> checkOutOptions = new LinkedHashMap<>();

    checkOutOptions.put("Yes", "1");
    checkOutOptions.put("No", "2");

    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("optionSelected")
            .name("Would You Like To Check-Out?")
            .selectItems(checkOutOptions)
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();

    if (result.getContext().get("optionSelected").equals("1")) {
      Optional<BookDTO> bookDTO = bookFacade.findBookById(bookId);
      bookFacade.checkOutBook(bookDTO.get());
    }
  }
}
