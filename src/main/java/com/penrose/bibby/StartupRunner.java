package com.penrose.bibby;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> Bibby is awake. Seeding data now...");
        // call whatever you want here
        // e.g. create a book, save it, print results, etc.
        // bookService.addBook("Pride and Prejudice", "Jane Austen");
    }
}