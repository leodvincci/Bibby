package com.penrose.bibby.cli.ui;

import com.penrose.bibby.cli.ConsoleColors;
import org.springframework.stereotype.Component;

@Component
public class BookcardRenderer {

    public BookcardRenderer(){

    }


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
