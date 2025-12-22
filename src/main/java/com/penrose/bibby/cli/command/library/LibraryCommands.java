package com.penrose.bibby.cli.command.library;

import com.penrose.bibby.cli.command.book.BookCreateIsbnCommands;
import com.penrose.bibby.cli.prompt.application.CliPromptService;
import com.penrose.bibby.cli.prompt.domain.PromptOptions;
import com.penrose.bibby.cli.ui.BookcardRenderer;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookMetaDataResponse;
import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;
import com.penrose.bibby.library.stacks.bookcase.contracts.ports.inbound.BookcaseFacade;
import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
import org.slf4j.Logger;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellOption;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ShellComponent
    @Command(command = "library", group = "Library Commands")
public class LibraryCommands {
    Logger log = org.slf4j.LoggerFactory.getLogger(LibraryCommands.class);
    BookCreateIsbnCommands bookCreateIsbnCommands;
    private final CliPromptService cliPrompt;
    private final PromptOptions promptOptions;
    private final BookFacade bookFacade;
    private final BookcaseFacade bookcaseFacade;
    private final ShelfFacade shelfFacade;
    private final BookcardRenderer bookcardRenderer;

    public LibraryCommands(BookCreateIsbnCommands bookCreateIsbnCommands, CliPromptService cliPrompt, PromptOptions promptOptions, BookFacade bookFacade, BookcaseFacade bookcaseFacade, ShelfFacade shelfFacade, BookcardRenderer bookcardRenderer) {
        this.bookCreateIsbnCommands = bookCreateIsbnCommands;
        this.cliPrompt = cliPrompt;
        this.promptOptions = promptOptions;
        this.bookFacade = bookFacade;
        this.bookcaseFacade = bookcaseFacade;
        this.shelfFacade = shelfFacade;
        this.bookcardRenderer = bookcardRenderer;
    }




    //todo: implement csv import
    @Command(command = "import"
            ,description =
                            """
            \u001B[38;5;3mBulk import books from a file of ISBNs. (Creates books; no placement.)
             \u001B[0m""")
    public void createBooksFromCsv(
            @ShellOption String filePath) throws IOException {

        log.info("Importing books from CSV...");


        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                BookMetaDataResponse bookMetaDataResponse = bookCreateIsbnCommands.importBook(line);
            }
        }catch (IOException exception) {
            log.error("Error reading CSV file: {}", exception.getMessage());
            throw new IOException("Failed to read CSV file", exception);
        }

        System.out.println("\033[38;5;42mBooks imported successfully from CSV.\033[0m");



    }

//    @Command(command = "library", description = "Scan a book's ISBN barcode to retrieve metadata and add it to the library. Use --multi for batch scanning", group = "Book Create Commands")
    public void createBookScan(BookMetaDataResponse bookMetaDataResponse) {

//        if (multi) multiBookScan();

        String isbn = bookMetaDataResponse.isbn();

        if (cliPrompt.promptToConfirmBookAddition()) {
            String location = cliPrompt.promptForBookcaseLocation();

            Long bookcaseId = cliPrompt.promptForBookcaseSelection(promptOptions.bookCaseOptionsByLocation(location));
            if(bookcaseId == null) return;

            Long shelfId = cliPrompt.promptForShelfSelection(bookcaseId);
            if(shelfId == null) return;

            List<Long> authorIds = bookCreateIsbnCommands.createAuthorsFromMetaData(bookMetaDataResponse.authors());

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




    //todo: implement csv export
    @Command(command = "export", description = "Export library book data to a CSV file.")
    public void exportBooksToCsv(){
        log.info("Exporting books to CSV...");
    }



}
