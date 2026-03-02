package com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.adapter;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Placement;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.PlacementRepositoryPort;
import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.mapping.PlacementMapper;
import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.repository.PlacementJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PlacementRepositoryAdapter implements PlacementRepositoryPort {

  private final PlacementJpaRepository placementJpaRepository;
  private final Logger logger = LoggerFactory.getLogger(PlacementRepositoryAdapter.class);

  public PlacementRepositoryAdapter(PlacementJpaRepository placementJpaRepository) {
    this.placementJpaRepository = placementJpaRepository;
  }

  @Override
  public void placeBookOnShelf(Placement placement) {
    placementJpaRepository.save(PlacementMapper.toEntity(placement));
    logger.info(
        "Book with id {} placed on shelf with id {}",
        placement.getBookId(),
        placement.getShelfId());
  }
}
