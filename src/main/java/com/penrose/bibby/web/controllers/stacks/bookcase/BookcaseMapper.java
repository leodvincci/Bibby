package com.penrose.bibby.web.controllers.stacks.bookcase;

import com.penrose.bibby.library.stacks.bookcase.api.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import org.slf4j.Logger;

public class BookcaseMapper {

  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(BookcaseMapper.class);

  public static BookcaseDTO toDTO(Bookcase bookcase) {
    logger.info("Mapping Bookcase to DTO: {}", bookcase);
    return new BookcaseDTO(
        bookcase.getBookcaseId(),
        bookcase.getShelfCapacity(),
        bookcase.getBookCapacity(),
        bookcase.getBookcaseLocation(),
        bookcase.getBookcaseZone(),
        bookcase.getBookcaseIndex());
  }
}
