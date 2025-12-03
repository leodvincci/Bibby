package com.penrose.bibby.cli.book;

import com.penrose.bibby.cli.prompt.application.CliPromptService;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
@Command
public class BookScanCommandLine {
    private final CliPromptService cliPrompt;

    public BookScanCommandLine(CliPromptService cliPrompt) {
        this.cliPrompt = cliPrompt;
    }


}
