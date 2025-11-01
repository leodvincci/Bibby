package com.penrose.bibby.library.shelf;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShelfRepository extends JpaRepository<ShelfEntity, Long> {

    List<ShelfEntity> findByBookcaseId(Long bookcaseId);
}
