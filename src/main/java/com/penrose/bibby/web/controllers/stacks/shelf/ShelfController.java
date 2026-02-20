package com.penrose.bibby.web.controllers.stacks.shelf;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
import com.penrose.bibby.web.controllers.stacks.shelf.mappers.ShelfResponseMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/shelves")
@RestController
public class ShelfController {

  ShelfService shelfService;
  ShelfResponseMapper shelfResponseMapper;

  public ShelfController(ShelfService shelfService, ShelfResponseMapper shelfResponseMapper) {
    this.shelfService = shelfService;
    this.shelfResponseMapper = shelfResponseMapper;
  }

  @GetMapping("/options")
  public List<ShelfOptionResponse> getShelfOptions() {
    return shelfService.getShelfOptions().stream().map(shelfResponseMapper::toShelfOption).toList();
  }

  /**
   * Retrieves a list of shelf options associated with a specific bookcase.
   *
   * @param bookcaseId the ID of the bookcase for which shelf options are being retrieved
   * @return a list of {@link ShelfOptionResponse} objects representing the available shelf options
   *     for the given bookcase
   */
  @GetMapping("/options/{bookcaseId}")
  public List<ShelfOptionResponse> getShelfOptionsByBookcase(@PathVariable Long bookcaseId) {
    return shelfService.getShelfOptionsByBookcase(bookcaseId).stream()
        .map(shelfResponseMapper::toShelfOption)
        .toList();
  }
}
