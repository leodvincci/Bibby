package com.penrose.bibby.library.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository <BookEntity, Long> {

    BookEntity findBookEntityByTitle(String title);

    BookEntity findByTitle(String title);

    BookEntity findByTitleIgnoreCase(String title);
}
