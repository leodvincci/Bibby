package com.penrose.bibby;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;

@Component
@Command(command = "book", group = "Book Commands")
public class BookCommands extends AbstractShellComponent {

    private final ComponentFlow.Builder componentFlowBuilder;

    public BookCommands(ComponentFlow.Builder componentFlowBuilder) {
        this.componentFlowBuilder = componentFlowBuilder;
    }

    @Command(command = "add", description = "Add a new book")
    public void addBook(){
        ComponentFlow flow;
        flow =componentFlowBuilder.clone()
                .withStringInput("title")
                    .name("Book Title:_")
                    .and()
                .withStringInput("author")
                    .name("Author Name:_")
                    .and()
                .withStringInput("genre")
                    .name("Genre (optional):_")
                    .and()
                .withStringInput("year")
                    .name("Year (optional):_")
                    .and()
                .build();
        ComponentFlow.ComponentFlowResult result = flow.run();

        String title  = result.getContext().get("title", String.class);
        String author = result.getContext().get("author", String.class);
        String genre  = result.getContext().get("genre", String.class);
        String year   = result.getContext().get("year", String.class);

        System.out.printf(
                """
                        
                        \u001B[36m</>\033[0m: I added the book\u001B[36m %s\033[0m by\u001B[36m %s\033[0m to your library
                        
                        """,
                title, author
        );

    }
}
