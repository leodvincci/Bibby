package com.penrose.bibby.cli;

import com.penrose.bibby.library.bookcase.Bookcase;
import com.penrose.bibby.library.bookcase.BookcaseEntity;
import com.penrose.bibby.library.bookcase.BookcaseService;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;

@Component
@Command(command = "bookcase", group = "Bookcase Commands")
public class BookcaseCommands extends AbstractShellComponent {

    private final ComponentFlow.Builder componentFlowBuilder;
    private final BookcaseService bookcaseService;

    public BookcaseCommands(ComponentFlow.Builder componentFlowBuilder, BookcaseService bookcaseService) {
        this.componentFlowBuilder = componentFlowBuilder;
        this.bookcaseService = bookcaseService;
    }


    @Command(command = "add", description = "Create a new bookcase in the library.")
    public void addBookcase() throws InterruptedException {
        ComponentFlow flow = componentFlowBuilder.clone()
                .withStringInput("bookcaseLabel")
                .name("Give this bookcase a label:_")
                .and()
                .withStringInput("shelfCapacity")
                .name("How many shelves?:_")
                .and()
                .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        bookcaseService.createNewBookCase(result.getContext().get("bookcaseLabel"),Integer.parseInt(result.getContext().get("shelfCapacity")));

    }

    @Command(command = "list" , description = "Display all bookcases currently in the library, along with their labels, total shelves")
    public void listAllBookcases(){
        for(BookcaseEntity b : bookcaseService.getAllBookcases()){
            System.out.println(b.getBookcaseLabel() + ":" + b.getShelfCapacity());
        }
    }


}
