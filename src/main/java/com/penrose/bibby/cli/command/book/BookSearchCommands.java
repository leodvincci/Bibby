package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.ui.BookcardRenderer;
import com.penrose.bibby.library.cataloging.author.api.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.api.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.api.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.api.ports.inbound.BookFacade;
import com.penrose.bibby.library.stacks.bookcase.api.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.api.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.api.ports.inbound.ShelfFacade;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
@Command(command = "book", group = "Book Search Commands")
public class BookSearchCommands {
  private final CliPromptService cliPrompt;
  private final BookcardRenderer bookcardRenderer;
  private final BookFacade bookFacade;
  private final BookcaseFacade bookcaseFacade;
  private final ShelfFacade shelfFacade;
  private final AuthorFacade authorFacade;
  private final BookCreateCommands bookCreateCommands;
  Logger log = org.slf4j.LoggerFactory.getLogger(BookCreateCommands.class);

  public BookSearchCommands(
      CliPromptService cliPrompt,
      BookcardRenderer bookcardRenderer,
      BookFacade bookFacade,
      BookcaseFacade bookcaseFacade,
      ShelfFacade shelfFacade,
      AuthorFacade authorFacade,
      BookCreateCommands bookCreateCommands) {

    this.cliPrompt = cliPrompt;
    this.bookcardRenderer = bookcardRenderer;
    this.bookFacade = bookFacade;
    this.bookcaseFacade = bookcaseFacade;
    this.shelfFacade = shelfFacade;
    this.authorFacade = authorFacade;
    this.bookCreateCommands = bookCreateCommands;
  }

  @Command(
      command = "search",
      description = "Find a book by title, author, genre, or location using an interactive prompt.")
  public void searchForBook() throws InterruptedException {
    String searchType = cliPrompt.promptForSearchType();
    if (searchType.equalsIgnoreCase("author")) {
      searchByAuthor();
    } else if (searchType.equalsIgnoreCase("title")) {
      searchByTitle();
    } else if (searchType.equalsIgnoreCase("isbn")) {
      searchByIsbn();
    }
  }

  private void searchByIsbn() {
    String shelfLocation = "";
    String bookcaseLocation = "";
    System.out.println("\n\u001B[95mSearch by ISBN");
    String isbn = cliPrompt.promptForIsbn();
    if (isbn == null) {
      System.out.println("NULL ISBN RETURNED");
      return;
    }

    BookDTO bookDTO = bookFacade.findBookByIsbn(isbn);

    if (bookDTO == null) {
      System.out.println("\n\u001B[36m</>\u001B[0m: No book found with ISBN: " + isbn + "\n");
    } else {

      System.out.println("\n\u001B[36m</>\u001B[0m: Book found: \n");
      if (bookDTO.shelfId() == null) {
        shelfLocation = "PENDING / NOT SET";
        bookcaseLocation = "PENDING / NOT SET";
      } else {
        Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
        Optional<BookcaseDTO> bookcaseDTO =
            bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
        bookcaseLocation = bookcaseDTO.get().location();
        shelfLocation = shelfDTO.get().shelfLabel();
      }
      Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());

      String bookCard =
          bookcardRenderer.createBookCard(
              bookDTO.title(),
              bookDTO.isbn(),
              authors.toString(),
              bookDTO.publisher(),
              bookcaseLocation,
              shelfLocation,
              "PENDING / NOT SET");
      System.out.println(bookCard);
    }
  }

  public void searchByAuthor() throws InterruptedException {
    System.out.println("\n\u001B[95mSearch by Author");
  }

  public void searchByTitle() throws InterruptedException {
    System.out.println("\n\u001B[95mSearch by Title");
    String title = cliPrompt.promptForBookTitle();

    log.info("Searching for book with title: {}", title);
    BookDTO bookDTO = bookFacade.findBookByTitle(title);

    if (bookDTO == null) {

      bookcardRenderer.printNotFound(title);
      return;
    } else if (bookDTO.shelfId() == null) {
      String bookCard =
          bookcardRenderer.createBookCard(
              bookDTO.title(),
              bookDTO.isbn(),
              authorFacade.findByBookId(bookDTO.id()).toString(),
              bookDTO.publisher(),
              "PENDING / NOT SET",
              "PENDING / NOT SET",
              "PENDING / NOT SET");
      System.out.println("\n\u001B[36m</>\u001B[0m: Found it! Here are the details:\n");
      System.out.println(bookCard);
    } else {
      Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
      Optional<BookcaseDTO> bookcaseDTO =
          bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
      System.out.println(authorFacade.findByBookId(bookDTO.id()).toString());
      String bookCard =
          bookcardRenderer.createBookCard(
              bookDTO.title(),
              bookDTO.isbn(),
              authorFacade.findByBookId(bookDTO.id()).toString(),
              bookDTO.publisher(),
              bookcaseDTO.get().location(),
              shelfDTO.get().shelfLabel(),
              bookcaseDTO.get().location());
      System.out.println("\n\u001B[36m</>\u001B[0m: Found it! Here are the details:\n");
      System.out.println(bookCard);
    }
    if (cliPrompt.promptSearchAgain()) {
      searchForBook();
    }
  }
}
