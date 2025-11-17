package com.penrose.bibby.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BibbyResponses {

    List<String> bibbySearchResponses = new ArrayList<>(List.of(
            "Got it — searching the stacks for books by",
            "Sure thing — I’ll take a quick look through the shelves for",
            "Alright - Give me a sec...checking the catalogue for books by",
            "Hold on, I’m diving into the stacks — Let’s see what we’ve got by",
            "Searching for books by",
            "Let’s take a quiet look through the shelves for",
            "On it — I’ll go dig through the stacks. Hope the intern filed things alphabetically this time."
    ));


    public String bookCheckoutNotAvailable(){
        String bibbyResponse1 =
                        """
        
                            \u001B[38;5;63m  .---.
                            \u001B[38;5;63m (* @ *)  \u001B[36m\u001B[38;5;220m "This one’s already off the shelf. No double-dipping on checkouts."
                            \u001B[38;5;63m  \\|=|/
        
                        """;

        return bibbyResponse1;
    }





}
