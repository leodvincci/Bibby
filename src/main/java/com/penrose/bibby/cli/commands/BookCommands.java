package com.penrose.bibby.cli.commands;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.ShellOption;

import com.penrose.bibby.cli.prompt.contracts.PromptFacade;
import com.penrose.bibby.library.book.contracts.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.book.contracts.dtos.BookRequestDTO;
import com.penrose.bibby.library.bookcase.contracts.dtos.BookcaseDTO;
import com.penrose.bibby.library.bookcase.contracts.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.shelf.contracts.ports.inbound.ShelfFacade;

import com.penrose.bibby.cli.prompt.application.CliPromptService;

import java.util.*;


@ShellComponent
@Command(command = "book", group = "Book Commands")
public class BookCommands extends AbstractShellComponent {

    private final AuthorFacade authorFacade;
    private final BookFacade bookFacade;
    private final BookcaseFacade bookcaseFacade;
    private final PromptFacade promptFacade;
    private final ShelfFacade shelfFacade;
    private final CliPromptService cliPrompt;
    private final ComponentFlow.Builder componentFlowBuilder;


    public BookCommands(ComponentFlow.Builder componentFlowBuilder,
                        AuthorFacade authorFacade,
                        ShelfFacade shelfFacade,
                        CliPromptService cliPrompt,
                        BookFacade bookFacade,
                        BookcaseFacade bookcaseFacade, PromptFacade promptFacade) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.authorFacade = authorFacade;
        this.shelfFacade = shelfFacade;
        this.cliPrompt = cliPrompt;
        this.bookFacade = bookFacade;
        this.bookcaseFacade = bookcaseFacade;
        this.promptFacade = promptFacade;
    }

    // ───────────────────────────────────────────────────────────────────
    //
    //                        Book Create Commands
    //
    //
    // ───────────────────────────────────────────────────────────────────

    @Command(command = "scan", description = "Scan a book's ISBN to add it to your library database",group = "Book Commands")
    public void scanBook(@Option(required = false, defaultValue = "single", description = "scan multiple books") @ShellOption (value = {"--type"}) String multi) {

        multi = multi == null ? "multi" : multi;

        if(multi.equalsIgnoreCase("multi")){
            multiBookScan();
        }

        if(multi.equalsIgnoreCase("single")) {
            System.out.println("\n\u001B[95mSingle Book Scan");
            String isbn = cliPrompt.promptForIsbnScan();
            if(!cliPrompt.isbnValidator(isbn)){
                return;
            }

            BookMetaDataResponse bookMetaDataResponse = bookFacade.findBookMetaDataByIsbn(isbn);
            if(bookMetaDataResponse == null){
                System.out.println("\n\u001B[36m</>\033[0m: No book found with ISBN: " + isbn + "\n");
            }else if (cliPrompt.promptBookConfirmation()) {

                bookFacade.createBookFromMetaData(bookMetaDataResponse, isbn, null);

                System.out.println("\n\u001B[36m</>\033[0m: Book added to the library database successfully!");
            }
        }
    }


    @Command(command = "register", description = "register a new book to your library")
    public void registerBook(@Option(required = false, defaultValue = "scan") @ShellOption(value = {"--type"}) String scan, @Option(required = false) @ShellOption(value = "-type") String multi) throws InterruptedException {
        if(scan == null && multi == null){
            scanBook("multi");
            return;
        }else if(multi != null){
            multiBookScan();
            return;

        }
        String title = cliPrompt.promptForBookTitle();
        int authorCount = cliPrompt.promptForBookAuthorCount();
        List<AuthorDTO> authors = new ArrayList<>();

        for (int i = 0; i < authorCount; i++) {
            authors.add(cliPrompt.promptForAuthor());
        }

        String isbn = cliPrompt.promptForBookIsbn();

        BookRequestDTO bookRequestDTO = new BookRequestDTO(title, authors,isbn);
        bookFacade.createNewBook(bookRequestDTO);

        System.out.println("\n\u001B[36m</>\033[0m: Ah, a brand-new book...");
        System.out.printf("\u001B[36m</>\033[0m:'%s', right?", title);
        System.out.println("\n\u001B[36m</>\033[0m: I’ll handle adding it to the database and prepare it for the library.");
    }


    @Command(command = "shelf", description = "Place a book on a shelf or move it to a new location.")
    public void addToShelf(){
        String title = cliPrompt.promptForBookTitle();
        BookDTO bookDTO = bookFacade.findBookByTitle(title);
        if(bookDTO == null){
            System.out.println("Book Not Found In Library");
        }else {
            Long bookCaseId = cliPrompt.promptForBookCase(bookCaseOptions());
            Long shelfId = cliPrompt.promptForShelf(bookCaseId);

            //Checks if shelf is full/capacity reached
            Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(shelfId);
//            Boolean isFull = shelfFacade.isFull(shelfDTO.get());
            if(shelfDTO.get().bookCapacity() <= shelfDTO.get().bookIds().size()){
                throw new IllegalStateException("Shelf is full");
            }else{

                bookFacade.setShelfForBook(bookDTO.id(), shelfId);

                System.out.println("Added Book To the Shelf!");
            }
        }
    }



    private void multiBookScan() {
        Long bookcaseId = cliPrompt.promptForBookCase(bookCaseOptions());
        Long shelfId = cliPrompt.promptForShelf(bookcaseId);
        System.out.println("Scanning multiple books...");
        List<String> scans = cliPrompt.promptMultiScan();

        for (String isbn : scans) {
            System.out.println("Scanned ISBN: " + isbn);

            BookMetaDataResponse bookMetaDataResponse = bookFacade.findBookMetaDataByIsbn(isbn);
            bookFacade.createBookFromMetaData(bookMetaDataResponse, isbn, shelfId);
            System.out.println("\n\u001B[36m</>\033[0m:" + bookMetaDataResponse.title() + " added to Library!");
            System.out.println(scans.size() + " books were added to the library.");
        }
    }




    public String createBookCard(String title, String id, String author, String location) {

        // %-42s ensures the text is left-aligned and padded to 42 characters
        // The emojis take up extra visual space, so adjusted padding slightly
        return """
                ╭──────────────────────────────────────────────────────────────────────────────╮
                │  📖 %-73s│
                ├──────────────────────────────────────────────────────────────────────────────┤
                │  ID: %-31s                                         │
                │  Author: %-31s                                     │
                │                                                                              │
                │📍Location: %-35s                               │
                ╰──────────────────────────────────────────────────────────────────────────────╯
        """.formatted(title, id, author, location);
    }

