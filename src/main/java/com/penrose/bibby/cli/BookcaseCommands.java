package com.penrose.bibby.cli;

import com.penrose.bibby.library.book.BookEntity;
import com.penrose.bibby.library.book.BookService;
import com.penrose.bibby.library.bookcase.Bookcase;
import com.penrose.bibby.library.bookcase.BookcaseEntity;
import com.penrose.bibby.library.bookcase.BookcaseService;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfService;
import com.penrose.bibby.library.shelf.ShelfSummary;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Command(command = "bookcase", group = "Bookcase Commands")
public class BookcaseCommands extends AbstractShellComponent {


    private final ComponentFlow.Builder componentFlowBuilder;
    private final BookcaseService bookcaseService;
    private final ShelfService shelfService;
    private final BookService bookService;




    public BookcaseCommands(ComponentFlow.Builder componentFlowBuilder, BookcaseService bookcaseService, ShelfService shelfService, BookService bookService) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.bookcaseService = bookcaseService;
        this.shelfService = shelfService;
        this.bookService = bookService;
    }

    public String bookcaseRowFormater(BookcaseEntity bookcaseEntity, int bookCount){
        return String.format(" %-12s \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mShelves    \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mBooks ", bookcaseEntity.getBookcaseLabel().toUpperCase(),bookcaseEntity.getShelfCapacity(),bookCount);
    }

    @Command(command = "add", description = "Create a new bookcase in the library.")
    public void addBookcase() throws InterruptedException {
        ComponentFlow flow = componentFlowBuilder.clone()
                .withStringInput("bookcaseLabel")
                .name("Give this bookcase a label:_")
                .and()
                .withStringInput("shelfCapacity")
                .name("How many shelves?:_")
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        bookcaseService.createNewBookCase(result.getContext().get("bookcaseLabel"),Integer.parseInt(result.getContext().get("shelfCapacity")));

    }

    private Map<String, String> bookCaseOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();
        for (BookcaseEntity b : bookcaseEntities) {
            int shelfBookCount = 0;
            List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());
            for(ShelfEntity s : shelves){
                List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());
                shelfBookCount += bookList.size();
            }
            options.put(bookcaseRowFormater(b,shelfBookCount), b.getBookcaseId().toString());
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

//        System.out.println("LOOOK: ");
//        System.out.println(result.getContext().get("bookcaseSelected").toString());
//        System.out.println(shelfService.getShelfSummariesForBookcase(Long.parseLong(result.getContext().get("bookcaseSelected"))));
//        System.out.println(":::::::::::::::::::::::::");

        selectShelf(Long.parseLong(result.getContext().get("bookcaseSelected")));





        }

//        for(BookcaseEntity b : bookcaseService.getAllBookcases()){
//            int shelfBookCount = 0;
//            List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());
//            for(ShelfEntity s : shelves){
//                List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());
//                shelfBookCount += bookList.size();
//            }
//            System.out.println(bookcaseRowFormater(b,shelfBookCount));
//            System.out.println(b.getBookcaseLabel() + ":" + b.getShelfCapacity());


    public void selectShelf(Long bookCaseId){
//        System.out.println("YUP!");
        List<ShelfSummary> shelfSummaries = shelfService.getShelfSummariesForBookcase(bookCaseId);

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



}




}


