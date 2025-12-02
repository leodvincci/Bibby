package com.penrose.bibby.library.shelf;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShelfController {

    ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }


}
