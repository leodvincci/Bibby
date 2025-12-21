package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.cli.ui.BookcardRenderer;
import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
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
public class BookCreateImportCommands {
    Logger log = org.slf4j.LoggerFactory.getLogger(BookCreateImportCommands.class);
    private final CliPromptService cliPrompt;
    private final BookcardRenderer bookcardRenderer;
    private final BookFacade bookFacade;
    private final BookcaseFacade bookcaseFacade;
    private final ShelfFacade shelfFacade;
    private final AuthorFacade authorFacade;
    private final PromptOptions promptOptions;

    public BookCreateImportCommands(CliPromptService cliPrompt, BookcardRenderer bookcardRenderer, BookFacade bookFacade, BookcaseFacade bookcaseFacade, ShelfFacade shelfFacade, AuthorFacade authorFacade, PromptOptions promptOptions){

        this.cliPrompt = cliPrompt;
        this.bookcardRenderer = bookcardRenderer;
        this.bookFacade = bookFacade;
        this.bookcaseFacade = bookcaseFacade;
        this.shelfFacade = shelfFacade;
        this.authorFacade = authorFacade;
        this.promptOptions = promptOptions;
    }




    /*
    ============================
        CLI Command Endpoints (Spring Shell entry points)
    ============================
     */

    @Command(command = "import", description = "import ISBN barcode to retrieve metadata and add it to the library.", group = "Library Commands")
    public void createBookImport(@ShellOption(defaultValue = "single") boolean multi) {

//        if (multi) multiBookScan();

        BookMetaDataResponse bookMetaDataResponse = scanBook();
        String isbn = bookMetaDataResponse.isbn();

        if (cliPrompt.promptToConfirmBookAddition()) {
            String location = cliPrompt.promptForBookcaseLocation();

            Long bookcaseId = cliPrompt.promptForBookcaseSelection(promptOptions.bookCaseOptionsByLocation(location));
            if(bookcaseId == null) return;

            Long shelfId = cliPrompt.promptForShelfSelection(bookcaseId);
            if(shelfId == null) return;

            List<Long> authorIds = createAuthorsFromMetaData(bookMetaDataResponse.authors());

            bookFacade.createBookFromMetaData(bookMetaDataResponse, authorIds, isbn, shelfId);

            String updatedBookCard = bookcardRenderer.createBookCard(bookMetaDataResponse.title(),
                    bookMetaDataResponse.isbn(),
                    bookMetaDataResponse.authors().toString(),
                    bookMetaDataResponse.publisher(),
                    bookcaseFacade.findBookCaseById(bookcaseId).get().bookcaseLabel(),
                    shelfFacade.findShelfById(shelfId).get().shelfLabel(),
                    bookcaseFacade.findBookCaseById(bookcaseId).get().location()
            );
            System.out.println(updatedBookCard);
        }
    }





    /*  ============================
        Scan flow helpers
        ============================
     */

    /**
     * Scans a book by prompting the user to enter its ISBN and retrieves metadata associated
     * with the ISBN from the system. The retrieved metadata includes details like title,
     * authors, publisher, and description of the book. The method also validates the entered
     * ISBN and displays a formatted "book card" with the book's metadata to the user.
     *
     * @return a {@code BookMetaDataResponse} object containing the metadata for the scanned
     *         book, or {@code null} if the ISBN is invalid or the process is aborted.
     */
    public BookMetaDataResponse scanBook(){
        log.info("Initiating scanBook for Single Scan.");
        System.out.println("\n\u001B[95mSingle Book Scan");
        String isbn = cliPrompt.promptForIsbn();
        BookMetaDataResponse bookMetaDataResponse = bookFacade.findBookMetaDataByIsbn(isbn);
        log.debug("BookMetaDataResponse received: {}", bookMetaDataResponse);
        log.debug("Authors verified/created for book.");
        log.info(bookMetaDataResponse.toString());
        System.out.println(
                bookcardRenderer.createBookCard(bookMetaDataResponse.title(),
                        bookMetaDataResponse.isbn(),
                        bookMetaDataResponse.authors().toString(),
                        bookMetaDataResponse.publisher(),
                        "PENDING / NOT SET",
                        "PENDING / NOT SET",
                        "PENDING / NOT SET")
        );
        return bookMetaDataResponse;
    }


