package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.cli.ui.BookcardRenderer;
import org.slf4j.Logger;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.ShellOption;

import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookRequestDTO;
import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;

import com.penrose.bibby.cli.prompt.application.CliPromptService;

import java.util.*;


/**
 * The BookCommands class provides a set of commands for managing books in a library
 * system. These commands allow users to interact with the book inventory, add new
 * books, place books on shelves, search for books, and check books in and out.
 *
 * This class integrates with various facades and services, such as AuthorFacade,
 * BookFacade, BookcaseFacade, ShelfFacade, and CliPromptService, to perform its operations.
 * It uses a component-based UI flow builder and rendering tools to enhance interactivity.
 */
@ShellComponent
@Command(command = "book", group = "Book Commands")
public class BookCommands extends AbstractShellComponent {

    private final AuthorFacade authorFacade;
    private final BookFacade bookFacade;
    private final BookcaseFacade bookcaseFacade;
    private final ShelfFacade shelfFacade;
    private final CliPromptService cliPrompt;
    private final ComponentFlow.Builder componentFlowBuilder;
    private final PromptOptions promptOptions;
    private final BookcardRenderer bookcardRenderer;
    Logger log = org.slf4j.LoggerFactory.getLogger(BookCommands.class);

    public BookCommands(ComponentFlow.Builder componentFlowBuilder,
                        AuthorFacade authorFacade,
                        ShelfFacade shelfFacade,
                        CliPromptService cliPrompt,
                        BookFacade bookFacade,
                        BookcaseFacade bookcaseFacade, PromptOptions promptOptions, BookcardRenderer bookcardRenderer) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.authorFacade = authorFacade;
        this.shelfFacade = shelfFacade;
        this.cliPrompt = cliPrompt;
        this.bookFacade = bookFacade;
        this.bookcaseFacade = bookcaseFacade;
        this.promptOptions = promptOptions;
        this.bookcardRenderer = bookcardRenderer;
    }




//
//    private void multiBookScan() {
//        log.info("Initiating multiBookScan for Multi Scan.");
//        Long bookcaseId = cliPrompt.promptForBookCase(bookCaseOptions());
//        Long shelfId = cliPrompt.promptForShelf(bookcaseId);
//        System.out.println("\n\u001B[95mMulti-Book Scan");
//        List<String> scans = cliPrompt.promptMultiScan();
//
//        for (String isbn : scans) {
//            System.out.println("Scanned ISBN: " + isbn);
//
//            BookMetaDataResponse bookMetaDataResponse = bookFacade.findBookMetaDataByIsbn(isbn);
//            bookFacade.createBookFromMetaData(bookMetaDataResponse, isbn, shelfId);
//            System.out.println("\n\u001B[36m</>\033[0m:" + bookMetaDataResponse.title() + " added to Library!");
//            System.out.println(scans.size() + " books were added to the library.");
//        }
//    }






