package com.penrose.bibby.library.stacks.shelf.infrastructure.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Placement;
import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.entity.PlacementEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.mapping.PlacementMapper;
import org.junit.jupiter.api.Test;

class PlacementMapperTest {

  @Test
  void toEntity_shouldMapBookIdAndShelfId() {
    Placement placement = new Placement(1L, 2L);

    PlacementEntity entity = PlacementMapper.toEntity(placement);

    assertThat(entity.getBookId()).isEqualTo(1L);
    assertThat(entity.getShelfId()).isEqualTo(2L);
  }

  @Test
  void toEntity_shouldCreateNewEntityWithNoId() {
    Placement placement = new Placement(10L, 20L);

    PlacementEntity entity = PlacementMapper.toEntity(placement);

    assertThat(entity.getId()).isNull();
  }
}
