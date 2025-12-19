package com.penrose.bibby.library.stacks.bookcase.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface BookcaseRepository {
    BookcaseEntity findBookcaseEntityByBookcaseLabel(String s);
    List<BookcaseEntity> findAll();

    List<String> getAllBookCaseLocations();

    void save(BookcaseEntity bookcaseEntity);

    Optional<BookcaseEntity> findById(Long id);

    List<BookcaseEntity> findByLocation(String location);
}
