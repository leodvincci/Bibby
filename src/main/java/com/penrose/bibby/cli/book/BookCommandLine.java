package com.penrose.bibby.cli.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.author.application.AuthorService;
import com.penrose.bibby.library.book.application.BookInfoService;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.book.infrastructure.external.GoogleBooksResponse;
import com.penrose.bibby.library.book.api.BookRequestDTO;
import com.penrose.bibby.library.book.infrastructure.mapping.BookMapper;
import com.penrose.bibby.library.book.application.BookService;
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
import com.penrose.bibby.library.bookcase.application.BookcaseService;

import com.penrose.bibby.library.shelf.application.ShelfService;
import com.penrose.bibby.library.shelf.domain.Shelf;
import com.penrose.bibby.library.shelf.domain.ShelfDomainRepositoryImpl;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.shelf.infrastructure.mapping.ShelfMapper;
import com.penrose.bibby.util.LoadingBar;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellOption;
import reactor.core.publisher.Mono;

import java.util.*;


@ShellComponent
@Command(command = "book", group = "Book Commands")
public class BookCommandLine extends AbstractShellComponent {

    final BookService bookService;
    final BookcaseService bookcaseService;
    final ShelfService shelfService;
    final AuthorService authorService;
    final CliPromptService cliPrompt;
    final ShelfMapper shelfMapper;
    final BookMapper bookMapper;
    final BookInfoService bookInfoService;
    final ShelfDomainRepositoryImpl shelfDomainRepository;
    private final ComponentFlow.Builder componentFlowBuilder;



