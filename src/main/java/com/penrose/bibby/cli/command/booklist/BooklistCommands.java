package com.penrose.bibby.cli.command.booklist;

import org.springframework.shell.command.annotation.Command;
import org.springframework.stereotype.Component;

@Component
@Command(command = "booklist", group = "Booklist Commands")
public class BooklistCommands {

    public BooklistCommands() {

    }


    @Command(command = "view", description = "\u001B[36mView existing booklists in the library.\u001B[0m")
    public void viewBooklists() {

        System.out.println("Viewing existing booklists...");

    }

    @Command(command = "new", description = "\u001B[36mCreate a new booklist in the library.\u001B[0m")
    public void createNewBooklist() {

        System.out.println("Creating a new booklist...");

    }


}
