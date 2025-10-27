package com.penrose.bibby;

import org.jline.utils.*;
import org.springframework.shell.jline.*;
import org.springframework.stereotype.Component;


@Component
public class CustomPromptProvider implements PromptProvider {

    @Override
    public AttributedString getPrompt(){
        return new AttributedString("Bibby:_ ",AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
    }

}
