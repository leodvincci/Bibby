package com.penrose.bibby.cli.prompt.application;

import com.penrose.bibby.cli.prompt.contracts.PromptFacade;
import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
import com.penrose.bibby.library.catalog.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.catalog.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.placement.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.placement.shelf.contracts.ports.inbound.ShelfFacade;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.SelectItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CliPromptService implements PromptFacade {
    private final ComponentFlow.Builder componentFlowBuilder;
    private final AuthorFacade authorFacade;
    private final BookFacade bookFacade;
    List<String> scans = new ArrayList<>();
    private final ShelfFacade shelfFacade;

    public CliPromptService(ComponentFlow.Builder componentFlowBuilder, ComponentFlow.Builder componentFlowBuilder1, ShelfFacade shelfFacade, AuthorFacade authorFacade, BookFacade bookFacade) {
        this.componentFlowBuilder = componentFlowBuilder1;
        this.shelfFacade = shelfFacade;
        this.authorFacade = authorFacade;
        this.bookFacade = bookFacade;
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

    public boolean promptBookConfirmation(){
        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("confirmation")
                .name("Would you like to add this book to the library?")
                .selectItems(yesNoOptions())
                .and().build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext().get("confirmation",String.class).equalsIgnoreCase("Yes");
    }

    public List<String> promptMultiScan(){
        ComponentFlow flow = componentFlowBuilder.clone()
                .withStringInput("multiScan")
                .name("Multi-Scan >:_")
                .and().build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        String scan = result.getContext().get("multiScan",String.class);

        if(!scan.equalsIgnoreCase("done")){
            scans.add(scan);
            promptMultiScan();
        }
        return scans;
    }

    public AuthorDTO promptForAuthor(){
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
        return new AuthorDTO(null,firstName,lastName);

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


    public Long  promptMultipleAuthorConfirmation(AuthorDTO author){
        ComponentFlow flow = componentFlowBuilder.clone()
                .withSingleItemSelector("chooseAuthor")
                .name("Author Selection")
                .selectItems(buildAuthorOptions(author))
                .and().build();
        ComponentFlow.ComponentFlowResult result = flow.run();
        return Long.parseLong(result.getContext().get("chooseAuthor",String.class));
    }

    private List<SelectItem> buildAuthorOptions(AuthorDTO author) {
        List<SelectItem> options = new ArrayList<>();

        options.add(SelectItem.of("\u001B[38;5;42mCreate New Author\u001B[0m", "0"));
        //get each author name and id from the list and add to options
        //return all author by first and last name
        List<AuthorDTO> authors = authorFacade.getAllAuthorsByName(author.firstName(), author.lastName());
        for(AuthorDTO a : authors){
            options.add(SelectItem.of("\u001B[38;5;63m"+a.firstName() + " " + a.lastName() + "\u001B[0m" + " ID: " + a.id() +  "\u001B[38;5;146m" + bookFacade.getBooksByAuthorId(a.id())+"\u001B[0m", String.valueOf(a.id())));
        }
        return options;
    }

    public String promptForIsbnScan(){
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("isbn")
                .name("ISBN Number:_")
                .next( ctx -> {
                    String value = ctx.getResultValue();
                    if(value.equalsIgnoreCase(":q")){
                        return null;
                    }else if(isbnValidator(value)){
                        return null;
                    }
                    ctx.setResultValue(null);
                    ctx.setInput("");
                    ctx.setDefaultValue("");
                    return "isbn";
                 })
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext().get("isbn",String.class);
    }

    public boolean isbnValidator(String isbn){
        if(isbn.startsWith("978") && isbn.length() == 13){
            return true;
        }else if(isbn.equalsIgnoreCase(":q")){
            System.out.println("\u001B[31mISBN entry cancelled by user.\u001B[0m");
            return false;
        }else {
            System.out.println("\u001B[31mInvalid ISBN. Please enter a valid 13-digit ISBN starting with '978'.\u001B[0m");
            return false;
        }
    }

    public Long promptForBookCase(Map<String, String> bookCaseOptions){
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withSingleItemSelector("bookcase")
                .name("Choose a Bookcase:_")
                .selectItems(bookCaseOptions)
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


    public String promptForBookIsbn(){
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("isbn")
                .name("ISBN:_")
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext().get("isbn",String.class);
    }

    /**
     * Prompt the user for the number of authors for a book.
     *
     * @return The number of authors as an integer.
     */
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
                .name("Select a search method:")
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
//        options.put("""
//                        Show all books       (View the complete library)""", "all");
        options.put("""
                        ISBN                 (e.g., 9780345391803)""", "isbn");
        options.put("""
                        Title                (e.g., "The Hitchhiker's Guide to the Galaxy")""", "title");
//        options.put("""
//                        keyword     (Search by words in the title)""", "title");
        options.put("""
                        Author               (e.g., Douglas Adams)""", "author");
//        options.put("""
//                        Genre                (Filter books by literary category)""", "genre");
//        options.put("""
//                        Shelf/Location       (Locate books by physical shelf ID)""", "shelf");
//        options.put("""
//                        Status               (Show available or checked-out books)""", "status");
        return options;
    }



    private Map<String, String> bookShelfOptions(Long bookcaseId) {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        List<ShelfDTO> shelfDTOS = shelfFacade.getAllDTOShelves(bookcaseId);
        for(ShelfDTO s : shelfDTOS){
            options.put(s.shelfLabel(), String.valueOf(s.shelfId()));
        }

        return options;
    }

    private Map<String, String> yesNoOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Yes  — \u001B[32mLet's Do It\u001B[0m", "Yes");
        options.put("No  —  \u001B[32mNot this time\u001B[0m", "No");
        return options;
    }





}
