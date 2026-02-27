package com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Placement;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.PlacementEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.PlacementJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlacementRepositoryPortImplTest {

  @Mock private PlacementJpaRepository placementJpaRepository;
  @InjectMocks private PlacementRepositoryPortImpl placementRepositoryPortImpl;
  @Captor private ArgumentCaptor<PlacementEntity> entityCaptor;

  @Test
  void placeBookOnShelf_shouldSaveEntityToRepository() {
    Placement placement = new Placement(1L, 2L);

    placementRepositoryPortImpl.placeBookOnShelf(placement);

    verify(placementJpaRepository).save(entityCaptor.capture());
    PlacementEntity captured = entityCaptor.getValue();
    assertThat(captured.getBookId()).isEqualTo(1L);
    assertThat(captured.getShelfId()).isEqualTo(2L);
  }

  @Test
  void placeBookOnShelf_shouldMapDomainToEntityCorrectly() {
    Placement placement = new Placement(42L, 99L);

    placementRepositoryPortImpl.placeBookOnShelf(placement);

    verify(placementJpaRepository).save(entityCaptor.capture());
    PlacementEntity captured = entityCaptor.getValue();
    assertThat(captured.getBookId()).isEqualTo(42L);
    assertThat(captured.getShelfId()).isEqualTo(99L);
  }
}