// Usage:
// System.out.println(createBookCard("Building Microservices", "978-1491950357", "Sam Newman", "PENDING / NOT SET"));


    // ───────────────────────────────────────────────────────────────────
    //
    //                       Book Search Commands
    //
    //
    // ───────────────────────────────────────────────────────────────────

    @Command(command = "search", description = "Find a book by title, author, genre, or location using an interactive prompt.")
    public void searchForBook() throws InterruptedException {
        String searchType = cliPrompt.promptForSearchType();
        if (searchType.equalsIgnoreCase("author")){
            searchByAuthor();
        }else if(searchType.equalsIgnoreCase("title")){
            searchByTitle();
        }else if(searchType.equalsIgnoreCase("isbn")){
            searchByIsbn();
        }
    }


    private void searchByIsbn() {
        String shelfLocation = "";
        String bookcaseLocation = "";
        System.out.println("\n\u001B[95mSearch by ISBN");
        String isbn = cliPrompt.promptForIsbnScan();
        if(isbn == null){
            System.out.println("NULL ISBN RETURNED");
            return;
        }

        BookDTO bookDTO = bookFacade.findBookByIsbn(isbn);

        if (bookDTO == null) {
            System.out.println("\n\u001B[36m</>\u001B[0m: No book found with ISBN: " + isbn + "\n");
        } else {

                System.out.println("\n\u001B[36m</>\u001B[0m: Book found: \n");
                if(bookDTO.shelfId() == null){
                    shelfLocation = "PENDING / NOT SET";
                    bookcaseLocation = "PENDING / NOT SET";
                }else{
                    Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
                    Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
                    bookcaseLocation = bookcaseDTO.get().bookcaseLabel();
                    shelfLocation = shelfDTO.get().shelfLabel();
                }
                Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());

                String bookCard = bookcardRenderer.createBookCard(
                        bookDTO.title(),
                        bookDTO.isbn(),
                        authors.toString(),
                        bookDTO.publisher(),
                        bookcaseLocation,
                        shelfLocation,
                        "PENDING / NOT SET"
                        );
                System.out.println(bookCard);

        }
    }



    public void searchByAuthor() throws InterruptedException {
        System.out.println("\n\u001B[95mSearch by Author");
        askBookCheckOut();
    }




    public void searchByTitle() throws InterruptedException {
        System.out.println("\n\u001B[95mSearch by Title");
        String title = cliPrompt.promptForBookTitle();

        log.info("Searching for book with title: {}", title);
        BookDTO bookDTO = bookFacade.findBookByTitle(title);

        if (bookDTO == null) {

            bookcardRenderer.printNotFound(title);
            return;
        }else if(bookDTO.shelfId() == null){
            String bookCard = bookcardRenderer.createBookCard(
                    bookDTO.title(),
                    bookDTO.isbn(),
                    authorFacade.findByBookId(bookDTO.id()).toString(),
                    bookDTO.publisher(),
                    "PENDING / NOT SET",
                    "PENDING / NOT SET",
                    "PENDING / NOT SET"

            );
            System.out.println("\n\u001B[36m</>\u001B[0m: Found it! Here are the details:\n");
            System.out.println(bookCard);
        }else{
            Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
            Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
            System.out.println(authorFacade.findByBookId(bookDTO.id()).toString());
            String bookCard = bookcardRenderer.createBookCard(
                    bookDTO.title(),
                    bookDTO.isbn(),
                    authorFacade.findByBookId(bookDTO.id()).toString(),
                    bookDTO.publisher(),
                    bookcaseDTO.get().bookcaseLabel(),
                    shelfDTO.get().shelfLabel(),
                    bookcaseDTO.get().location()
            );
            System.out.println("\n\u001B[36m</>\u001B[0m: Found it! Here are the details:\n");
            System.out.println(bookCard);
        }
        if (cliPrompt.promptSearchAgain()){
            searchForBook();
        }
    }
