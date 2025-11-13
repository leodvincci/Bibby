package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository <BookEntity, Long> {

    BookEntity findBookEntityByTitle(String title);

    BookEntity findByTitle(String title);

    BookEntity findByTitleIgnoreCase(String title);

    List<BookEntity> findByTitleContaining(String title);

    List<BookEntity> findByShelfId(Long id);
}
