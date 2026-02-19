package com.penrose.bibby.library.stacks.shelf.infrastructure.repository;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelfJpaRepository extends JpaRepository<ShelfEntity, Long> {

  List<ShelfEntity> findByBookcaseId(Long bookcaseId);

  @Query(
      """
        SELECT new com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfSummary(
            s.shelfId,
            s.shelfLabel,
            (SELECT COUNT(b.bookId) FROM com.penrose.bibby.library.cataloging.book.infrastructure.entity.BookEntity b WHERE b.shelfId = s.shelfId)
        )
        FROM ShelfEntity s
        WHERE s.bookcaseId = :bookcaseId
        ORDER BY s.shelfPosition ASC
        """)
  List<ShelfSummary> findShelfSummariesByBookcaseId(@Param("bookcaseId") Long bookcaseId);

  List<ShelfEntity> getShelfEntitiesByBookcaseId(Long bookcaseId);

  void deleteByBookcaseId(Long bookcaseId);
}
