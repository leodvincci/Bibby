package com.penrose.bibby.web.shelf;

import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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

  @GetMapping("/api/v1/shelves/options/{bookcaseId}")
  public List<ShelfOptionResponse> getShelfOptionsByBookcase(@PathVariable Long bookcaseId) {
    return shelfService.getShelfOptionsByBookcase(bookcaseId);
  }
}