//
//    @Command(command = "list", description = "View all books with optional filters and sorting.")
//    public void listBooks(){
//        System.out.println("Listing all books /w filter");
//    }

    // ───────────────────────────────────────────────────────────────────
    //
    //                       Book Update Commands
    //
    //
    // ───────────────────────────────────────────────────────────────────
    @Command(command = "check-out", description = "Check-Out a book from the library")
    public void checkOutBook(){
        String bookTitle = cliPrompt.promptForBookTitle();
        BookDTO bookDTO = bookFacade.findBookByTitle(bookTitle);
        String bookcaseName = "N.A";
        String shelfName ="N.A";
        if(bookDTO == null){
            System.out.println("Book Not Found.");
        }else if(bookDTO.shelfId() != null){
            Optional<ShelfDTO> shelf = shelfFacade.findShelfById(bookDTO.shelfId());
            Optional<BookcaseDTO> bookcase = bookcaseFacade.findBookCaseById(shelf.get().bookcaseId());
            bookcaseName = bookcase.get().bookcaseLabel();
            shelfName = shelf.get().shelfLabel();
        }if (bookDTO.availabilityStatus().toString().equals("CHECKED_OUT")){
            System.out.println(
                    """
                    
                    \u001B[38;5;63m  .---.
                    \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "This one’s already off the shelf. No double-dipping on checkouts."
                    \u001B[38;5;63m  \\|=|/
                    
                    """);
        }else{
            Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());
            System.out.println(String.format("""
                    \n\u001B[32mConfirm Checkout\n\u001B[0m
                            \033[31mTitle\u001B[0m %s
                            \033[31mAuthor/s\u001B[0m %s
                            
                            \033[31mStatus %s
                            
                            \033[31mBookcase\u001B[0m %s
                            \033[31mShelf\u001B[0m %s
                    """,bookDTO.title(), authors, bookDTO.availabilityStatus(), bookcaseName ,shelfName));
            ComponentFlow confirmationFlow = componentFlowBuilder.clone()
                    .withStringInput("isConfirmed")
                    .name("y or n:_ ")
                    .and().build();
            ComponentFlow.ComponentFlowResult confirmationResult = confirmationFlow.run();

            if (confirmationResult.getContext().get("isConfirmed").equals("y")){
                bookFacade.checkOutBook(bookDTO);
                System.out.println(
                        String.format("""
                        
                        \u001B[38;5;63m  .---.
                        \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "All set — \u001B[38;5;42m %s \u001B[38;5;15m is checked out and ready to go with you. \033[36m"
                        \u001B[38;5;63m  \\|=|/
                        
                        """,bookTitle));
            }else{
                System.out.println(
                        """
                        
                        \u001B[38;5;63m  .---.
                        \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "Cool, I’ll just… put this back by myself...and whisper *maybe next time* to the shelves... Again."
                        \u001B[38;5;63m  \\|=|/
                        
                        """);
            }
        }

    }

    @Command(command = "check-in",description = "Return a borrowed book to the library and update its shelf placement.")
    public void checkInBook(){
        String bookTitle = cliPrompt.promptForBookTitle();
        BookDTO bookDTO = bookFacade.findBookByTitle(bookTitle);

        String bookcaseLabel = "No Assigned Bookcase";
        String bookshelfLabel = "No Assigned Bookshelf";
        if(bookDTO == null){
            System.out.println("Book Not Found");
        }else if(bookDTO.shelfId() != null){
            Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
            Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
            bookcaseLabel = bookcaseDTO.get().bookcaseLabel();
            bookshelfLabel = shelfDTO.get().shelfLabel();
        }
        Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());

        System.out.println(String.format("""
                    \n\u001B[32mConfirm Checkin\n\u001B[0m
                            \033[31mTitle\u001B[0m %s
                            \033[31mAuthor/s\u001B[0m %s
                            
                            \033[31mStatus %s
                            
                            \033[31mBookcase\u001B[0m %s
                            \033[31mShelf\u001B[0m %s
                    """,bookDTO.title(), authors, bookDTO.availabilityStatus(), bookcaseLabel ,bookshelfLabel));

        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("isConfirmed" )
                .name("y or n:_ ")
                .and().build();
        ComponentFlow.ComponentFlowResult result;
        result = flow.run();

        if(result.getContext().get("isConfirmed").equals("y")){
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
        componentFlow = componentFlowBuilder.clone()
                .withStringInput("bookID")
                .name("Book ID#:_ ")
                .and().build();

        componentFlow.run();
        System.out.println("\n\u001B[36m</>\u001B[0m:Don’t forget to check it back in… or at least feel guilty about it.\n");
    }

    public void askBookCheckOut() throws InterruptedException {
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                        .withSingleItemSelector("checkOutDecision")
                                .name("Want to check one out, or just window-shopping the shelves again?")
                .selectItems(promptOptions.yesNoOptions())
                .and().build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        String checkOutResponse = result.getContext().get("checkOutDecision",String.class);
        if(checkOutResponse.equalsIgnoreCase("yes")){
            checkOutBookByID();
        }else {
            System.out.println("\n\u001B[36m</>\u001B[0m:Cool, I’ll just… put all these back by myself...and whisper *maybe next time* to the shelves... Again.\n");
        }
    }
}
