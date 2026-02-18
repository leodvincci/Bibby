package com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.ShelfDomainRepository;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.ports.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShelfDomainRepositoryImpl implements ShelfDomainRepository {

  private final ShelfJpaRepository jpaRepository;
  private final ShelfMapper shelfMapper;
  private final BookAccessPort bookAccessPort;

  public ShelfDomainRepositoryImpl(
      ShelfJpaRepository jpaRepository, ShelfMapper shelfMapper, BookAccessPort bookAccessPort) {
    this.jpaRepository = jpaRepository;
    this.shelfMapper = shelfMapper;
    this.bookAccessPort = bookAccessPort;
  }

  @Override
  public Shelf getById(ShelfId id) {
    ShelfEntity entity = jpaRepository.findById(id.shelfId()).orElse(null);
    List<Long> bookIds = bookAccessPort.getBookIdsByShelfId(id.shelfId());
    return shelfMapper.toDomainFromDTO(entity, bookIds);
  }

  @Override
  public void save(Shelf shelf) {
    ShelfEntity entity = jpaRepository.findById(shelf.getId()).get();
    //        entity = shelfMapper.updateEntity(shelf);
    jpaRepository.save(entity);
  }
}