    public BookCommandLine(ComponentFlow.Builder componentFlowBuilder, BookService bookService, BookcaseService bookcaseService, ShelfService shelfService, AuthorService authorService, CliPromptService cliPrompt, ShelfMapper shelfMapper, BookMapper bookMapper, BookInfoService bookInfoService, ShelfDomainRepositoryImpl shelfDomainRepository) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.bookService = bookService;
        this.bookcaseService = bookcaseService;
        this.shelfService = shelfService;
        this.authorService = authorService;
        this.cliPrompt = cliPrompt;
        this.shelfMapper = shelfMapper;
        this.bookMapper = bookMapper;
        this.bookInfoService = bookInfoService;
        this.shelfDomainRepository = shelfDomainRepository;
    }

    // ───────────────────────────────────────────────────────────────────
    //
    //                        Book Create Commands
    //
    //
    // ───────────────────────────────────────────────────────────────────
    @Command(command = "add", description = "Add a new book to your library database")
    public void addBook() throws InterruptedException {
        String title = cliPrompt.promptForBookTitle();
        int authorCount = cliPrompt.promptForBookAuthorCount();
        List<Author> authors = new ArrayList<>();

        for (int i = 0; i < authorCount; i++) {
            authors.add(cliPrompt.promptForAuthor());
        }

        BookRequestDTO bookRequestDTO = new BookRequestDTO(title, authors);
        bookService.createNewBook(bookRequestDTO);

        System.out.println("\n\u001B[36m</>\033[0m: Ah, a brand-new book...");
        System.out.printf("\u001B[36m</>\033[0m:'%s', right?", title);
        System.out.println("\n\u001B[36m</>\033[0m: I’ll handle adding it to the database and prepare it for the library.");
//        System.out.println("\n\u001B[36m</>\033[0m: Should I recommend where it belongs?\n");
//
//        flow = componentFlowBuilder.clone()
//                .withSingleItemSelector("recommendShelf")
//                .selectItems(yesNoOptions())
//                .and().build();

//
//        if(result.getContext().get("recommendShelf",String.class).equalsIgnoreCase("yes")){
//            System.out.println(
//                    """
//                    \u001B[36m</>\033[0m: Recommended Shelf → \u001B[33mD-48\033[0m: Programming & Engineering.
//                         Fits best near “\u001B[31mThe Pragmatic Programmer\033[0m” and “\u001B[31mRefactoring\033[0m".
//                         These titles share themes of maintainable code and engineering craftsmanship.
//                         Placing them together makes your shelf flow logically by topic.
//                    """
//            );

//            System.out.println("\u001B[36m</>\033[0m:Shall I make it official and slide this one onto the shelf?\n");

//            flow = componentFlowBuilder.clone()
//                    .withSingleItemSelector("recommendShelf")
//                    .selectItems(yesNoOptions())
//                    .and().build();
//            ComponentFlow.ComponentFlowResult result = flow.run();
//            result = flow.run();
//            if(result.getContext().get("recommendShelf",String.class).equalsIgnoreCase("yes")){
//                System.out.println("\u001B[36m</>\033[0m: And there it is — " + "Shelf \u001B[33mD-48\033[0m" + ", freshly updated with another gem.\n");
//            }else{
//                System.out.println("\u001B[36m</>\033[0m: No rush. Every book finds its home eventually.\n");
//            }
//        }else{
//            System.out.println("\u001B[36m</>\033[0m: Fair enough. We can pick another shelf anytime.\n");
//        }
    }
    @Command(command = "shelf", description = "Place a book on a shelf or move it to a new location.")
    public void addToShelf(){
        String title = cliPrompt.promptForBookTitle();
        BookEntity bookEnt = bookService.findBookByTitle(title);

        if(bookEnt == null){
            System.out.println("Book Not Found In Library");
        }else {
            Long bookCaseId = cliPrompt.promptForBookCase(bookCaseOptions());
            Long shelfId = cliPrompt.promptForShelf(bookCaseId);
//            System.out.println(shelfId);
//            System.out.println(title);
//            Shelf shelf = shelfMapper.toDomain(shelfService.findShelfById(shelfId).get());
//            System.out.println(shelf);
            Shelf shelfDomain = shelfDomainRepository.getById(shelfId);
            if(shelfDomain.isFull()){
                throw new IllegalStateException("Shelf is full");
            }else{
                bookEnt.setShelfId(shelfId);
                bookService.saveBook(bookEnt);
                System.out.println("Added Book To the Shelf!");
            }
        }
    }

    @Command(command = "scan", description = "Scan a book's ISBN to add it to your library database",group = "Book Commands")
    public void scanBook(@Option(required = false, defaultValue = "single", description = "scan multiple books") @ShellOption (value = {"--type"}) String multi) {
        multi = multi == null ? "multi" : multi;

        if(multi.equalsIgnoreCase("multi")){
            multiBookScan();
        }

        if(multi.equalsIgnoreCase("single")) {
            System.out.println("Scanning single book...");
            String isbn = cliPrompt.promptForIsbnScan();
            System.out.println("Scanned ISBN: " + isbn);
            GoogleBooksResponse googleBooksResponse = bookInfoService.lookupBook(isbn).block();
            if (addScanResultCommand(googleBooksResponse, isbn)) {
                bookService.createScannedBook(googleBooksResponse, isbn);
                System.out.println("\n\u001B[36m</>\033[0m: Book added to the library database successfully!");
            }
        }


    }

    private void multiBookScan() {
        Long bookcaseId = cliPrompt.promptForBookCase(bookCaseOptions());
        Long shelfId = cliPrompt.promptForShelf(bookcaseId);
        ShelfEntity shelfEntity = shelfService.findShelfById(shelfId).get();
        System.out.println("Scanning multiple books...");
        List<String> scans = cliPrompt.promptMultiScan();

        for (String isbn : scans) {
            System.out.println("Scanned ISBN: " + isbn);
            GoogleBooksResponse googleBooksResponse = bookInfoService.lookupBook(isbn).block();

                BookEntity bookEntity = bookService.createScannedBook(googleBooksResponse, isbn);
                bookEntity.setShelfId(shelfId);
                bookService.saveBook(bookEntity);
                System.out.println("\n\u001B[36m</>\033[0m:" + bookEntity.getTitle() +  " added to Library!");

        }
        System.out.println(scans.size() + " books were added to the library.");
    }


