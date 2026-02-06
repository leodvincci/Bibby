package com.penrose.bibby.library.stacks.bookcase.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookcaseJpaRepository extends JpaRepository<BookcaseEntity, Long> {
  BookcaseEntity findBookcaseEntityByBookcaseLabel(String s);

  List<BookcaseEntity> findAllByBookcaseLocation(String s);

  List<BookcaseEntity> findAllByUserId(Long userId);
}
