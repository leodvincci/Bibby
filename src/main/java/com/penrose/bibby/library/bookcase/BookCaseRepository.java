package com.penrose.bibby.library.bookcase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookCaseRepository extends JpaRepository<BookCaseEntity , Long> {
}