    public BookMetaDataResponse importBook(String isbn){
        log.info("Initiating scanBook for Import.");
//        System.out.println("\n\u001B[95mSingle Book Scan");
        BookMetaDataResponse bookMetaDataResponse = bookFacade.findBookMetaDataByIsbn(isbn);
        log.debug("BookMetaDataResponse received: {}", bookMetaDataResponse);
        log.debug("Authors verified/created for book.");
        log.info(bookMetaDataResponse.toString());
        System.out.println(
                bookcardRenderer.bookImportCard(bookMetaDataResponse.title(),
                        bookMetaDataResponse.isbn(),
                        bookMetaDataResponse.authors().toString(),
                        bookMetaDataResponse.publisher())
        );
        return bookMetaDataResponse;
    }

    /* ============================
        Author (metadata)
       ============================
    */

    /**
     * Creates a list of author IDs by processing a list of author names.
     * Each name is converted into an AuthorDTO, checked for existence,
     * and either added as a new author or matched to an existing author.
     *
     * @param authorNames a list of strings representing author names, where each name
     *                    is expected to be in a "FirstName LastName" format
     * @return a list of author IDs corresponding to the processed authors
     */
    public List<Long> createAuthorsFromMetaData(List<String> authorNames){
        List<Long> authors = new ArrayList<>();

        for (String authorName : authorNames) {
            AuthorDTO authorDTO = mapAuthorStringToDTO(authorName);
            boolean exists = authorExists(authorDTO.firstName(), authorDTO.lastName());
            authors.add(exists ? createNewAuthorOrAddExisting(authorDTO)
                    : saveNewAuthor(authorDTO).id());
        }
        return authors;
    }




    /* ============================
        Mapping + facade wrappers
       ============================
    */

    /**
     * Saves a new author to the system database.
     *
     * @param authorDTO the data transfer object containing the author's details to be saved
     * @return the AuthorDTO object corresponding to the newly saved author
     */
    public AuthorDTO saveNewAuthor(AuthorDTO authorDTO){
        return authorFacade.saveAuthor(authorDTO);
    }

    /**
     * Converts a given author name string into an AuthorDTO object.
     * The full name is split into first and last name based on spaces.
     * If the name contains only one word, it is treated as the first name, and the last name is left empty.
     *
     * @param authorName the string representing the author's name, expected in "FirstName LastName" format
     * @return an AuthorDTO object with firstName and lastName extracted from the input string
     */
    public AuthorDTO mapAuthorStringToDTO(String authorName){
        String[] nameParts = authorName.trim().split(" ");
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[nameParts.length - 1] : "";
        return new AuthorDTO(null, firstName, lastName);
    }

    /**
     * Creates a new author or retrieves an existing one based on the provided author details.
     * If an author with the same first and last name exists, the method either prompts the user
     * to select an existing author or creates a new one depending on the user's decision.
     *
     * @param authorDTO the data transfer object containing the author's first name and last name
     * @return the ID of the existing or newly created author, or null if the operation is aborted
     */
    public Long createNewAuthorOrAddExisting(AuthorDTO authorDTO){
        String firstName = authorDTO.firstName();
        String lastName = authorDTO.lastName();

        if(authorExists(firstName,lastName)) {
            log.info("Author already exists: {} {}", authorDTO.firstName(), authorDTO.lastName());
            System.out.println("Multiple Authors with this name.\n");
            Long authorId = cliPrompt.promptMultipleAuthorConfirmation(authorDTO);
            log.info("Existing author selected with ID: {}", authorId);
            //todo: 0 is a magic number here, refactor needed
            if (authorId == 0) {
                log.info("Creating new author as per user request: {} {}", authorDTO.firstName(), authorDTO.lastName());
                return (authorFacade.saveAuthor(authorDTO).id());
//                log.info("Author saved: {}", firstName + " " + lastName);
            } else {
                log.info("Fetching existing author with ID: {}", authorId);

                AuthorDTO existingAuthor = authorFacade.findById(authorId);
                return (existingAuthor.id());
//                log.info("Existing author added to list: {}", existingAuthor);
            }
        }
        return null;
    }

    /**
     * Checks if an author exists in the system with the given first and last name.
     *
     * @param firstName the first name of the author
     * @param lastName the last name of the author
     * @return true if an author with the specified first and last name exists, false otherwise
     */
    public boolean authorExists(String firstName, String lastName){
        return authorFacade.authorExistFirstNameLastName(firstName,lastName);
    }
}
