package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookRequestDTO;
import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
import org.slf4j.Logger;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellOption;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the command group dedicated to creating and managing books
 * within the library system. Provides methods for registering new books,
 * managing authors, and handling interactive prompts for metadata and
 * author information.
 *
 * Fields:
 * - log: Logger instance for capturing debug and operation details.
 * - cliPrompt: Command Line Interface prompt service for user interaction.
 * - bookcardRenderer: Service for rendering book information cards.
 * - bookFacade: Facade for managing book-related operations.
 * - bookcaseFacade: Facade for handling bookcase-related operations.
 * - shelfFacade: Facade for managing shelf functionalities.
 * - authorFacade: Facade for handling author-related operations.
 * - promptOptions: Options to customize command-line prompts during interactions.
 */
@ShellComponent
@Command(command = "book", group = "Book Create Commands")
public class BookCreateCommands {
    Logger log = org.slf4j.LoggerFactory.getLogger(BookCreateCommands.class);
    private final CliPromptService cliPrompt;
    private final BookFacade bookFacade;
    private final AuthorFacade authorFacade;

    public BookCreateCommands(CliPromptService cliPrompt, BookFacade bookFacade, AuthorFacade authorFacade){

        this.cliPrompt = cliPrompt;
        this.bookFacade = bookFacade;
        this.authorFacade = authorFacade;
    }

    /**
     * Registers a new book entry in the library system. Depending on the provided options,
     * this method can either scan a book's ISBN for metadata retrieval or allow manual entry
     * of book details.
     *
     * @param scan  if true, initiates a scanning process to retrieve book metadata;
     *              if false, allows manual entry of book details.
     * @param multi if true, enables multiple book scanning functionality (currently unused);
     *              if false, processes a single book entry.
     */
//    @Command(command = "register"
//            , description = """
//                            \u001B[38;5;185mAdd books to your library by ISBN (scan/type/paste) or manual entry. If an ISBN is provided, Bibby fetches metadata and creates a new book record.
//                            Supports single entry, batch entry, or file import.
//                            \u001B[0m
//                            """
//            , group = "Book Create Commands")
    public void registerBook(
            @ShellOption(defaultValue = "false") boolean scan,
            @ShellOption(defaultValue = "false") boolean multi) {

        log.info("Starting new book registration process.");
        log.debug("{} {}", scan, multi);

        ScanMode mode = ScanMode.from(scan, multi);
        switch(mode){
//            case SINGLE -> createBookScan(false);
//            case MULTI -> multiBookScan();
            case NONE -> createBookManually();
        }
    }

    /**
     * Manually creates a new book entry by prompting the user for input.
     * This method facilitates the interactive addition of a book, including its title, authors,
     * and ISBN, by collecting details directly from the user and validating the information.
     * The collected data is then used to create a BookRequestDTO, which is passed to the
     * book facade for further processing and storage.
     *
     * The following steps are performed:
     * 1. Prompts the user to input the book title.
     * 2. Invokes the createAuthors method to gather and process author details.
     * 3. Prompts the user to input the book's ISBN.
     * 4. Logs the collected details (title, authors, and ISBN).
     * 5. Constructs a BookRequestDTO object encapsulating these details.
     * 6. Uses the book facade to create a new book entry in the system.
     */
    public void createBookManually() {
        System.out.println("\n\u001B[95mCreate New Book\u001B[0m (':q' to quit)");

        String title = cliPrompt.promptForBookTitle();
        if(title.equals(":q")){
            log.info("Book creation process aborted by user.");
            System.out.println("Book creation aborted.\n");
            return;
        }
        List<AuthorDTO> authors = createAuthors();
        String isbn = cliPrompt.promptForBookIsbn();
        log.info("Collected Book Details - Title: {}, ISBN: {}, Authors: {}", title, isbn, authors);

        BookRequestDTO bookRequestDTO = new BookRequestDTO(title,isbn,authors);
        log.info("BookRequestDTO created: {}", bookRequestDTO);

        bookFacade.createNewBook(bookRequestDTO);
    }


    public List<AuthorDTO> createAuthors(){
        int numberOfAuthors = cliPrompt.promptForBookAuthorCount();
        List<AuthorDTO> authors = new ArrayList<>();
        for (int i = 0; i < numberOfAuthors; i++) {
            AuthorDTO authorDTO = cliPrompt.promptForAuthorDetails();
            log.info("Collected Author Details: {}", authorDTO);

            if(authorFacade.authorExistFirstNameLastName(authorDTO.firstName(),authorDTO.lastName())){
                log.info("Author already exists: {} {}", authorDTO.firstName(), authorDTO.lastName());
                System.out.println("Multiple Authors with this name.\n");
                Long authorId = cliPrompt.promptMultipleAuthorConfirmation(authorDTO);
                log.info("Existing author selected with ID: {}", authorId);
                //todo: 0 is a magic number here, refactor needed
                if(authorId == 0){
                    log.info("Creating new author as per user request: {} {}", authorDTO.firstName(), authorDTO.lastName());
                    authors.add(authorFacade.saveAuthor(authorDTO));
                    log.info("Author saved: {}", authors.get(i));
                }else{
                    log.info("Fetching existing author with ID: {}", authorId);

                    AuthorDTO existingAuthor = authorFacade.findById(authorId);
                    authors.add(existingAuthor);
                    log.info("Existing author added to list: {}", existingAuthor);
                }
            }else{
                log.info("Creating new author: {} {}", authorDTO.firstName(), authorDTO.lastName());
                authors.add(authorFacade.saveAuthor(authorDTO));
                log.info("Author saved: {}", authors.get(i));
            }

        }
        log.info("Authors Created and Returned: {}", authors);
        return authors;
    }
}
