package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.api.ports.inbound.BookFacade;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.api.ports.inbound.ShelfFacade;
import java.util.Optional;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
@Command(command = "book", group = "Book Placement Commands")
public class BookPlacementCommands {
  private final BookFacade bookFacade;
  private final ShelfFacade shelfFacade;
  private final CliPromptService cliPrompt;
  private final PromptOptions promptOptions;

  public BookPlacementCommands(
      BookFacade bookFacade,
      ShelfFacade shelfFacade,
      CliPromptService cliPrompt,
      PromptOptions promptOptions) {
    this.bookFacade = bookFacade;
    this.shelfFacade = shelfFacade;
    this.cliPrompt = cliPrompt;
    this.promptOptions = promptOptions;
  }

  @Command(
      command = "shelve",
      description =
          """
                    \u001B[38;5;3mPlace a book onto a shelf by ISBN; creates it first if missing. (Create-or-place.)
                    \u001B[0m
                    """,
      group = "Book Placement Commands")
  public void addToShelf() {
    // What if the library has multiple copies of the same book title?
    // For now, we will assume titles are unique
    // todo(priority 2): prompt user to select from multiple copies if found
    String title = cliPrompt.promptForBookTitle();
    BookDTO bookDTO = bookFacade.findBookByTitle(title);
    if (bookDTO == null) {
      System.out.println("Book Not Found In Library");
    } else {
      Long bookCaseId = cliPrompt.promptForBookcaseSelection(promptOptions.bookCaseOptions());
      Long newShelfId = cliPrompt.promptForShelfSelection(bookCaseId);

      // Checks if shelf is full/capacity reached
      Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(newShelfId);
      //            Boolean isFull = shelfFacade.isFull(shelfDTO.get());
      if (shelfDTO.get().bookCapacity() <= shelfDTO.get().bookIds().size()) {
        throw new IllegalStateException("Shelf is full");
      } else {

        bookFacade.updateTheBooksShelf(bookDTO, newShelfId);

        System.out.println("Added Book To the Shelf!");
      }
    }
  }

  @Command(
      command = "place",
      description =
          """
              \u001B[38;5;185mAssign an existing book to a shelf (or move it). Choose by ISBN or from unplaced books. Updates shelf location only—does not create new book records.
              \u001B[0m""",
      group = "Book Placement Commands")
  public void bookPlacement() {
    System.out.println("Book placement command executed.");
  }

  @Command(
      command = "cart",
      description =
          """
              \u001B[38;5;185mShow books waiting to be shelved (unplaced books). Place books from the cart to their designated shelves.
              \u001B[0m""",
      group = "Book Placement Commands")
  public void bookCartPlacement() {
    System.out.println("Book cart placement command executed.");
  }
}
