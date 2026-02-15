package com.penrose.bibby.cli.prompt.application;

import com.penrose.bibby.cli.prompt.api.PromptFacade;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.library.cataloging.author.api.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.api.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.api.ports.inbound.BookFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.stereotype.Component;

@Component
public class CliPromptService implements PromptFacade {
  private final ComponentFlow.Builder componentFlowBuilder;
  private final AuthorFacade authorFacade;
  private final BookFacade bookFacade;
  List<String> scans = new ArrayList<>();
  private final PromptOptions promptOptions;

  public CliPromptService(
      ComponentFlow.Builder componentFlowBuilder,
      ComponentFlow.Builder componentFlowBuilder1,
      AuthorFacade authorFacade,
      BookFacade bookFacade,
      PromptOptions promptOptions) {
    this.componentFlowBuilder = componentFlowBuilder1;
    this.authorFacade = authorFacade;
    this.bookFacade = bookFacade;
    this.promptOptions = promptOptions;
  }

  public boolean promptSearchAgain() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("searchDecision")
            .name("Would you like to search again?")
            .selectItems(promptOptions.yesNoOptions())
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("searchDecision", String.class).equalsIgnoreCase("Yes");
  }

  /**
   * Prompts the user with a confirmation dialog to decide whether to add a book to the library. The
   * method displays a Yes/No choice to the user and processes the response accordingly.
   *
   * @return true if the user confirms to add the book, false otherwise.
   */
  public boolean promptToConfirmBookAddition() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("confirmation")
            .name("Would you like to add this book to the library?")
            .selectItems(promptOptions.yesNoOptions())
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    if (result.getContext().get("confirmation", String.class).equalsIgnoreCase("No")) {
      System.out.println("\u001B[38:5:190mCanceled. Book was not added.\u001B[0m");
      return false;
    }
    return result.getContext().get("confirmation", String.class).equalsIgnoreCase("Yes");
  }

  public List<String> promptMultiScan() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withStringInput("multiScan")
            .name("Multi-Scan >:_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    String scan = result.getContext().get("multiScan", String.class);

    if (!scan.equalsIgnoreCase("done")) {
      scans.add(scan);
      promptMultiScan();
    }
    return scans;
  }

  public AuthorDTO promptForAuthorDetails() {
    ComponentFlow flow;
    flow =
        componentFlowBuilder
            .clone()
            .withStringInput("authorFirstName")
            .name("Author's First Name:_")
            .and()
            .withStringInput("authorLastName")
            .name("Author's Last Name:_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    String firstName = result.getContext().get("authorFirstName", String.class);
    String lastName = result.getContext().get("authorLastName", String.class);
    //        BookRequestDTO bookRequestDTO = new BookRequestDTO(title,firstName, lastName);
    //        bookService.createNewBook(bookRequestDTO);
    return new AuthorDTO(null, firstName, lastName);
  }

  public Long promptForShelfSelection(Long bookCaseId) {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("bookshelf")
            .name("Chose a shelf position")
            .selectItems(promptOptions.bookShelfOptions(bookCaseId))
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    if (result.getContext().get("bookshelf", String.class).equals("cancel")) {
      System.out.println("\u001B[38:5:190mCanceled. No changes were made.\u001B[0m");
      return null;
    }
    return Long.parseLong(result.getContext().get("bookshelf", String.class));
  }

  public Long promptMultipleAuthorConfirmation(AuthorDTO author) {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("chooseAuthor")
            .name("Author Selection")
            .selectItems(promptOptions.authorOptions(author))
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    return Long.parseLong(result.getContext().get("chooseAuthor", String.class));
  }

  /**
   * Prompts the user to enter an ISBN value through a console interface. The method validates the
   * input and ensures it meets the expected format of a 13-digit ISBN starting with "978". The user
   * can abort the input by entering ":q". Invalid inputs are flagged, and the prompt is repeated
   * until a valid value or exit signal is provided.
   *
   * @return The valid ISBN entered by the user as a String, or null if the input process is
   *     canceled.
   */
  public String promptForIsbn() {
    ComponentFlow flow;
    flow =
        componentFlowBuilder
            .clone()
            .withStringInput("isbn")
            .name("ISBN Number (or 'm' for manual ':q' to abort):_")
            .defaultValue("m")
            .next(
                ctx -> {
                  String value = ctx.getResultValue();
                  if (value.equalsIgnoreCase("m")) {
                    System.out.println("\u001B[33mManual ISBN entry selected.\u001B[0m");
                    ctx.setResultValue("m");
                    ctx.setInput("m");
                    ctx.setDefaultValue("m");
                    return "m";
                  } else if (value.equalsIgnoreCase(":q")) {
                    System.out.println("\u001B[31mISBN entry cancelled by user.\u001B[0m");
                    return ":q";
                  } else if (!isbnValidator(value)) {
                    ctx.setResultValue(null);
                    ctx.setInput("");
                    ctx.setDefaultValue("m");
                    return "isbn";
                  }
                  return null;
                })
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("isbn", String.class);
  }

  public boolean isbnValidator(String isbn) {
    if (isbn.startsWith("978") && isbn.length() == 13) {
      return true;
    } else if (isbn.equalsIgnoreCase(":q")) {
      System.out.println("\u001B[31mISBN entry cancelled by user.\u001B[0m");
      return false;
    } else {
      System.out.println(
          "\u001B[31mInvalid ISBN. Please enter a valid 13-digit ISBN starting with '978'.\u001B[0m");
      //            throw new IllegalArgumentException("Invalid ISBN");
      return false;
    }
  }

  /**
   * Prompts the user to select a bookcase from a list of options and returns the selected bookcase
   * ID. If the user chooses to cancel, no changes are made, and the method returns null.
   *
   * @param bookCaseOptions A map of bookcase IDs (as strings) to their corresponding labels
   *     representing the available bookcase options.
   * @return The selected bookcase ID as a Long, or null if the selection was canceled by the user.
   */
  public Long promptForBookcaseSelection(Map<String, String> bookCaseOptions) {
    ComponentFlow flow;
    flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("bookcase")
            .name("Choose a Bookcase:_")
            .selectItems(bookCaseOptions)
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    if (result.getContext().get("bookcase", String.class).equals("cancel")) {
      System.out.println("\u001B[38:5:190mCanceled. No changes were made.\u001B[0m");
      return null;
    }
    return Long.parseLong(result.getContext().get("bookcase", String.class));
  }

  public String promptForBookTitle() {
    ComponentFlow flow;
    flow = componentFlowBuilder.clone().withStringInput("title").name("Book Title:_").and().build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("title", String.class);
  }

  public String promptForBookIsbn() {
    ComponentFlow flow;
    flow = componentFlowBuilder.clone().withStringInput("isbn").name("ISBN:_").and().build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("isbn", String.class);
  }

  /**
   * Prompt the user for the number of authors for a book.
   *
   * @return The number of authors as an integer.
   */
  public int promptForBookAuthorCount() {
    ComponentFlow flow;
    flow =
        componentFlowBuilder
            .clone()
            .withStringInput("author_count")
            .name("Number of Authors:_")
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    String authorCount = result.getContext().get("author_count", String.class);
    return Integer.parseInt(authorCount);
  }

  public String promptForSearchType() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("searchType")
            .name("Select a search method:")
            .selectItems(promptOptions.searchOptions())
            .max(10)
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("searchType", String.class);
  }

  public String promptForBookcaseLocation() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("bookcaseLocation")
            .name("Select a location:")
            .selectItems(promptOptions.bookcaseLocationOptions())
            .max(5)
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();

    if (result.getContext().get("bookcaseLocation", String.class).equals("new")) {
      return promptNewBookcaseLocation();
    }
    if (result.getContext().get("bookcaseLocation").equals("cancel")) {
      System.out.println("\u001B[38:5:190mCanceled. Bookcase was not created.\u001B[0m");
      return null;
    }

    return result.getContext().get("bookcaseLocation", String.class);
  }

  public String promptNewBookcaseLocation() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withStringInput("newLocation")
            .name("Location (e.g., Office, Bedroom, Basement):_")
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("newLocation", String.class);
  }

  public boolean promptForPlacementDecision() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("placementDecision")
            .name("Assign a shelf now?")
            .selectItems(promptOptions.yesNoOptions())
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("placementDecision", String.class).equalsIgnoreCase("Yes");
  }

  public String promptForBookEditSelection() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("bookEditSelection")
            .name("Select metadata to edit: ")
            .selectItems(promptOptions.metaDataSelection())
            .max(8)
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();

    return result.getContext().get("bookEditSelection", String.class);
  }

  public String promptForEditPublisher() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withStringInput("newPublisher")
            .name("Enter New Publisher (':q' to quit):_")
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    return result.getContext().get("newPublisher", String.class);
  }

  public boolean promptToConfirmChange(String fieldName) {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("chooseSelection")
            .name("Confirm Publisher Changes: " + fieldName)
            .selectItems(promptOptions.yesNoOptions())
            .and()
            .build();
    ComponentFlow.ComponentFlowResult result = flow.run();
    if (result.getContext().get("chooseSelection", String.class).equalsIgnoreCase("No")) {
      System.out.println("\u001B[38:5:190mCanceled. No changes were made.\u001B[0m");
      return false;
    }
    return true;
  }

  public boolean promptForDuplicateConfirmation() {
    ComponentFlow flow =
        componentFlowBuilder
            .clone()
            .withSingleItemSelector("duplicateConfirmation")
            .name("A book with this ISBN already exists. Add another copy?")
            .selectItems(promptOptions.yesNoOptions())
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    if (result.getContext().get("duplicateConfirmation", String.class).equalsIgnoreCase("No")) {
      System.out.println("\u001B[38:5:190mCanceled. Book was not added.\u001B[0m");
      return false;
    }
    return result.getContext().get("duplicateConfirmation", String.class).equalsIgnoreCase("Yes");
  }
}
