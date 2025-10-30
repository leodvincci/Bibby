package com.penrose.bibby.library.shelf;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelfRepository extends JpaRepository<ShelfEntity, Long> {

}
