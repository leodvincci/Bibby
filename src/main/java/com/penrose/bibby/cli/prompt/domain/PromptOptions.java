package com.penrose.bibby.cli.prompt.domain;

import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
import org.springframework.shell.component.flow.SelectItem;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PromptOptions {
    ShelfFacade shelfFacade;
    AuthorFacade authorFacade;
    BookFacade bookFacade;
    BookcaseFacade bookcaseFacade;

    public PromptOptions(ShelfFacade shelfFacade, AuthorFacade authorFacade, BookFacade bookFacade, BookcaseFacade bookcaseFacade) {
        this.shelfFacade = shelfFacade;
        this.authorFacade = authorFacade;
        this.bookFacade = bookFacade;
        this.bookcaseFacade = bookcaseFacade;
    }

    public Map<String, String> yesNoOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Yes", "Yes");
        options.put("No", "No");
        return options;
    }

    public Map<String, String> bookShelfOptions(Long bookcaseId) {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("\u001B[38;5;202m[Cancel]\033[36m","cancel");
        List<ShelfDTO> shelfDTOS = shelfFacade.getAllDTOShelves(bookcaseId);
        for(ShelfDTO s : shelfDTOS){
            options.put(s.shelfLabel(), String.valueOf(s.shelfId()));
        }

        return options;
    }

    public Map<String, String> searchOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("""
                        ISBN                 (e.g., 9780345391803)""", "isbn");
        options.put("""
                        Title                (e.g., "The Hitchhiker's Guide to the Galaxy")""", "title");
        options.put("""
                        Author               (e.g., Douglas Adams)""", "author");
        return options;
    }

    public List<SelectItem> authorOptions(AuthorDTO author) {
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

    public Map<String, String> bookCaseOptionsByLocation(String location) {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("\u001B[38;5;202m[Cancel]\033[36m","cancel");
        List<BookcaseDTO> bookcaseDTOs  = bookcaseFacade.getAllBookcasesByLocation(location);
        for(BookcaseDTO b : bookcaseDTOs){
            options.put(b.bookcaseLabel(), b.bookcaseId().toString());
        }
        return options;
    }

    public Map<String, String> BookCaseOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("\u001B[38;5;202m[Cancel]\033[36m","cancel");
        List<BookcaseDTO> bookcaseDTOs  = bookcaseFacade.getAllBookcases();
        for(BookcaseDTO b : bookcaseDTOs){
            options.put(b.bookcaseLabel(), b.bookcaseId().toString());
        }
        return options;
    }


    public Map<String,String> bookcaseLocationOptions() {
        Map<String,String> options = new LinkedHashMap<>();
        options.put("\u001B[38;5;202m[CANCEL]\033[36m","cancel");
        options.put("\u001B[38;5;42m[CREATE NEW LOCATION]\u001B[0m", "new");
        List<String> bookcaseLocations  = bookcaseFacade.getAllBookcaseLocations();
        for(String bookcase : bookcaseLocations){
            options.put("\u001B[38;5;63m"+ bookcase +"\u001B[0m", bookcase);
        }
        return options;
    }

    public Map<String,String> bookcaseLocationOptionsBrowse() {
        Map<String,String> options = new LinkedHashMap<>();
        options.put("\u001B[38;5;202m[CANCEL]\033[36m","cancel");
        List<String> bookcaseLocations  = bookcaseFacade.getAllBookcaseLocations();
        for(String bookcase : bookcaseLocations){
            options.put("\u001B[38;5;63m"+ bookcase +"\u001B[0m", bookcase);
        }
        return options;
    }

    public Map<String, String> bookCaseOptions() {
        // LinkedHashMap keeps insertion order so the menu shows in the order you add them
        Map<String, String> options = new LinkedHashMap<>();
        options.put("\u001B[38;5;202m [CANCEL]\033[36m","cancel");
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

    public String bookcaseRowFormater(BookcaseDTO bookcaseDTO, int bookCount){
        return String.format(" %-20s \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mShelves    \u001B[1m\u001B[38;5;63m%-3d\u001B[22m\u001B[38;5;15mBooks",bookcaseDTO.bookcaseLabel().toUpperCase(),bookcaseDTO.shelfCapacity(),bookCount);
    }

    public List<SelectItem> metaDataSelection() {
        List<SelectItem> options = new ArrayList<>();
        options.add(SelectItem.of("[CANCEL]", "cancel"));
        options.add(SelectItem.of("Title", "title"));
        options.add(SelectItem.of("Authors", "authors"));
        options.add(SelectItem.of("Publisher", "publisher"));
        return options;
    }
}
