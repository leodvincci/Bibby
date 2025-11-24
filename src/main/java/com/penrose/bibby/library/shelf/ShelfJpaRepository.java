package com.penrose.bibby.library.shelf;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShelfJpaRepository extends JpaRepository<ShelfEntity, Long> {

    List<ShelfEntity> findByBookcaseId(Long bookcaseId);

    @Query("""
        SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
            s.shelfId,
            s.shelfLabel,
            COUNT(b.bookId)
        )
        FROM ShelfEntity s
        LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
        WHERE s.bookcaseId = :bookcaseId
        GROUP BY s.shelfId, s.shelfLabel
        ORDER BY s.shelfPosition ASC
        """)
    List<ShelfSummary> findShelfSummariesByBookcaseId(@Param("bookcaseId") Long bookcaseId);

}
