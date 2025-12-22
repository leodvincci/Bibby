package com.penrose.bibby.cli.command.shelf;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
@Command(command = "shelf", group = "Shelf Commands")
public class ShelfScanCommands {

    public ShelfScanCommands(){

    }

    @Command(command = "shelve",
            description = """ 
                    \u001B[38;5;3mShelve an entire shelf from a file of ISBNs. Select a target shelf, then import the ISBN list
                    Bibby will create missing books from metadata and place all found books onto that shelf (create-or-place).
                    \u001B[0m"""
            , group = "Shelf Commands")
    public void shelfScan(){
        System.out.println("Shelf scan command executed.");
    }


}
