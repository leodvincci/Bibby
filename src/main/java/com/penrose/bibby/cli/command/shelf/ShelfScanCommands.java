package com.penrose.bibby.cli.command.shelf;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
@Command(command = "shelf", group = "Shelf Commands")
public class ShelfScanCommands {

    public ShelfScanCommands(){

    }

    @Command(command = "scan", description = "Scan books on a shelf." , group = "Shelf Commands")
    public void shelfScan(){
        System.out.println("Shelf scan command executed.");
    }


}
