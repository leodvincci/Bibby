package com.penrose.bibby.library.stacks.bookcase.infrastructure.persistence.mapping;

import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.persistence.entity.BookcaseEntity;
import org.slf4j.Logger;

public class BookcaseMapper {

  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(BookcaseMapper.class);

  public static Bookcase toDomain(BookcaseEntity bookcaseEntity) {
    return new Bookcase(
        bookcaseEntity.getBookcaseId(),
        bookcaseEntity.getUserId(),
        bookcaseEntity.getShelfCapacity(),
        bookcaseEntity.getBookCapacity(),
        bookcaseEntity.getBookcaseLocation(),
        bookcaseEntity.getBookcaseZone(),
        bookcaseEntity.getBookcaseIndex());
  }

  public static BookcaseEntity toEntity(Bookcase bookcase) {
    BookcaseEntity bookcaseEntity = new BookcaseEntity();
    bookcaseEntity.setUserId(bookcase.getUserId());
    bookcaseEntity.setShelfCapacity(bookcase.getShelfCapacity());
    bookcaseEntity.setBookCapacity(bookcase.getBookCapacity());
    bookcaseEntity.setBookcaseLocation(bookcase.getBookcaseLocation());
    bookcaseEntity.setBookcaseZone(bookcase.getBookcaseZone());
    bookcaseEntity.setBookcaseIndex(bookcase.getBookcaseIndex());
    logger.info("Mapping Bookcase to Entity: {}", bookcaseEntity);
    return bookcaseEntity;
  }
}
