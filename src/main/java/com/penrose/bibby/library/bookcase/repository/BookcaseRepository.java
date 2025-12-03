package com.penrose.bibby.library.bookcase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookcaseRepository extends JpaRepository<BookcaseEntity, Long> {
    BookcaseEntity findBookcaseEntityByBookcaseLabel(String s);
}
