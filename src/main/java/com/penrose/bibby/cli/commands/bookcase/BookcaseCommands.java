package com.penrose.bibby.cli.commands.bookcase;

import com.penrose.bibby.cli.commands.book.BookCommands;
import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;

import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDetailView;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookSummary;
import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfSummary;
import java.util.*;

@Component
@Command(command = "bookcase", group = "Bookcase Commands")
public class BookcaseCommands extends AbstractShellComponent {


    private final ComponentFlow.Builder componentFlowBuilder;
    private final ShelfFacade shelfFacade;
    private final BookFacade bookFacade;
    private final BookcaseFacade bookcaseFacade;


    public BookcaseCommands(ComponentFlow.Builder componentFlowBuilder,
                            ShelfFacade shelfFacade, BookFacade bookFacade, BookcaseFacade bookcaseFacade) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.shelfFacade = shelfFacade;
        this.bookFacade = bookFacade;
        this.bookcaseFacade = bookcaseFacade;
    }

    public String bookcaseRowFormater(BookcaseDTO bookcaseDTO, int bookCount){
        return String.format(" %-20s \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mShelves    \u001B[1m\u001B[38;5;63m%-3d\u001B[22m\u001B[38;5;15mBooks", bookcaseDTO.bookcaseLabel().toUpperCase(),bookcaseDTO.shelfCapacity(),bookCount);
    }

    @Command(command = "create", description = "Create a new bookcase in the library.")
    public void createBookcase() throws InterruptedException {


        ComponentFlow flow = componentFlowBuilder.clone()
                .withStringInput("bookcaseLabel")
                .name("What should we call this new bookcase?:_")
                .and()
                .withStringInput("shelfCount")
                .name("How many shelves does it have?:_")
                .and()
                .withStringInput("bookCapacity")
                .name("And how many books fit on a single shelf?:_")
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();

        String bookcaseLabel = result.getContext().get("bookcaseLabel");
        int shelfCount = Integer.parseInt(result.getContext().get("shelfCount"));
        int bookCapacity = Integer.parseInt(result.getContext().get("bookCapacity"));

        String confirmationMsg = """
            
            -----------------------------------
                    NEW BOOKCASE SUMMARY
            -----------------------------------
            Label:          %s
            Shelf Count:    %s
            Capacity/Shelf: %s
            
            Total Storage:  %d books
            -----------------------------------

    """.formatted(
                bookcaseLabel,
                shelfCount,
                bookCapacity,
                (shelfCount * bookCapacity)
        );

        System.out.println(confirmationMsg);


      flow = componentFlowBuilder.clone()
                .withStringInput("confirmation")
                .name("Are these details correct? (y/n):_")
                .and()
                .build();

      ComponentFlow.ComponentFlowResult res =  flow.run();
      if(res.getContext().get("confirmation").equals("Y") | res.getContext().get("confirmation").equals("y")) {
          bookcaseFacade.createNewBookCase(bookcaseLabel,shelfCount,bookCapacity);
          System.out.println("Created");
      }else{
          System.out.println("Not Created");
      }



    }

    private Map<String, String> bookCaseOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        List<BookcaseDTO> bookcaseDTOs = bookcaseFacade.getAllBookcases();
        for (BookcaseDTO bookcaseDTO : bookcaseDTOs) {
            int shelfBookCount = 0;
            List<ShelfDTO> shelves = shelfFacade.findByBookcaseId(bookcaseDTO.bookcaseId());

            for(ShelfDTO s : shelves){
                List<BookDTO> bookList = shelfFacade.findBooksByShelf(s.shelfId());
                shelfBookCount += bookList.size();
            }
            options.put(bookcaseRowFormater(bookcaseDTO,shelfBookCount), bookcaseDTO.bookcaseId().toString());
        }
        return  options;
    }



    @Command(command = "browse" , description = "Display all bookcases currently in the library, along with their labels, total shelves")
    public void listAllBookcases(){
        BookCommands bookCommands;
        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("bookcaseSelected")
                .name("Select a Bookcase")
                .selectItems(bookCaseOptions())
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();

        selectShelf(Long.parseLong(result.getContext().get("bookcaseSelected")));



        }

    public void selectShelf(Long bookCaseId){
        List<ShelfSummary> shelfSummaries = shelfFacade.getShelfSummariesForBookcase(bookCaseId);

        Map<String, String> bookShelfOptions = new LinkedHashMap<>();
        for(ShelfSummary s: shelfSummaries ){
            bookShelfOptions.put(String.format(
                        "%-10s    \u001B[38;5;197m%-2d\u001B[22m\u001B[38;5;38m Books \u001B[0m"
                    ,s.label(),s.bookCount()),s.shelfId().toString());
        }

        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("shelfSelected")
                .name("Select a Bookshelf")
                .selectItems(bookShelfOptions)
                .and()
                .build();
        ComponentFlow.ComponentFlowResult result = flow.run();

        selectBookFromShelf(Long.parseLong(result.getContext().get("shelfSelected")));

}

    public void selectBookFromShelf(Long shelfId){
        Map<String, String> bookOptions = new LinkedHashMap<>();

        for(BookSummary bookSummary: bookFacade.getBooksForShelf(shelfId) ){
            bookOptions.put(String.format(
                    "\u001B[38;5;197m%-10s  \u001B[0m"
                    ,bookSummary.title()),String.valueOf(bookSummary.bookId()));
        }

        if (bookOptions.isEmpty()) {
            getTerminal().writer().println("No books found on this shelf .");
            getTerminal().writer().flush();
            return; // end flow for now
        }

        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("shelfSelected")
                .name("Select a Book")
                .selectItems(bookOptions)
                .and()
                .build();
        ComponentFlow.ComponentFlowResult result = flow.run();

        System.out.println();

        getBookDetailsView(Long.parseLong(result.getContext().get("shelfSelected")));

    }

    public void getBookDetailsView(Long bookId){
        BookDetailView bookDetails = bookFacade.getBookDetails(bookId);
        String res = String.format(
                """
                    Title \u001B[38;5;197m%-10s  \u001B[0m
                    Authors \u001B[38;5;197m%-10s  \u001B[0m
                    Bookcase \u001B[38;5;197m%-10s  \u001B[0m
                    Bookshelf \u001B[38;5;197m%-10s  \u001B[0m
                    Book Status \u001B[38;5;197m%-10s  \u001B[0m
                """,bookDetails.title(),bookDetails.authors(),bookDetails.bookcaseLabel(),bookDetails.shelfLabel(),bookDetails.bookStatus());


        System.out.println( res );
        Map<String, String> checkOutOptions = new LinkedHashMap<>();

        checkOutOptions.put("Yes", "1");
        checkOutOptions.put("No", "2");

        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("optionSelected")
                .name("Would You Like To Check-Out?")
                .selectItems(checkOutOptions)
                .and()
                .build();
        ComponentFlow.ComponentFlowResult result = flow.run();

        if(result.getContext().get("optionSelected").equals("1") ){
            Optional<BookDTO> bookDTO = bookFacade.findBookById(bookId);
            bookFacade.checkOutBook(bookDTO.get());
        }

    }





}


