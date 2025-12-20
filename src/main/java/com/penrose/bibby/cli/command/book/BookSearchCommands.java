package com.penrose.bibby.cli.command.book;


import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.ui.BookcardRenderer;
import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDTO;
import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.stacks.bookcase.contracts.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
import org.slf4j.Logger;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;

import java.util.Optional;
import java.util.Set;

@ShellComponent
@Command(command = "book", group = "Book Search Commands")
public class BookSearchCommands {
    private final CliPromptService cliPrompt;
    private final BookcardRenderer bookcardRenderer;
    private final BookFacade bookFacade;
    private final BookcaseFacade bookcaseFacade;
    private final ShelfFacade shelfFacade;
    private final AuthorFacade authorFacade;
    Logger log = org.slf4j.LoggerFactory.getLogger(BookCreateCommands.class);
    public BookSearchCommands(CliPromptService cliPrompt, BookcardRenderer bookcardRenderer, BookFacade bookFacade, BookcaseFacade bookcaseFacade, ShelfFacade shelfFacade, AuthorFacade authorFacade){

        this.cliPrompt = cliPrompt;
        this.bookcardRenderer = bookcardRenderer;
        this.bookFacade = bookFacade;
        this.bookcaseFacade = bookcaseFacade;
        this.shelfFacade = shelfFacade;
        this.authorFacade = authorFacade;
    }



    @Command(command = "search", description = "Find a book by title, author, genre, or location using an interactive prompt.")
    public void searchForBook() throws InterruptedException {
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
        String shelfLocation = "";
        String bookcaseLocation = "";
        System.out.println("\n\u001B[95mSearch by ISBN");
        String isbn = cliPrompt.promptForIsbn();
        if(isbn == null){
            System.out.println("NULL ISBN RETURNED");
            return;
        }

        BookDTO bookDTO = bookFacade.findBookByIsbn(isbn);

        if (bookDTO == null) {
            System.out.println("\n\u001B[36m</>\u001B[0m: No book found with ISBN: " + isbn + "\n");
        } else {

            System.out.println("\n\u001B[36m</>\u001B[0m: Book found: \n");
            if(bookDTO.shelfId() == null){
                shelfLocation = "PENDING / NOT SET";
                bookcaseLocation = "PENDING / NOT SET";
            }else{
                Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
                Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
                bookcaseLocation = bookcaseDTO.get().bookcaseLabel();
                shelfLocation = shelfDTO.get().shelfLabel();
            }
            Set<AuthorDTO> authors = authorFacade.findByBookId(bookDTO.id());

            String bookCard = bookcardRenderer.createBookCard(
                    bookDTO.title(),
                    bookDTO.isbn(),
                    authors.toString(),
                    bookDTO.publisher(),
                    bookcaseLocation,
                    shelfLocation,
                    "PENDING / NOT SET"
            );
            System.out.println(bookCard);

        }
    }



    public void searchByAuthor() throws InterruptedException {
        System.out.println("\n\u001B[95mSearch by Author");
    }




    public void searchByTitle() throws InterruptedException {
        System.out.println("\n\u001B[95mSearch by Title");
        String title = cliPrompt.promptForBookTitle();

        log.info("Searching for book with title: {}", title);
        BookDTO bookDTO = bookFacade.findBookByTitle(title);

        if (bookDTO == null) {

            bookcardRenderer.printNotFound(title);
            return;
        }else if(bookDTO.shelfId() == null){
            String bookCard = bookcardRenderer.createBookCard(
                    bookDTO.title(),
                    bookDTO.isbn(),
                    authorFacade.findByBookId(bookDTO.id()).toString(),
                    bookDTO.publisher(),
                    "PENDING / NOT SET",
                    "PENDING / NOT SET",
                    "PENDING / NOT SET"

            );
            System.out.println("\n\u001B[36m</>\u001B[0m: Found it! Here are the details:\n");
            System.out.println(bookCard);
        }else{
            Optional<ShelfDTO> shelfDTO = shelfFacade.findShelfById(bookDTO.shelfId());
            Optional<BookcaseDTO> bookcaseDTO = bookcaseFacade.findBookCaseById(shelfDTO.get().bookcaseId());
            System.out.println(authorFacade.findByBookId(bookDTO.id()).toString());
            String bookCard = bookcardRenderer.createBookCard(
                    bookDTO.title(),
                    bookDTO.isbn(),
                    authorFacade.findByBookId(bookDTO.id()).toString(),
                    bookDTO.publisher(),
                    bookcaseDTO.get().bookcaseLabel(),
                    shelfDTO.get().shelfLabel(),
                    bookcaseDTO.get().location()
            );
            System.out.println("\n\u001B[36m</>\u001B[0m: Found it! Here are the details:\n");
            System.out.println(bookCard);
        }
        if (cliPrompt.promptSearchAgain()){
            searchForBook();
        }
    }
}
