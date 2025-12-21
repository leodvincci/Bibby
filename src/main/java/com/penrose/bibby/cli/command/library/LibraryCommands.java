package com.penrose.bibby.cli.command.library;

import org.slf4j.Logger;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellOption;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ShellComponent
    @Command(command = "library", group = "Library Commands")
public class LibraryCommands {
    Logger log = org.slf4j.LoggerFactory.getLogger(LibraryCommands.class);


    public LibraryCommands() {

    }

    //todo: implement csv import
    @Command(command = "import", description = "Import CSV of ISBNs to add multiple books at once.")
    public void createBooksFromCsv(
            @ShellOption String filePath) throws IOException {

        log.info("Importing books from CSV...");


        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }catch (IOException exception) {
            log.error("Error reading CSV file: {}", exception.getMessage());
            throw new IOException("Failed to read CSV file", exception);
        }



    }

    //todo: implement csv export
    @Command(command = "export", description = "Export library book data to a CSV file.")
    public void exportBooksToCsv(){
        log.info("Exporting books to CSV...");
    }



}
