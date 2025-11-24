package com.penrose.bibby.cli;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.bookcase.BookcaseEntity;
import com.penrose.bibby.library.bookcase.BookcaseService;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfService;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CliPromptService {
    private final ComponentFlow.Builder componentFlowBuilder;
    private final BookcaseService bookcaseService;
    private final ShelfService shelfService;

    public CliPromptService(ComponentFlow.Builder componentFlowBuilder, ComponentFlow.Builder componentFlowBuilder1, BookcaseService bookcaseService, ShelfService shelfService) {

        this.componentFlowBuilder = componentFlowBuilder1;
        this.bookcaseService = bookcaseService;
        this.shelfService = shelfService;
    }

    public boolean promptSearchAgain(){
        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("searchDecision")
                .name("Would you like to search again?")
                .selectItems(yesNoOptions())
                .and().build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext().get("searchDecision",String.class).equalsIgnoreCase("Yes");
    }

    public Author promptForAuthor(){
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("authorFirstName")
                .name("Author's First Name:_")
                .and()
                .withStringInput("authorLastName")
                .name("Author's Last Name:_")
                .and().build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        String firstName  = result.getContext().get("authorFirstName", String.class);
        String lastName = result.getContext().get("authorLastName", String.class);
//        BookRequestDTO bookRequestDTO = new BookRequestDTO(title,firstName, lastName);
//        bookService.createNewBook(bookRequestDTO);
        return new Author(firstName,lastName);

    }

    public Long promptForShelf(Long bookCaseId){
        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("bookshelf")
                .name("Chose a shelf position")
                .selectItems(bookShelfOptions(bookCaseId))
                .and().build();
        ComponentFlow.ComponentFlowResult result = flow.run();
        return Long.parseLong(result.getContext().get("bookshelf",String.class));
    }

    public Long promptForBookCase(){
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withSingleItemSelector("bookcase")
                .name("Choose a Bookcase:_")
                .selectItems(bookCaseOptions())
                .and().build();
        ComponentFlow.ComponentFlowResult result = flow.run();
        return Long.parseLong(result.getContext().get("bookcase",String.class));
    }

    public String promptForBookTitle(){
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("title")
                .name("Book Title:_")
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext().get("title",String.class);
    }

    public int promptForBookAuthorCount(){
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("author_count")
                .name("Number of Authors:_")
                .and()
                .build();
        ComponentFlow.ComponentFlowResult result = flow.run();
        String authorCount = result.getContext().get("author_count",String.class);
        return Integer.parseInt(authorCount);
    }

    public String promptForSearchType(){
        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("searchType")
                .name("How would you like to search?")
                .selectItems(buildSearchOptions())
                .max(10)
                .and()
                .build();
        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext().get("searchType",String.class);
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





}
