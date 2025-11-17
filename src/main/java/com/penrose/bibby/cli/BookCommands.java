package com.penrose.bibby.cli;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.author.AuthorService;
import com.penrose.bibby.library.book.BookController;
import com.penrose.bibby.library.book.BookEntity;
import com.penrose.bibby.library.book.BookRequestDTO;
import com.penrose.bibby.library.book.BookService;
import com.penrose.bibby.library.bookcase.BookcaseEntity;
import com.penrose.bibby.library.bookcase.BookcaseService;
import com.penrose.bibby.library.shelf.Shelf;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfService;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
@Command(command = "book", group = "Book Commands")
public class BookCommands extends AbstractShellComponent {

    final BookService bookService;
    final BookController bookController;
    final BookcaseService bookcaseService;
    final ShelfService shelfService;
    final AuthorService authorService;



    // ───────────────────────────────────────────────────────────────────
    //
    //                        Book Create Commands
    //
    //
    // ───────────────────────────────────────────────────────────────────




    List<String> bibbySearchResponses = new ArrayList<>(List.of(
            "Got it — searching the stacks for books by",
            "Sure thing — I’ll take a quick look through the shelves for",
            "Alright - Give me a sec...checking the catalogue for books by",
            "Hold on, I’m diving into the stacks — Let’s see what we’ve got by",
            "Searching for books by",
            "Let’s take a quiet look through the shelves for",
            "On it — I’ll go dig through the stacks. Hope the intern filed things alphabetically this time."
    ));

    public void setBibbySearchResponses(String bibbySearchResponses) {
        this.bibbySearchResponses.add(bibbySearchResponses);
    }

    private final ComponentFlow.Builder componentFlowBuilder;