//    @Command(command = "shelf", description = "Place a book on a shelf or move it to a new location.")
//    public void scanToShelf(GoogleBooksResponse bookMetaData){
//        String title = bookMetaData.items().get(0).volumeInfo().title();
//        BookEntity bookEnt = bookService.findBookByTitle(title);
//
//        if(bookEnt == null){
//            System.out.println("Book Not Found In Library");
//        }else {
//            Long bookCaseId = cliPrompt.promptForBookCase(bookCaseOptions());
//            Long shelfId = cliPrompt.promptForShelf(bookCaseId);
////            System.out.println(shelfId);
////            System.out.println(title);
////            Shelf shelf = shelfMapper.toDomain(shelfService.findShelfById(shelfId).get());
////            System.out.println(shelf);
//            Shelf shelfDomain = shelfDomainRepository.getById(shelfId);
//            if(shelfDomain.isFull()){
//                throw new IllegalStateException("Shelf is full");
//            }else{
//                bookEnt.setShelfId(shelfId);
//                bookService.saveBook(bookEnt);
//                System.out.println("Added Book To the Shelf!");
//            }
//        }
//    }

    // ───────────────────────────────────────────────────────────────────
    //
    //                       Book Search Commands
    //
    //
    // ───────────────────────────────────────────────────────────────────

    @Command(command = "search", description = "Search for books by title, author, genre, or location using an interactive prompt.")
    public void searchBook() throws InterruptedException {
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
        BookEntity bookEntity = bookService.findBookByIsbn(isbn);
        if (bookEntity == null) {
            System.out.println("\n\u001B[36m</>\u001B[0m: No book found with ISBN: " + isbn + "\n");
        } else {
            System.out.println("\n\u001B[36m</>\u001B[0m: Book found: \n");
            System.out.println("Book Details:");
            System.out.println("Title: " + bookEntity.getTitle());
            Set<AuthorEntity> authors = authorService.findByBookId(bookEntity.getBookId());
            System.out.println("Author(s): " + authors);
            System.out.println("ISBN: " + bookEntity.getIsbn());
            if (bookEntity.getShelfId() != null) {
                Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
                Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
                System.out.println("Location: Bookcase " + bookcaseEntity.get().getBookcaseLabel() + ", Shelf " + shelfEntity.get().getShelfLabel());
            } else {
                System.out.println("Location: Not Shelved");
            }
        }
    }

    public void searchByAuthor() throws InterruptedException {
        System.out.println("\n\u001B[95mSearch by Author");
        Author author = cliPrompt.promptForAuthor();
        System.out.println("\n\u001B[36m</>\u001B[0m: Ah, the works of " + author.getFirstName() + " " + author.getLastName() + " — a fine choice. Let me check the shelves...");
        LoadingBar.showLoading();
        System.out.println("\n\u001B[36m</>\u001B[0m: Found 2 titles — both are sitting on their shelves, available.");
        System.out.println("""
                ──────────────────────────────────────────────
                [12] \u001B[33mMy Life Decoded: The Story of Leo\u001B[0m   \n[Shelf A1] (AVAILABLE)\s
                
                [29] \u001B[33mThe Answer is 42 \u001B[0m   \n[Shelf B2] (AVAILABLE)
                """);
        System.out.println("\u001B[90m───────────────────────────────────────────────\u001B[0m");
        askBookCheckOut();
    }

    public void searchByTitle() throws InterruptedException {
        System.out.println("\n\u001B[95mSearch by Title");
        String title = cliPrompt.promptForBookTitle();
        System.out.println("\u001B[36m</>\u001B[0m:Hold on, I’m diving into the stacks — Let’s see if I can find " + title);
        System.out.print("\u001B[36m</>\u001B[0m:");

        BookEntity bookEntity = bookService.findBookByTitle(title);

        LoadingBar.showLoading();

        if (bookEntity == null) {
            System.out.println("\n\u001B[36m</>\u001B[0m:I just flipped through every shelf — no luck this time.\n");
        }else if(bookEntity.getShelfId() == null){
            System.out.println("\nBook Was Found Without a Location\n");
        }else{
            Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
            Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
            System.out.println("\nBook Was Found \nBookcase: " + bookcaseEntity.get().getBookcaseLabel() + "\n" + shelfEntity.get().getShelfLabel() + "\n");
        }
        if (cliPrompt.promptSearchAgain()){
            searchBook();
        }
    }

    @Command(command = "list", description = "View all books with optional filters and sorting.")
    public void listBooks(){
        System.out.println("Listing all books /w filter");
    }

    // ───────────────────────────────────────────────────────────────────
    //
    //                       Book Update Commands
    //
    //
    // ───────────────────────────────────────────────────────────────────
    @Command(command = "check-out", description = "Check-Out a book from the library")
    public void checkOutBook(){
        String bookTitle = cliPrompt.promptForBookTitle();
        BookEntity book = bookService.findBookByTitle(bookTitle);
        String bookcaseName = "N.A";
        String shelfName ="N.A";
        if(book == null){
            System.out.println("Book Not Found.");
        }else if(book.getShelfId() != null){
            Optional<ShelfEntity> shelf = shelfService.findShelfById(book.getShelfId());
            Optional<BookcaseEntity> bookcase = bookcaseService.findBookCaseById(shelf.get().getBookcaseId());
            bookcaseName = bookcase.get().getBookcaseLabel();
            shelfName = shelf.get().getShelfLabel();
        }if (book.getAvailabilityStatus().equals("CHECKED_OUT")){
            System.out.println(
                    """
                    
                    \u001B[38;5;63m  .---.
                    \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "This one’s already off the shelf. No double-dipping on checkouts."
                    \u001B[38;5;63m  \\|=|/
                    
                    """);
        }else{
            Set<AuthorEntity> authors = authorService.findByBookId(book.getBookId());
            System.out.println(String.format("""
                    \n\u001B[32mConfirm Checkout\n\u001B[0m
                            \033[31mTitle\u001B[0m %s
                            \033[31mAuthor/s\u001B[0m %s
                            
                            \033[31mStatus %s
                            
                            \033[31mBookcase\u001B[0m %s
                            \033[31mShelf\u001B[0m %s
                    """,book.getTitle(), authors, book.getAvailabilityStatus(), bookcaseName ,shelfName));
            ComponentFlow confirmationFlow = componentFlowBuilder.clone()
                    .withStringInput("isConfirmed")
                    .name("y or n:_ ")
                    .and().build();
            ComponentFlow.ComponentFlowResult confirmationResult = confirmationFlow.run();

            if (confirmationResult.getContext().get("isConfirmed").equals("y")){
                bookService.checkOutBook(book);
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
        BookEntity bookEntity = bookService.findBookByTitle(bookTitle);

        String bookcaseLabel = "No Assigned Bookcase";
        String bookshelfLabel = "No Assigned Bookshelf";
        if(bookEntity == null){
            System.out.println("Book Not Found");
        }else if(bookEntity.getShelfId() != null){
            Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
            Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
            bookcaseLabel = bookcaseEntity.get().getBookcaseLabel();
            bookshelfLabel = shelfEntity.get().getShelfLabel();
        }
        Set<AuthorEntity> authors = authorService.findByBookId(bookEntity.getBookId());

        System.out.println(String.format("""
                    \n\u001B[32mConfirm Checkin\n\u001B[0m
                            \033[31mTitle\u001B[0m %s
                            \033[31mAuthor/s\u001B[0m %s
                            
                            \033[31mStatus %s
                            
                            \033[31mBookcase\u001B[0m %s
                            \033[31mShelf\u001B[0m %s
                    """,bookEntity.getTitle(), authors, bookEntity.getAvailabilityStatus(), bookcaseLabel ,bookshelfLabel));

        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("isConfirmed" )
                .name("y or n:_ ")
                .and().build();
        ComponentFlow.ComponentFlowResult result;
        result = flow.run();

        if(result.getContext().get("isConfirmed").equals("y")){
            bookService.checkInBook(bookTitle);
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
        Thread.sleep(2300);
        System.out.println("\u001B[36m</>\u001B[0m:Persuading the shelf to let go...\n");
        Thread.sleep(2300);
        Thread.sleep(1000);
        System.out.println("\u001B[36m</>\u001B[0m:Dusting off the cover...\n");
        Thread.sleep(2300);
        System.out.println("\u001B[36m</>\u001B[0m:Logging transaction...\n");
        Thread.sleep(1000);
        System.out.println("\u001B[36m</>\u001B[0m:Checking it out...please hold while I fake progress bars.\n");
        Thread.sleep(1000);
        LoadingBar.showLoading();
        Thread.sleep(2000);
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
        List<BookcaseEntity> bookcaseEntities  = bookcaseService.getAllBookcases();
        for(BookcaseEntity b : bookcaseEntities){
            options.put(b.getBookcaseLabel(), b.getBookcaseId().toString());
        }

        return options;
    }

    public boolean addScanResultCommand(GoogleBooksResponse bookMetaData,String isbn) {
        String title = (bookMetaData.items().get(0).volumeInfo().title());
        String authors =bookMetaData.items().get(0).volumeInfo().authors().toString();
        String publishingDate = (bookMetaData.items().get(0).volumeInfo().publishedDate());
        String categories = String.valueOf(bookMetaData.items().get(0).volumeInfo().categories());
        String description = (bookMetaData.items().get(0).volumeInfo().description());
//        System.out.println("\n\u001B[36m</>\u001B[0m: Book scanned successfully. Here's the metadata:");
        System.out.println("\n\u001B[36m</>\u001B[0m:");

        System.out.printf(""
                + "========================================\n"
                + "📚  Book Metadata\n"
                + "========================================\n"
                + "\n"
                + "ISBN:              %s\n"
                + "Title:             %s\n"
                + "Authors:           %s\n"
                + "Published:         %s\n"
                + "Categories:        %s\n"
                + "\n"
                + "Description:\n"
                + "%s\n"
                + "\n"
                + "========================================\n",isbn,title,authors,publishingDate,categories,description);
        System.out.println();
        return cliPrompt.promptBookConfirmation();
    }
}
