package com.penrose.bibby.cli.command.library;

import org.slf4j.Logger;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
    @Command(command = "library", group = "Library Commands")
public class LibraryCommands {
    Logger log = org.slf4j.LoggerFactory.getLogger(LibraryCommands.class);


    public LibraryCommands() {

    }

    //todo: implement csv import
    @Command(command = "import", description = "Import CSV of ISBNs to add multiple books at once.")
    public void createBooksFromCsv(
            @ShellOption String filePath){

        log.info("Importing books from CSV...");

    }

    //todo: implement csv export
    @Command(command = "export", description = "Export library book data to a CSV file.")
    public void exportBooksToCsv(){
        log.info("Exporting books to CSV...");
    }



}