    public BookCommands(ComponentFlow.Builder componentFlowBuilder, BookService bookService, BookController bookController, BookcaseService bookcaseService, ShelfService shelfService, AuthorService authorService) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.bookService = bookService;
        this.bookController = bookController;
        this.bookcaseService = bookcaseService;
        this.shelfService = shelfService;
        this.authorService = authorService;
    }

    public void authorNameComponentFlow(String title){
        ComponentFlow flow2;
        flow2 = componentFlowBuilder.clone()
                .withStringInput("authorFirstName")
                .name("Author's First Name:_")
                .and()
                .withStringInput("authorLastName")
                .name("Author's Last Name:_")
                .and().build();

        ComponentFlow.ComponentFlowResult result = flow2.run();
        String firstName  = result.getContext().get("authorFirstName", String.class);
        String lastName = result.getContext().get("authorLastName", String.class);
        BookRequestDTO bookRequestDTO = new BookRequestDTO(title,firstName, lastName);
        bookService.createNewBook(bookRequestDTO);
    }

    @Command(command = "add", description = "Add a new book to your library database")
    public void addBook() throws InterruptedException {
        ComponentFlow flow;
        ComponentFlow flow2;
        int authorCount;
        String title;
        String author;



        flow = componentFlowBuilder.clone()
                .withStringInput("title")
                .name("Book Title:_")
                .and()
                .withStringInput("author_count")
                .name("Number of Authors:_")
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        authorCount = Integer.parseInt(result.getContext().get("author_count",String.class));



         title  = result.getContext().get("title", String.class);

        for(int i = 0; i < authorCount; i++){
            authorNameComponentFlow(title);
        }

        Thread.sleep(1000);

        System.out.println("\n\u001B[36m</>\033[0m: Ah, a brand-new book...");
        Thread.sleep(1750);
        System.out.printf("\u001B[36m</>\033[0m:'%s', right?",title);
        Thread.sleep(2350);
        System.out.println("\n\u001B[36m</>\033[0m: I’ll handle adding it to the database and prepare it for the library.");
        Thread.sleep(3800);
        System.out.println("\n\u001B[36m</>\033[0m: Should I recommend where it belongs?\n");
        Thread.sleep(1000);

        flow = componentFlowBuilder.clone()
                .withSingleItemSelector("recommendShelf")
                .selectItems(yesNoOptions())
                .and().build();
        result = flow.run();

        if(result.getContext().get("recommendShelf",String.class).equalsIgnoreCase("yes")){
            Thread.sleep(2000);
            System.out.println(
                    """
                    \u001B[36m</>\033[0m: Recommended Shelf → \u001B[33mD-48\033[0m: Programming & Engineering.
                         Fits best near “\u001B[31mThe Pragmatic Programmer\033[0m” and “\u001B[31mRefactoring\033[0m".
                         These titles share themes of maintainable code and engineering craftsmanship.
                         Placing them together makes your shelf flow logically by topic.
                    """
            );

            Thread.sleep(2000);
            System.out.println("\u001B[36m</>\033[0m:Shall I make it official and slide this one onto the shelf?\n");

            flow = componentFlowBuilder.clone()
                    .withSingleItemSelector("recommendShelf")
                    .selectItems(yesNoOptions())
                    .and().build();
            result = flow.run();

            if(result.getContext().get("recommendShelf",String.class).equalsIgnoreCase("yes")){
                Thread.sleep(2000);
                System.out.println("\u001B[36m</>\033[0m: And there it is — " + "Shelf \u001B[33mD-48\033[0m" + ", freshly updated with another gem.\n");
            }else{
                Thread.sleep(2000);
                System.out.println("\u001B[36m</>\033[0m: No rush. Every book finds its home eventually.\n");
            }
        }else{
            Thread.sleep(2000);
            System.out.println("\u001B[36m</>\033[0m: Fair enough. We can pick another shelf anytime.\n");
        }



    }


    public void showLoading() throws InterruptedException {
        LoadingBar.showProgressBar("Loading books from shelf...", 40, 150);
    }




    /*

        Book Search Commands

    */




    @Command(command = "search", description = "Search for books by title, author, genre, or location using an interactive prompt.")
    public void searchBook() throws InterruptedException {


        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("searchType")
                .name("How would you like to search?")
                .selectItems(buildSearchOptions())
                .max(10)
                .and()
                .build();


        ComponentFlow.ComponentFlowResult result = flow.run();

        String searchType = result.getContext().get("searchType", String.class);
        if (searchType.equalsIgnoreCase("author")){
            searchByAuthor();
        }else if(searchType.equalsIgnoreCase("title")){
            searchByTitle();
        }

    }


    @Command(command = "shelf", description = "Place a book on a shelf or move it to a new location.")
    public void addToShelf(){
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("bookTitle")
                .name("What book are you shelving?:_")
                .and()
                .withSingleItemSelector("bookcase")
                .name("Choose a Bookcase:_")
                .selectItems(bookCaseOptions())
                .and().build();
        ComponentFlow.ComponentFlowResult result = flow.run();
        String title = result.getContext().get("bookTitle",String.class);
        Long bookCaseId = Long.parseLong(result.getContext().get("bookcase",String.class));
        System.out.println("BOOK CASE ID: " + bookCaseId);


        flow = componentFlowBuilder.clone()
                .withSingleItemSelector("bookshelf")
                .name("Chose a shelf position")
                .selectItems(bookShelfOptions(bookCaseId))
                .and().build();


        result = flow.run();




        BookEntity bookEnt = bookService.findBookByTitle(title);
        if(bookEnt == null){
            System.out.println("Book Not Found In Library");
        }else {
            Long shelfId = Long.parseLong(result.getContext().get("bookshelf",String.class));
            System.out.println(shelfId);
            System.out.println(title);
            bookEnt.setShelfId(shelfId);
            bookService.updateBook(bookEnt);
            System.out.println("Added Book To the Shelf!");
        }

    }

    public void searchByAuthorVoice(){
        List<String> searchResponses = new ArrayList<>();

    }

    public void searchByAuthor() throws InterruptedException {

        System.out.println("\n\u001B[95mSearch by Author");

        ComponentFlow componentFlow;
        String authorFirstName;
        String authorLastName;
        componentFlow = componentFlowBuilder.clone()
                .withStringInput("authorFirstName")
                .name("Enter author First Name:_ ")
                .and()
                .withStringInput("authorLastName")
                .name("Enter author Last Name:_")
                .and().build();

        ComponentFlow.ComponentFlowResult result = componentFlow.run();
        authorFirstName = result.getContext().get("authorFirstName",String.class);
        authorLastName = result.getContext().get("authorLastName",String.class);



        Thread.sleep(1000);
        System.out.println("\n\u001B[36m</>\u001B[0m: Ah, the works of " + authorFirstName + " " + authorLastName + " — a fine choice. Let me check the shelves...");
        Thread.sleep(4000);
        showLoading();



        System.out.println("\n\u001B[36m</>\u001B[0m: Found 2 titles — both are sitting on their shelves, available.");
        Thread.sleep(2000);

        System.out.println("""
                ──────────────────────────────────────────────
                [12] \u001B[33mMy Life Decoded: The Story of Leo\u001B[0m   \n[Shelf A1] (AVAILABLE)\s
                
                [29] \u001B[33mThe Answer is 42 \u001B[0m   \n[Shelf B2] (AVAILABLE)
                """);
        System.out.println("\u001B[90m───────────────────────────────────────────────\u001B[0m");

        Thread.sleep(500);

        askBookCheckOut();
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
        showLoading();
        Thread.sleep(2000);
        System.out.println("\n\u001B[36m</>\u001B[0m:Don’t forget to check it back in… or at least feel guilty about it.\n");


    }

    public void searchByTitle() throws InterruptedException {
        System.out.println("\n");
        String title;
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("bookTitle")
                .name("Enter book title:_")
                .and().build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        title = result.getContext().get("bookTitle",String.class);
        System.out.println("\u001B[36m</>\u001B[0m:Hold on, I’m diving into the stacks — Let’s see if I can find " + title);
        System.out.print("\u001B[36m</>\u001B[0m:");


        Thread.sleep(1000);

        BookEntity bookEntity = bookService.findBookByTitle(title);

        showLoading();

        Thread.sleep(500);

        if (bookEntity == null) {
            System.out.println("\n\u001B[36m</>\u001B[0m:I just flipped through every shelf — no luck this time.\n");
        }else if(bookEntity.getShelfId() == null){
            System.out.println("\nBook Was Found Without a Location\n");
        }else{
            Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
            Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
            System.out.println("\nBook Was Found \nBookcase: " + bookcaseEntity.get().getBookcaseLabel() + "\nShelf: " + shelfEntity.get().getShelfLabel() + "\n");
        }

        Thread.sleep(2000);

        flow = componentFlowBuilder.clone()
                .withSingleItemSelector("searchDecision")
                .name("Would you like to search again?")
                .selectItems(yesNoOptions())
                .and().build();

        result = flow.run();
        if (result.getContext().get("searchDecision",String.class).equalsIgnoreCase("Yes")){
            searchBook();
        }
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
            Thread.sleep(1000);
            System.out.println("\n\u001B[36m</>\u001B[0m:Cool, I’ll just… put all these back by myself...and whisper *maybe next time* to the shelves... Again.\n");

        }


    }



    private Map<String, String> buildSearchOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Show all books  — \u001B[32mView the complete library\n\u001B[0m", "all");
        options.put("Title or keyword  —  \u001B[32mSearch by words in the title\n\u001B[0m", "title");
        options.put("Author  —  \u001B[32mFind books by author name\n\u001B[0m", "author");
        options.put("Genre  —  \u001B[32mFilter books by literary category\n\u001B[0m", "genre");
        options.put("Shelf/Location  —  \u001B[32mLocate books by physical shelf ID\n\u001B[0m", "shelf");
        options.put("Status  — \u001B[32mShow available or checked-out books\n\u001B[0m", "status");
        return options;
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


    private Map<String, String> bookShelfOptions(Long bookcaseId) {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        List<ShelfEntity> shelves = shelfService.getAllShelves(bookcaseId);
        for(ShelfEntity s : shelves){
            System.out.println(s.getBookcaseId());
            options.put(s.getShelfLabel(), String.valueOf(s.getShelfId()));
        }

        return options;
    }

    private Map<String, String> yesNoOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Yes  — \u001B[32mLet's Do It\n\u001B[0m", "Yes");
        options.put("No  —  \u001B[32mNot this time\n\u001B[0m", "No");
        return options;
    }




    @Command(command = "list", description = "View all books with optional filters and sorting.")
    public void listBooks(){
        System.out.println("Listing all books /w filter");
    }

    @Command(command = "check-out", description = "Check-Out a book from the library")
    public void checkOutBook(){
        ComponentFlow bookTitleFlow;
        bookTitleFlow = componentFlowBuilder.clone()
                .withStringInput("bookTitle" )
                .name("Book Title:")
                .and().build();
        ComponentFlow.ComponentFlowResult bookTitleResult = bookTitleFlow.run();

        String bookTitle = bookTitleResult.getContext().get("bookTitle");

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
        }if (book.getBookStatus().equals("CHECKED_OUT")){
            System.out.println(
                    """
                    
                    \u001B[38;5;63m  .---.
                    \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "This one’s already off the shelf. No double-dipping on checkouts."
                    \u001B[38;5;63m  \\|=|/
                    
                    """);
            

        }else{
            List<AuthorEntity> authors = bookService.findAuthorsByBookId(book.getBookId());
            System.out.println(String.format("""
                    \n\u001B[32mConfirm Checkout\n\u001B[0m
                            \033[31mTitle\u001B[0m %s
                            \033[31mAuthor/s\u001B[0m %s
                            
                            \033[31mStatus %s
                            
                            \033[31mBookcase\u001B[0m %s
                            \033[31mShelf\u001B[0m %s
                    """,book.getTitle(), authors, book.getBookStatus(), bookcaseName ,shelfName));
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
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("bookTitle" )
                .name("Book Title:")
                .and().build();
        ComponentFlow.ComponentFlowResult result = flow.run();

        String bookTitle = result.getContext().get("bookTitle");

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

        List<AuthorEntity> authors = bookService.findAuthorsByBookId(bookEntity.getBookId());

        System.out.println(String.format("""
                    \n\u001B[32mConfirm Checkin\n\u001B[0m
                            \033[31mTitle\u001B[0m %s
                            \033[31mAuthor/s\u001B[0m %s
                            
                            \033[31mStatus %s
                            
                            \033[31mBookcase\u001B[0m %s
                            \033[31mShelf\u001B[0m %s
                    """,bookEntity.getTitle(), authors, bookEntity.getBookStatus(), bookcaseLabel ,bookshelfLabel));


        flow = componentFlowBuilder.clone()
                .withStringInput("isConfirmed" )
                .name("y or n:_ ")
                .and().build();
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

    @Command(command = "suggest-shelf", description = "Use AI to recommend optimal shelf placement for a book.")
    public void suggestBookShelf(){
        System.out.println("Book should be placed on Shelf: G-16");
    }
}
