package com.penrose.bibby;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;

@Component
@Command(command = "book", group = "Book Commands")
public class BookCommands extends AbstractShellComponent {


    private final ComponentFlow.Builder componentFlowBuilder;

    public BookCommands(ComponentFlow.Builder componentFlowBuilder) {
        this.componentFlowBuilder = componentFlowBuilder;
    }

    @Command(command = "add", description = "Add a new book to your library database")
    public void addBook(){
        ComponentFlow flow;
        flow =componentFlowBuilder.clone()
                .withStringInput("title")
                    .name("Book Title:_")
                    .and()
                .withStringInput("author")
                    .name("Author Name:_")
                    .and()
                .withStringInput("genre")
                    .name("Genre (optional):_")
                    .and()
                .withStringInput("year")
                    .name("Year (optional):_")
                    .and()
                .build();
        ComponentFlow.ComponentFlowResult result = flow.run();

        String title  = result.getContext().get("title", String.class);
        String author = result.getContext().get("author", String.class);
        String genre  = result.getContext().get("genre", String.class);
        String year   = result.getContext().get("year", String.class);

        System.out.printf(
                """
                        
                        \u001B[36m</>\033[0m: I added the book\u001B[36m %s\033[0m by\u001B[36m %s\033[0m to your library
                        
                        """,
                title, author
        );

    }


    @Command(command = "search", description = "Search for books by title, author, genre, or location using an interactive prompt.")
    public void searchBook(){
        System.out.println("Searching...");
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

    @Command(command = "assign-shelf", description = "Assign or update a book’s physical shelf location.")
    public void assignBookShelf(){
        System.out.println("Book Placed On Shelf:A-42");
    }

    @Command(command = "suggest-shelf", description = "Use AI to recommend optimal shelf placement for a book.")
    public void suggestBookShelf(){
        System.out.println("Book should be placed on Shelf: G-16");
    }
}
