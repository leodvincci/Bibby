package com.penrose.bibby.web.controllers.stacks.shelf;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfCommandFacade;
import com.penrose.bibby.library.stacks.shelf.core.ports.inbound.ShelfQueryFacade;
import com.penrose.bibby.web.controllers.stacks.shelf.mappers.ShelfResponseMapper;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/shelves")
@RestController
public class ShelfController {

  private final ShelfQueryFacade shelfQueryFacade;
  private final ShelfCommandFacade shelfCommandFacade;
  private final ShelfResponseMapper shelfResponseMapper;

  public ShelfController(ShelfQueryFacade shelfQueryFacade, ShelfCommandFacade shelfCommandFacade, ShelfResponseMapper shelfResponseMapper) {
    this.shelfQueryFacade = shelfQueryFacade;
    this.shelfCommandFacade = shelfCommandFacade;
    this.shelfResponseMapper = shelfResponseMapper;
  }

  @GetMapping("/options")
  public List<ShelfOptionResponse> getShelfOptions() {
    return shelfQueryFacade.findAll().stream().map(shelfResponseMapper::toShelfOption).toList();
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
    return shelfQueryFacade.findShelvesByBookcaseId(bookcaseId).stream()
        .map(shelfResponseMapper::toShelfOption)
        .toList();
  }

  public record AddBookToShelfRequest(Long bookId, Long shelfId) {}

  @PostMapping("/placements")
  public void addBookToShelf(@RequestBody AddBookToShelfRequest request) {
    shelfCommandFacade.placeBookOnShelf(request.bookId(), request.shelfId());
  }
}
