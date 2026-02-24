package com.penrose.bibby.library.stacks.bookcase.core.domain;

import com.penrose.bibby.library.stacks.bookcase.api.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;

import java.util.List;

public class BookcaseMapper {
    public static Bookcase toDomain(BookcaseEntity bookcaseEntity) {
        return new Bookcase(
                bookcaseEntity.getBookcaseId(),
                bookcaseEntity.getShelfCapacity(),
                bookcaseEntity.getBookCapacity(),
                bookcaseEntity.getBookcaseLocation(),
                bookcaseEntity.getBookcaseZone(),
                bookcaseEntity.getBookcaseIndex());
    }

    public static BookcaseEntity toEntity(Bookcase bookcase) {
        BookcaseEntity bookcaseEntity = new BookcaseEntity();
        bookcaseEntity.setBookcaseId(bookcase.getBookcaseId());
        bookcaseEntity.setShelfCapacity(bookcase.getShelfCapacity());
        bookcaseEntity.setBookCapacity(bookcase.getBookCapacity());
        bookcaseEntity.setBookcaseLocation(bookcase.getBookcaseLocation());
        return bookcaseEntity;
    }

    public static BookcaseDTO toDTO(Bookcase bookcase) {
        return new BookcaseDTO(
                bookcase.getBookcaseId(),
                bookcase.getShelfCapacity(),
                bookcase.getBookCapacity(),
                bookcase.getBookcaseLocation(),
                bookcase.getBookcaseZone(),
                bookcase.getBookcaseIndex());
    }

}
