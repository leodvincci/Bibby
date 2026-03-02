package com.penrose.bibby.library.stacks.bookcase.infrastructure.persistence.repository;

import com.penrose.bibby.library.stacks.bookcase.infrastructure.persistence.entity.BookcaseEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookcaseJpaRepository extends JpaRepository<BookcaseEntity, Long> {
  BookcaseEntity findBookcaseEntityByBookcaseLocation(String s);

  List<BookcaseEntity> findAllByBookcaseLocation(String s);

  List<BookcaseEntity> findAllByUserId(Long userId);
}
