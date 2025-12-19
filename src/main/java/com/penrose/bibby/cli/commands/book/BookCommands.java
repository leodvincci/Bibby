package com.penrose.bibby.cli.commands.book;

import com.penrose.bibby.cli.ConsoleColors;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
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
    Logger log = org.slf4j.LoggerFactory.getLogger(BookCommands.class);

    public BookCommands(ComponentFlow.Builder componentFlowBuilder,
                        AuthorFacade authorFacade,
                        ShelfFacade shelfFacade,
                        CliPromptService cliPrompt,
                        BookFacade bookFacade,
                        BookcaseFacade bookcaseFacade, PromptOptions promptOptions) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.authorFacade = authorFacade;
        this.shelfFacade = shelfFacade;
        this.cliPrompt = cliPrompt;
        this.bookFacade = bookFacade;
        this.bookcaseFacade = bookcaseFacade;
        this.promptOptions = promptOptions;
    }

    // ───────────────────────────────────────────────────────────────────
    //
    //                        Book Create Commands
    //
    //
    // ───────────────────────────────────────────────────────────────────

    @Command(command = "scan", description = "Scan a book's ISBN to add it to your library database",group = "Book Commands")
    public void scanBook(
            @ShellOption (defaultValue = "single") boolean multi) {

//        if (multi) multiBookScan();
        log.info("Initiating scanBook for Single Scan.");
        System.out.println("\n\u001B[95mSingle Book Scan");
        String isbn = cliPrompt.promptForIsbnScan();
        if (!cliPrompt.isbnValidator(isbn)) return;
        BookMetaDataResponse bookMetaDataResponse = bookFacade.findBookMetaDataByIsbn(isbn);
        log.debug("BookMetaDataResponse received: {}", bookMetaDataResponse);


        log.debug("Authors verified/created for book.");
        log.info(bookMetaDataResponse.toString());
        String bookcard = createBookCard(bookMetaDataResponse.title(),
                bookMetaDataResponse.isbn(),
                bookMetaDataResponse.authors().toString(),
                bookMetaDataResponse.publisher(),
                "PENDING / NOT SET",
                "PENDING / NOT SET");
        System.out.println(bookcard);

        if (cliPrompt.promptBookConfirmation()) {
            Long bookcaseId = cliPrompt.promptForBookCase(promptOptions.bookCaseOptions());
            if(bookcaseId == null){
                return;
            }
            Long shelfId = cliPrompt.promptForShelf(bookcaseId);
            if(shelfId == null){
                return;
            }
            List<Long> authorIds = createAuthorsFromMetaData(bookMetaDataResponse.authors());

            bookFacade.createBookFromMetaData(bookMetaDataResponse, authorIds, isbn, shelfId);
            String updatedBookCard = createBookCard(bookMetaDataResponse.title(),
                    bookMetaDataResponse.isbn(),
                    bookMetaDataResponse.authors().toString(),
                    bookMetaDataResponse.publisher(),
                    bookcaseFacade.findBookCaseById(bookcaseId).get().bookcaseLabel(),
                    shelfFacade.findShelfById(shelfId).get().shelfLabel()
            );
            System.out.println(updatedBookCard);
        }

    }


    @Command(command = "new", description = "Create a new book entry")
    public void registerBook(
            @ShellOption(defaultValue = "false") boolean scan,
            @ShellOption(defaultValue = "false") boolean multi) {

        log.info("Starting new book registration process.");
        log.debug("{} {}", scan, multi);

        ScanMode mode = ScanMode.from(scan, multi);
        switch(mode){
            case SINGLE -> scanBook(false);
//            case MULTI -> multiBookScan();
            case NONE -> createBookManually();
        }
    }


    public List<AuthorDTO> createAuthors(){
        int numberOfAuthors = cliPrompt.promptForBookAuthorCount();
        List<AuthorDTO> authors = new ArrayList<>();
        for (int i = 0; i < numberOfAuthors; i++) {
            AuthorDTO authorDTO = cliPrompt.promptForAuthor();
            log.info("Collected Author Details: {}", authorDTO);

            if(authorFacade.authorExistFirstNameLastName(authorDTO.firstName(),authorDTO.lastName())){
                log.info("Author already exists: {} {}", authorDTO.firstName(), authorDTO.lastName());
                System.out.println("Multiple Authors with this name.\n");
                Long authorId = cliPrompt.promptMultipleAuthorConfirmation(authorDTO);
                log.info("Existing author selected with ID: {}", authorId);
                //todo: 0 is a magic number here, refactor needed
                if(authorId == 0){
                    log.info("Creating new author as per user request: {} {}", authorDTO.firstName(), authorDTO.lastName());
                    authors.add(authorFacade.saveAuthor(authorDTO));
                    log.info("Author saved: {}", authors.get(i));
                }else{
                    log.info("Fetching existing author with ID: {}", authorId);

                    AuthorDTO existingAuthor = authorFacade.findById(authorId);
                    authors.add(existingAuthor);
                    log.info("Existing author added to list: {}", existingAuthor);
                }
            }else{
                log.info("Creating new author: {} {}", authorDTO.firstName(), authorDTO.lastName());
                authors.add(authorFacade.saveAuthor(authorDTO));
                log.info("Author saved: {}", authors.get(i));
            }

        }
        log.info("Authors Created and Returned: {}", authors);
        return authors;
    }

    /**
     * Checks if an author exists in the system with the given first and last name.
     *
     * @param firstName the first name of the author
     * @param lastName the last name of the author
     * @return true if an author with the specified first and last name exists, false otherwise
     */
    public boolean authorExists(String firstName, String lastName){
        return authorFacade.authorExistFirstNameLastName(firstName,lastName);
    }

    /**
     * Creates a new author if the given author does not exist in the system,
     * or associates an existing author based on user input when multiple matches are found.
     *
     * @param authorDTO the data transfer object containing the first name and last name of the author
     * @return the ID of the newly created or existing author, or null if no valid selection is made
     */
    public Long createNewAuthorOrAddExisting(AuthorDTO authorDTO){
        String firstName = authorDTO.firstName();
        String lastName = authorDTO.lastName();

        if(authorExists(firstName,lastName)) {
            log.info("Author already exists: {} {}", authorDTO.firstName(), authorDTO.lastName());
            System.out.println("Multiple Authors with this name.\n");
            Long authorId = cliPrompt.promptMultipleAuthorConfirmation(authorDTO);
            log.info("Existing author selected with ID: {}", authorId);
            //todo: 0 is a magic number here, refactor needed
            if (authorId == 0) {
                log.info("Creating new author as per user request: {} {}", authorDTO.firstName(), authorDTO.lastName());
                return (authorFacade.saveAuthor(authorDTO).id());
//                log.info("Author saved: {}", firstName + " " + lastName);
            } else {
                log.info("Fetching existing author with ID: {}", authorId);

                AuthorDTO existingAuthor = authorFacade.findById(authorId);
                return (existingAuthor.id());
//                log.info("Existing author added to list: {}", existingAuthor);
            }
        }
        return null;
    }

    /**
     * Saves a new author to the system database.
     *
     * @param authorDTO the data transfer object containing the author's details to be saved
     * @return the AuthorDTO object corresponding to the newly saved author
     */
    public AuthorDTO saveNewAuthor(AuthorDTO authorDTO){
        return authorFacade.saveAuthor(authorDTO);
    }

    /**
     * Converts a given author name string into an AuthorDTO object.
     * The full name is split into first and last name based on spaces.
     * If the name contains only one word, it is treated as the first name, and the last name is left empty.
     *
     * @param authorName the string representing the author's name, expected in "FirstName LastName" format
     * @return an AuthorDTO object with firstName and lastName extracted from the input string
     */
    public AuthorDTO mapAuthorStringToDTO(String authorName){
        String[] nameParts = authorName.trim().split(" ");
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[nameParts.length - 1] : "";
        return new AuthorDTO(null, firstName, lastName);
    }

    /**
     * Creates a list of author IDs by processing a list of author names.
     * Each name is converted into an AuthorDTO, checked for existence,
     * and either added as a new author or matched to an existing author.
     *
     * @param authorNames a list of strings representing author names, where each name
     *                    is expected to be in a "FirstName LastName" format
     * @return a list of author IDs corresponding to the processed authors
     */
    public List<Long> createAuthorsFromMetaData(List<String> authorNames){
        List<Long> authors = new ArrayList<>();

        for (String authorName : authorNames) {
            AuthorDTO authorDTO = mapAuthorStringToDTO(authorName);
            boolean exists = authorExists(authorDTO.firstName(), authorDTO.lastName());
            authors.add(exists ? createNewAuthorOrAddExisting(authorDTO)
                    : saveNewAuthor(authorDTO).id());
        }
        return authors;
    }

    public void createBookManually() {
        System.out.println("\n\u001B[95mCreate New Book");

        String title = cliPrompt.promptForBookTitle();
        List<AuthorDTO> authors = createAuthors();
        String isbn = cliPrompt.promptForBookIsbn();
        log.info("Collected Book Details - Title: {}, ISBN: {}, Authors: {}", title, isbn, authors);

        BookRequestDTO bookRequestDTO = new BookRequestDTO(title,isbn,authors);
        log.info("BookRequestDTO created: {}", bookRequestDTO);

        bookFacade.createNewBook(bookRequestDTO);
    }

    @Command(command = "shelf", description = "Place a book on a shelf or move it to a new location.")
    public void addToShelf(){
        // What if the library has multiple copies of the same book title?
        // For now, we will assume titles are unique
        // todo(priority 2): prompt user to select from multiple copies if found
        String title = cliPrompt.promptForBookTitle();
        BookDTO bookDTO = bookFacade.findBookByTitle(title);
        if(bookDTO == null){
            System.out.println("Book Not Found In Library");
        }else {
            Long bookCaseId = cliPrompt.promptForBookCase(promptOptions.bookCaseOptions());
            Long newShelfId = cliPrompt.promptForShelf(bookCaseId);

            //Checks if shelf is full/capacity reached
            Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(newShelfId);
//            Boolean isFull = shelfFacade.isFull(shelfDTO.get());
            if(shelfDTO.get().bookCapacity() <= shelfDTO.get().bookIds().size()){
                throw new IllegalStateException("Shelf is full");
            }else{

                bookFacade.updateTheBooksShelf(bookDTO, newShelfId);

                System.out.println("Added Book To the Shelf!");
            }
        }
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




    public String createBookCard(String title, String isbn, String author, String publisher, String bookcase, String shelf) {

        // %-42s ensures the text is left-aligned and padded to 42 characters
        // The emojis take up extra visual space, so adjusted padding slightly
        return """
                
                ╭──────────────────────────────────────────────────────────────────────────────╮
                │  📖 \033[38;5;63m%-73s\033[0m│
                ├──────────────────────────────────────────────────────────────────────────────┤
                │  \033[38;5;42mISBN\033[0m: %-31s                                       │
                │  \033[38;5;42mAuthor\033[0m: %-31s                                     │
                │  \033[38;5;42mPublisher\033[0m: %-31s                                  │
                │                                                                              │
                │  \033[38;5;42mBookcase\033[0m: %-35s                               │
                │  \033[38;5;42mShelf\033[0m: %-35s                                  │
                ╰──────────────────────────────────────────────────────────────────────────────╯
                
                
        """.formatted(title, isbn, author, publisher, bookcase,shelf);
    }

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

                String bookCard = createBookCard(
                        bookDTO.title(),
                        bookDTO.isbn(),
                        authors.toString(),
                        bookDTO.publisher(),
                        bookcaseLocation,
                        shelfLocation
                );
                System.out.println(bookCard);

        }
    }



    public void searchByAuthor() throws InterruptedException {
        System.out.println("\n\u001B[95mSearch by Author");
        askBookCheckOut();
    }

    public void printNotFound(String title) {
        String msg = """
                
                
                ╭──────────────────────────────────────────────╮
                │  🚫 No Results Found                         │
                ├──────────────────────────────────────────────┤
           \033[0m     │  \033[0mQuery:\033[0m  %-34s  │
                │                                              │
                │  Status: Not in library.                     │
                │  Action: Check spelling or add new book.     │
                ╰──────────────────────────────────────────────╯
                
                
        """.formatted(
                title.length() > 34 ? title.substring(0, 31) + "..." : title
        ); // Truncates title if it's too long to fit the box

        System.out.println(ConsoleColors.RED + msg + ConsoleColors.RESET);
    }


    public void searchByTitle() throws InterruptedException {
        System.out.println("\n\u001B[95mSearch by Title");
        String title = cliPrompt.promptForBookTitle();

        log.info("Searching for book with title: {}", title);
        BookDTO bookDTO = bookFacade.findBookByTitle(title);

        if (bookDTO == null) {

            printNotFound(title);
            return;
        }else if(bookDTO.shelfId() == null){
            String bookCard = createBookCard(
                    bookDTO.title(),
                    bookDTO.isbn(),
                    authorFacade.findByBookId(bookDTO.id()).toString(),
                    bookDTO.publisher(),
                    "PENDING / NOT SET",
                    "PENDING / NOT SET"

            );
            System.out.println("\n\u001B[36m</>\u001B[0m: Found it! Here are the details:\n");
            System.out.println(bookCard);
        }else{
            Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
            Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
            String bookCard = createBookCard(
                    bookDTO.title(),
                    bookDTO.isbn(),
                    authorFacade.findByBookId(bookDTO.id()).toString(),
                    "PENDING / NOT SET",
                    bookcaseDTO.get().bookcaseLabel(),
                    shelfDTO.get().shelfLabel()
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

    @Command(command = "suggest-shelf", description = "Use AI to recommend optimal shelf placement for a book.")
    public void suggestBookShelf(){
        System.out.println("Book should be placed on Shelf: G-16");
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
