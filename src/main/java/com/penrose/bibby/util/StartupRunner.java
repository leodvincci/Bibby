package com.penrose.bibby.util;
import com.penrose.bibby.library.shelf.application.ShelfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {
    @Autowired
    ShelfService shelfService;
    @Override
    public void run(String... args) throws Exception {

        System.out.println(">>> Bibby is awake. Seeding data now...");

        // TODO: Add data seeding logic here if needed
    }
}