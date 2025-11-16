package com.penrose.bibby.cli.book;

import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.stereotype.Component;

@Component
public class BookComponentFlow {

    private final ComponentFlow.Builder componentFlowBuilder;
    ComponentFlow componentFlow;

    public BookComponentFlow(ComponentFlow.Builder componentFlowBuilder) {
        this.componentFlowBuilder = componentFlowBuilder;
    }


    public String[] getAuthorName(){
        componentFlow = componentFlowBuilder.clone()
                .withStringInput("authorFirstName")
                .name("Author's First Name:_")
                .and()
                .withStringInput("authorLastName")
                .name("Author's Last Name:_")
                .and().build();

        ComponentFlow.ComponentFlowResult res = componentFlow.run();

        String firstName = res.getContext().get("authorFirstName", String.class);
        String lastName = res.getContext().get("authorLastName", String.class);

        return new String[] {firstName,lastName};
    }

    public String getBookTitle(){
        componentFlow = componentFlowBuilder.clone()
                .withStringInput("title")
                .name("Book Title:_")
                .and()
                .build();
        ComponentFlow.ComponentFlowResult result = componentFlow.run();
        return result.getContext().get("title", String.class);
    }

    public int getAuthorCount(){
        componentFlow = componentFlowBuilder.clone()
                .withStringInput("author_count")
                .name("Number of Authors:_")
                .and()
                .build();
        ComponentFlow.ComponentFlowResult result = componentFlow.run();
        return Integer.parseInt(result.getContext().get("author_count", String.class));
    }


}
