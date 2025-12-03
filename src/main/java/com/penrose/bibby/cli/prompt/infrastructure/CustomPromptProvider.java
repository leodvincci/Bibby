package com.penrose.bibby.cli.prompt.infrastructure;

import org.jline.utils.*;
import org.springframework.shell.jline.*;
import org.springframework.stereotype.Component;


@Component
public class CustomPromptProvider implements PromptProvider {

    @Override
    public AttributedString getPrompt(){
        return new AttributedString("Guest" + " </>\uD835\uDC01\uD835\uDC08\uD835\uDC01\uD835\uDC01\uD835\uDC18:_ ",AttributedStyle.DEFAULT
                .foreground(AttributedStyle.GREEN)
                .bold()

        );
    }

}
