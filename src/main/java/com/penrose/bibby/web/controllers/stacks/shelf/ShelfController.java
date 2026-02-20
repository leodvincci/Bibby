package com.penrose.bibby.web.controllers.stacks.shelf;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.core.application.usecases.QueryShelfUseCase;
import com.penrose.bibby.web.controllers.stacks.shelf.mappers.ShelfResponseMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/shelves")
@RestController
public class ShelfController {

  QueryShelfUseCase queryShelfUseCase;
  ShelfResponseMapper shelfResponseMapper;

  public ShelfController(
      QueryShelfUseCase queryShelfUseCase, ShelfResponseMapper shelfResponseMapper) {
    this.queryShelfUseCase = queryShelfUseCase;
    this.shelfResponseMapper = shelfResponseMapper;
  }

  @GetMapping("/options")
  public List<ShelfOptionResponse> getShelfOptions() {
    return queryShelfUseCase.findAll().stream().map(shelfResponseMapper::toShelfOption).toList();
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
    return queryShelfUseCase.findAllShelves(bookcaseId).stream()
        .map(shelfResponseMapper::toShelfOption)
        .toList();
  }
}
