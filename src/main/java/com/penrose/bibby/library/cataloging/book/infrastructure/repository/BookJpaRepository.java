package com.penrose.bibby.library.cataloging.book.infrastructure.repository;

import com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookDetailView;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BookSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookJpaRepository extends JpaRepository <BookEntity, Long> {

    BookEntity findBookEntityByTitle(String title);

    BookEntity findByTitle(String title);

    Optional<BookEntity> findByTitleIgnoreCase(String title);

    List<BookEntity> findByTitleContaining(String title);

    List<BookEntity> findByShelfId(Long id);

    List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);


    @Query(value = """
        SELECT b.book_id, b.title,
               STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
               bc.bookcase_label, s.shelf_label, b.availability_status
        FROM books b
        JOIN book_authors ba ON b.book_id = ba.book_id
        JOIN authors a ON ba.author_id = a.author_id
        JOIN shelves s ON s.shelf_id = b.shelf_id
        JOIN bookcases bc ON bc.bookcase_id = s.bookcase_id
        WHERE b.book_id = :bookId
        GROUP BY b.book_id, b.title, bc.bookcase_label, s.shelf_label, b.availability_status
    """, nativeQuery = true)
    BookDetailView getBookDetailView(Long bookId);

    long countByShelfId(Long shelfId);

    BookEntity findByIsbn(String isbn);

    List<BookEntity> findByAuthorsAuthorId(Long id);
}
