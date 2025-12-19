package com.penrose.bibby.library.stacks.bookcase.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookcaseJpaRepository extends JpaRepository<BookcaseEntity, Long> {
    BookcaseEntity findBookcaseEntityByBookcaseLabel(String s);

    List<BookcaseEntity> findAllByBookcaseLocation(String s);
}
