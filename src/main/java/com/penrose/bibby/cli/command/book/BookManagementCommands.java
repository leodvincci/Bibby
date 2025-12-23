package com.penrose.bibby.cli.command.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
@Command(command = "book" , group = "Book Management Commands")
public class BookManagementCommands {

    private final CliPromptService cliPromptService;

    public BookManagementCommands(CliPromptService cliPromptService){
        this.cliPromptService = cliPromptService;
    }

    @Command(command = "edit"
            , description = """
                            \u001B[38;5;185mEdit existing book details in your library. Update metadata, authors, and other information as needed.
                            \u001B[0m
                            """
            , group = "Book Management Commands")
    public void BookEditCommand(){
        System.out.println("\n\u001B[95mEdit Book\u001B[0m (':q' to quit)");
        String isbn = cliPromptService.promptForIsbn();
        System.out.println("Editing book with ISBN: " + isbn);

    }

}
