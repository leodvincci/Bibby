package com.penrose.bibby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

@CommandScan
@SpringBootApplication
public class BibbyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibbyApplication.class, args);
    }
}
