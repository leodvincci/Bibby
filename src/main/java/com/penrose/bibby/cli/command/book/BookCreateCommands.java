package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.cli.ui.BookcardRenderer;
import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookRequestDTO;
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
public class BookCreateCommands {
    Logger log = org.slf4j.LoggerFactory.getLogger(BookCreateCommands.class);
    private final CliPromptService cliPrompt;
    private final BookcardRenderer bookcardRenderer;
    private final BookFacade bookFacade;
    private final BookcaseFacade bookcaseFacade;
    private final ShelfFacade shelfFacade;
    private final AuthorFacade authorFacade;
    private final PromptOptions promptOptions;

    public BookCreateCommands(CliPromptService cliPrompt, BookcardRenderer bookcardRenderer, BookFacade bookFacade, BookcaseFacade bookcaseFacade, ShelfFacade shelfFacade, AuthorFacade authorFacade, PromptOptions promptOptions){

        this.cliPrompt = cliPrompt;
        this.bookcardRenderer = bookcardRenderer;
        this.bookFacade = bookFacade;
        this.bookcaseFacade = bookcaseFacade;
        this.shelfFacade = shelfFacade;
        this.authorFacade = authorFacade;
        this.promptOptions = promptOptions;
    }


    /**
     * Scans a book's ISBN and facilitates its addition to the library database. This method
     * allows users to interactively scan a book, validate its ISBN, retrieve metadata,
     * and confirm or modify its placement in the library shelving system.
     *
     * @param multi if true, enables multiple book scanning functionality (currently unused);
     *              if false, initiates a single book scanning process.
     */
    @Command(command = "scan", description = "Scan a book's ISBN barcode to retrieve metadata and add it to the library. Use --multi for batch scanning", group = "Book Create Commands")
    public void scanBook(@ShellOption(defaultValue = "single") boolean multi) {

//        if (multi) multiBookScan();
        log.info("Initiating scanBook for Single Scan.");
        System.out.println("\n\u001B[95mSingle Book Scan");
        String isbn = cliPrompt.promptForIsbnScan();
        if (!cliPrompt.isbnValidator(isbn)) return;
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

        if (cliPrompt.promptBookConfirmation()) {   
            String location = cliPrompt.promptForBookcaseLocation();
            System.out.println("Selected Location: " + location);
            Long bookcaseId = cliPrompt.promptForBookCase(promptOptions.bookCaseOptionsByLocation(location));
            if(bookcaseId == null){
                return;
            }
            Long shelfId = cliPrompt.promptForShelf(bookcaseId);
            if(shelfId == null){
                return;
            }
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

    @Command(command = "new", description = "Register a new book in the library system by manual entry" , group = "Book Create Commands")
    public void registerBook(
            @ShellOption(defaultValue = "false") boolean scan,
            @ShellOption(defaultValue = "false") boolean multi) {

        log.info("Starting new book registration process.");
        log.debug("{} {}", scan, multi);

        ScanMode mode = ScanMode.from(scan, multi);
        switch(mode){
            case SINGLE -> scanBook(false);
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
        System.out.println("\n\u001B[95mCreate New Book");

        String title = cliPrompt.promptForBookTitle();
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
            AuthorDTO authorDTO = cliPrompt.promptForAuthor();
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
     * Creates a new author if the given author does not exist in the system,
     * or associates an existing author based on user input when multiple matches are found.
     *
     * @param authorDTO the data transfer object containing the first name and last name of the author
     * @return the ID of the newly created or existing author, or null if no valid selection is made
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


    //
//    private void multiBookScan() {
//        log.info("Initiating multiBookScan for Multi Scan.");
//        Long bookcaseId = cliPrompt.promptForBookCase(bookCaseOptions());
//        Long shelfId = cliPrompt.promptForShelf(bookcaseId);
//        System.out.println("\n\u001B[95mMulti-Book Scan");
//        List<String> scans = cliPrompt.promptMultiScan();
//
//        for (String isbn : scans) {
//            System.out.println("Scanned ISBN: " + isbn);
//
//            BookMetaDataResponse bookMetaDataResponse = bookFacade.findBookMetaDataByIsbn(isbn);
//            bookFacade.createBookFromMetaData(bookMetaDataResponse, isbn, shelfId);
//            System.out.println("\n\u001B[36m</>\033[0m:" + bookMetaDataResponse.title() + " added to Library!");
//            System.out.println(scans.size() + " books were added to the library.");
//        }
//    }



}
