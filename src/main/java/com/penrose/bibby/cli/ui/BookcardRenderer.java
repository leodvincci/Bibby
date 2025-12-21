package com.penrose.bibby.cli.ui;

import org.springframework.stereotype.Component;

/**
 * Renders book-related information into formatted visual outputs for the console,
 * including book cards, error messages, and other data presentations.
 *
 * This class is primarily responsible for creating formatted console output
 * using colors, alignment, and other visual aspects to enhance readability and usability.
 * Methods in this class handle formatting of book data, counting authors,
 * and providing fallback messaging when no results are found.
 *
 * Dependencies:
 *  - `ConsoleColors` class for defining text colors.
 *  - Method utilities for string formatting and operations.
 */
@Component
public class BookcardRenderer {

    public BookcardRenderer(){

    }


    /**
     * Creates a formatted book card with the specified metadata.
     *
     * @param title     the title of the book
     * @param isbn      the International Standard Book Number (ISBN) of the book
     * @param author    the author(s) of the book
     * @param publisher the publisher of the book
     * @param bookcase  the label or identifier of the bookcase where the book is stored
     * @param shelf     the label or identifier of the shelf where the book is stored
     * @param location  the location (e.g., library branch or area) where the book is stored
     * @return a formatted string representing the book card, including styled details such as title, ISBN, author, publisher, location, bookcase, and shelf
     */
    public String createBookCard(String title, String isbn, String author, String publisher, String bookcase, String shelf, String location) {

        // %-42s ensures the text is left-aligned and padded to 42 characters
        // The emojis take up extra visual space, so adjusted padding slightly
        return """
                
                ╭──────────────────────────────────────────────────────────────────────────────────────╮
                │  📖 \033[38;5;63m%-73s\033[0m        │     
                ├──────────────────────────────────────────────────────────────────────────────────────┤
                │  \033[38;5;42mISBN\033[0m:      %-31s                                          │
                │  \033[38;5;42mAuthor\033[0m:    %-31.31s%-3.3s                                       │                                                              
                │  \033[38;5;42mPublisher\033[0m: %-31s                                          │
                │                                                                                      │
                │  \033[38;5;42mLocation\033[0m:  %-35s                                      │
                │  \033[38;5;42mBookcase\033[0m:  %-35s                                      │
                │  \033[38;5;42mShelf\033[0m:     %-35s                                      │
                ╰──────────────────────────────────────────────────────────────────────────────────────╯
                
                
        """.formatted(
                title,
                isbn,
                formater(author),
                author.length() > 42 ? "..." : " ",
                publisher,
                location,
                bookcase,
                shelf
        );
    }

    public String bookImportCard(String title, String isbn, String author, String publisher) {

        // %-42s ensures the text is left-aligned and padded to 42 characters
        // The emojis take up extra visual space, so adjusted padding slightly
        return """
                
                ╭──────────────────────────────────────────────────────────────────────────────────────╮
                │  📖 \033[38;5;63m%-73s\033[0m        │     
                ├──────────────────────────────────────────────────────────────────────────────────────┤
                │  \033[38;5;42mISBN\033[0m:      %-31s                                          │
                │  \033[38;5;42mAuthor\033[0m:    %-31.31s%-3.3s                                       │                                                              
                │  \033[38;5;42mPublisher\033[0m: %-31.31s                                          │
                ╰──────────────────────────────────────────────────────────────────────────────────────╯
                
                
        """.formatted(
                title,
                isbn,
                formater(author),
                author.length() > 42 ? "..." : " ",
                publisher,
                publisher.length() > 32 ? "..." : " "

                );
    }



    public String formater(String authors){
        String normalizedAuthors = authors.replaceAll("[\\[\\]]", ""); // Remove brackets
        authors = normalizedAuthors.replaceAll(",\\s*", ","); // Ensure single space after commas
        return authors;
    }

    public int countAuthors(String authors) {
        String[] authorArray = authors.split(",");
        return authorArray.length;
    }

    public void printNotFound(String title) {
        String msg = """
                
                
                ╭──────────────────────────────────────────────╮
                │  🚫 No Results Found                         │
                ├──────────────────────────────────────────────┤
           \033[0m     │  \033[0mQuery:\033[0m  %-34s  │
                │                                              │
                │  Status: Not in library.                     │
                │  Action: Check spelling or add new book.     │
                ╰──────────────────────────────────────────────╯
                
                
        """.formatted(
                title.length() > 34 ? title.substring(0, 31) + "..." : title
        ); // Truncates title if it's too long to fit the box

        System.out.println(ConsoleColors.RED + msg + ConsoleColors.RESET);
    }

}
