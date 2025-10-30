package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.bookcase.BookCase;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShelfController {

    ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    public void addBookToShelf(Book book, BookCase bookCase){
        shelfService.addToShelf(book,"A-42");
        System.out.println("Controller adding to shelf...");
    }

}
