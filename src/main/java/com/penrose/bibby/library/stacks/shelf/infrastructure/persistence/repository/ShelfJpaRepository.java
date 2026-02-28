package com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.repository;

import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.entity.ShelfEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelfJpaRepository extends JpaRepository<ShelfEntity, Long> {

  List<ShelfEntity> findByBookcaseId(Long bookcaseId);

  List<ShelfEntity> getShelfEntitiesByBookcaseId(Long bookcaseId);

  void deleteByBookcaseId(Long bookcaseId);
}
