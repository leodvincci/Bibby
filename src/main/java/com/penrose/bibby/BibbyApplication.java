package com.penrose.bibby;

import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import java.text.AttributedString;

@SpringBootApplication
public class BibbyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibbyApplication.class, args);
    }
}
