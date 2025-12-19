package com.penrose.bibby.web.shelf;

import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@RestController
public class ShelfController {

    ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    @GetMapping("/api/v1/shelves/options")
    public List<ShelfOptionResponse> getShelfOptions() {
        return shelfService.getShelfOptions();
    }

}
