package com.penrose.bibby.cli;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.book.BookController;
import com.penrose.bibby.library.book.BookRequestDTO;
import com.penrose.bibby.library.book.BookService;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
@Command(command = "book", group = "Book Commands")
public class BookCommands extends AbstractShellComponent {

    final BookService bookService;
    final BookController bookController;
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

    public BookCommands(ComponentFlow.Builder componentFlowBuilder, BookService bookService, BookController bookController) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.bookService = bookService;
        this.bookController = bookController;
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

        ComponentFlow.ComponentFlowResult res = flow2.run();
        String firstName  = res.getContext().get("authorFirstName", String.class);
        String lastName = res.getContext().get("authorLastName", String.class);
        BookRequestDTO bookRequestDTO = new BookRequestDTO(title,firstName, lastName);
        bookService.createNewBook(bookRequestDTO);
//        new AuthorEntity(firstName, lastName);
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
//        String[] authorFullName = author.split(" ");

//        BookRequestDTO bookRequestDTO = new BookRequestDTO(title,authorFullName[0], authorFullName[1]);

//        bookService.addBook(title,authorFullName[0],authorFullName[1]);
//        bookService.createNewBook(bookRequestDTO);

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

        ComponentFlow.ComponentFlowResult res = componentFlow.run();
        authorFirstName =res.getContext().get("authorFirstName",String.class);
        authorLastName =res.getContext().get("authorLastName",String.class);



        Thread.sleep(1000);
        System.out.println("\n\u001B[36m</>\u001B[0m: Ah, the works of " + authorFirstName + " " + authorLastName + " — a fine choice. Let me check the shelves...");
        Thread.sleep(4000);
        showLoading();

        //call controller to search by first and last name.



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

        ComponentFlow.ComponentFlowResult res = flow.run();
        title = res.getContext().get("bookTitle",String.class);
        System.out.println("\u001B[36m</>\u001B[0m:Hold on, I’m diving into the stacks — Let’s see if I can find " + title);
        System.out.print("\u001B[36m</>\u001B[0m:");
        Thread.sleep(1000);

        Boolean isFound = bookService.findBookByTitle(title);
        showLoading();

        Thread.sleep(500);

        if (!isFound) {
            System.out.println("\n\u001B[36m</>\u001B[0m:I just flipped through every shelf — no luck this time.\n");
        }else{
            System.out.println("\nBook Was Found in Bookcase: 000 on Shelf: 111\n");
        }

        Thread.sleep(2000);

        flow = componentFlowBuilder.clone()
                .withSingleItemSelector("searchDecision")
                .name("Would you like to search again?")
                .selectItems(yesNoOptions())
                .and().build();

        res = flow.run();
        if (res.getContext().get("searchDecision",String.class).equalsIgnoreCase("Yes")){
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

        ComponentFlow.ComponentFlowResult res = flow.run();
        String theRes = res.getContext().get("checkOutDecision",String.class);
        if(theRes.equalsIgnoreCase("yes")){
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

    @Command(command = "check-out", description = "Borrow a book by selecting it interactively and marking it as checked out.")
    public void checkOutBook(){
        System.out.println("Checking Out Your Book");
    }

    @Command(command = "check-in",description = "Return a borrowed book to the library and update its shelf placement.")
    public void checkInBook(){
        System.out.println("Book Checked Back onto Shelf");
    }

    @Command(command = "shelf", description = "Place a book on a shelf or move it to a new location.")
    public void assignBookShelf(){
        System.out.println("Book Placed On Shelf:A-42");
    }

    @Command(command = "suggest-shelf", description = "Use AI to recommend optimal shelf placement for a book.")
    public void suggestBookShelf(){
        System.out.println("Book should be placed on Shelf: G-16");
//        System.out.println("Book should be placed on Shelf: G-16");
    }
}
