package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;

import java.util.Optional;

@ShellComponent
@Command(command = "book", group = "Book Placement Commands")
public class BookPlacementCommands {
    private final BookFacade bookFacade;
    private final ShelfFacade shelfFacade;
    private final CliPromptService cliPrompt;
    private final PromptOptions promptOptions;




    public BookPlacementCommands(BookFacade bookFacade, ShelfFacade shelfFacade, CliPromptService cliPrompt, PromptOptions promptOptions) {
        this.bookFacade = bookFacade;
        this.shelfFacade = shelfFacade;
        this.cliPrompt = cliPrompt;
        this.promptOptions = promptOptions;
    }




    @Command(command = "shelve",
            description = """
                    \u001B[38;5;3mAdd books to a specific shelf by ISBN (scan/type/paste). If the book already exists, Bibby will place it on the shelf.
                    If it doesn’t exist, Bibby will fetch metadata, create the book, and then place it on the shelf (create-or-place).
                    \u001B[0m
                    """
            , group = "Book Placement Commands")
    public void addToShelf(){
        // What if the library has multiple copies of the same book title?
        // For now, we will assume titles are unique
        // todo(priority 2): prompt user to select from multiple copies if found
        String title = cliPrompt.promptForBookTitle();
        BookDTO bookDTO = bookFacade.findBookByTitle(title);
        if(bookDTO == null){
            System.out.println("Book Not Found In Library");
        }else {
            Long bookCaseId = cliPrompt.promptForBookcaseSelection(promptOptions.bookCaseOptions());
            Long newShelfId = cliPrompt.promptForShelfSelection(bookCaseId);

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

    @Command(command = "place"
            , description = """
              \u001B[38;5;185mAssign an existing book in your library to a shelf (or move it to a new shelf). 
              You can identify books by ISBN (single, batch entry, or file input) or select from your list of unplaced books.
              Updates shelf location only—does not create new book records.
              \u001B[0m"""
            , group = "Book Placement Commands")
    public void bookPlacement(){
        System.out.println("Book placement command executed.");
    }



}
