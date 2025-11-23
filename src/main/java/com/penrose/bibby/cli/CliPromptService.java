package com.penrose.bibby.cli;

import com.penrose.bibby.library.author.Author;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.stereotype.Component;

@Component
public class CliPromptService {
    private final ComponentFlow.Builder componentFlowBuilder;

    public CliPromptService(ComponentFlow.Builder componentFlowBuilder, ComponentFlow.Builder componentFlowBuilder1) {

        this.componentFlowBuilder = componentFlowBuilder1;
    }

    public Author promptForAuthor(String title){
        ComponentFlow flow;
        flow = componentFlowBuilder.clone()
                .withStringInput("authorFirstName")
                .name("Author's First Name:_")
                .and()
                .withStringInput("authorLastName")
                .name("Author's Last Name:_")
                .and().build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        String firstName  = result.getContext().get("authorFirstName", String.class);
        String lastName = result.getContext().get("authorLastName", String.class);
//        BookRequestDTO bookRequestDTO = new BookRequestDTO(title,firstName, lastName);
//        bookService.createNewBook(bookRequestDTO);
        return new Author(firstName,lastName);

    }


}