// Usage:
// System.out.println(createBookCard("Building Microservices", "978-1491950357", "Sam Newman", "PENDING / NOT SET"));


    // ───────────────────────────────────────────────────────────────────
    //
    //                       Book Search Commands
    //
    //
    // ───────────────────────────────────────────────────────────────────

    @Command(command = "find", description = "Find a book by title, author, genre, or location using an interactive prompt.")
    public void findBook() throws InterruptedException {
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
            System.out.println("Book Details:");
            System.out.println("Title: " + bookDTO.title());
            Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());
            System.out.println("Author(s): " + authors);
            System.out.println("ISBN: " + bookDTO.isbn());
            if (bookDTO.shelfId() != null) {
                Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());

                Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());

                System.out.println("Location: Bookcase " + bookcaseDTO.get().bookcaseLabel() + ", Shelf " + shelfDTO.get().shelfLabel());
            } else {
                System.out.println("Location: Not Shelved");
            }
        }
    }

    public void searchByAuthor() throws InterruptedException {
        System.out.println("\n\u001B[95mSearch by Author");
        askBookCheckOut();
    }

    public void searchByTitle() throws InterruptedException {



        System.out.println("\n\u001B[95mSearch by Title");
        String title = cliPrompt.promptForBookTitle();
        BookDTO bookDTO = bookFacade.findBookByTitle(title);

        if (bookDTO == null) {
            System.out.println("\n\u001B[36m</>\u001B[0m:I just flipped through every shelf — no luck this time.\n");
        }else if(bookDTO.shelfId() == null){
            String bookCard = createBookCard(
                    bookDTO.title(),
                    bookDTO.id().toString(),
                    authorFacade.findByBookId(bookDTO.id()).toString(),
                    "PENDING / NOT SET"
            );
            System.out.println(bookCard);
        }else{
            Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
            Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
            String bookCard = createBookCard(
                    bookDTO.title(),
                    bookDTO.id().toString(),
                    authorFacade.findByBookId(bookDTO.id()).toString(),
                    "Bookcase " + bookcaseDTO.get().bookcaseLabel() + ", Shelf " + shelfDTO.get().shelfLabel()
            );
            System.out.println(bookCard);
        }
        if (cliPrompt.promptSearchAgain()){
            findBook();
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
                .selectItems(yesNoOptions())
                .and().build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        String checkOutResponse = result.getContext().get("checkOutDecision",String.class);
        if(checkOutResponse.equalsIgnoreCase("yes")){
            checkOutBookByID();
        }else {
            System.out.println("\n\u001B[36m</>\u001B[0m:Cool, I’ll just… put all these back by myself...and whisper *maybe next time* to the shelves... Again.\n");
        }
    }

    private Map<String, String> yesNoOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Yes  — \u001B[32mLet's Do It\n\u001B[0m", "Yes");
        options.put("No  —  \u001B[32mNot this time\n\u001B[0m", "No");
        return options;
    }

    @Command(command = "suggest-shelf", description = "Use AI to recommend optimal shelf placement for a book.")
    public void suggestBookShelf(){
        System.out.println("Book should be placed on Shelf: G-16");
    }

    private Map<String, String> bookCaseOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        List<BookcaseDTO> bookcaseDTOs  = bookcaseFacade.getAllBookcases();
        for(BookcaseDTO b : bookcaseDTOs){
            options.put(b.bookcaseLabel(), b.bookcaseId().toString());
        }

        return options;
    }

    public boolean addScanResultCommand(BookMetaDataResponse bookMetaData,String isbn) {
        String title = bookMetaData.title();
        String authors =bookMetaData.authors().toString();
//        String publishingDate = bookMetaData.pulblishedDate();
//        String categories = bookMetaData.categories().toString();
        String description = bookMetaData.description();
//        System.out.println("\n\u001B[36m</>\u001B[0m:");
//
//        System.out.printf(""
//                + "========================================\n"
//                + "📚  Book Metadata\n"
//                + "========================================\n"
//                + "\n"
//                + "ISBN:              %s\n"
//                + "Title:             %s\n"
//                + "Authors:           %s\n"
//                + "\n"
//                + "Description:\n"
//                + "%s\n"
//                + "\n"
//                + "========================================\n",isbn,title,authors,description);
        System.out.println();
        return cliPrompt.promptBookConfirmation();
    }
}
