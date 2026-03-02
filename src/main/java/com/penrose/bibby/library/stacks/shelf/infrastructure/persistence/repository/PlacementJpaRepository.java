package com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.repository;

import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.entity.PlacementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementJpaRepository extends JpaRepository<PlacementEntity, Long> {}
