package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.bookcase.Bookcase;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShelfController {

    ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }


}
