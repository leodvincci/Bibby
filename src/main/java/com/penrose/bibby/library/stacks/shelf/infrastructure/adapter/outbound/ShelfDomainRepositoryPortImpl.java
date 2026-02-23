package com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ShelfDomainRepositoryPortImpl implements ShelfDomainRepositoryPort {

  private final ShelfJpaRepository jpaRepository;
  private final ShelfMapper shelfMapper;
  private final BookAccessPort bookAccessPort;
  private final Logger logger =
      org.slf4j.LoggerFactory.getLogger(ShelfDomainRepositoryPortImpl.class);

  public ShelfDomainRepositoryPortImpl(
      ShelfJpaRepository jpaRepository, ShelfMapper shelfMapper, BookAccessPort bookAccessPort) {
    this.jpaRepository = jpaRepository;
    this.shelfMapper = shelfMapper;
    this.bookAccessPort = bookAccessPort;
  }

  @Override
  public Shelf getById(ShelfId id) {
    ShelfEntity entity = jpaRepository.findById(id.shelfId()).orElse(null);
    if (entity == null) {
      return null;
    }
    List<Long> bookIds = bookAccessPort.getBookIdsByShelfId(id.shelfId());
    return shelfMapper.toDomainFromEntity(entity, bookIds);
  }

  @Override
  public void save(Shelf shelf) {
    ShelfEntity entity = new ShelfEntity();
    entity.setBookcaseId(shelf.getBookcaseId());
    entity.setShelfPosition(shelf.getShelfPosition());
    entity.setShelfLabel(shelf.getShelfLabel());
    entity.setBookCapacity(shelf.getBookCapacity());
    jpaRepository.save(entity);
    logger.info(
        "Shelf created with ID: {} for bookcase: {}", entity.getShelfId(), shelf.getBookcaseId());
  }

  @Override
  public void deleteByBookcaseId(Long bookcaseId) {
    jpaRepository.deleteByBookcaseId(bookcaseId);
    logger.info("Deleted shelves for bookcase with ID: {}", bookcaseId);
  }

  @Override
  public Shelf findById(Long shelfId) {
    ShelfEntity entity = jpaRepository.findById(shelfId).orElse(null);
    if (entity == null) {
      throw new RuntimeException("Shelf not found with ID: " + shelfId);
    }
    List<Long> bookIds = bookAccessPort.getBookIdsByShelfId(shelfId);

    return shelfMapper.toDomainFromEntity(entity, bookIds);
  }

  /**
   * Retrieves a list of shelves for a given bookcase, suitable for populating shelf selection
   * options.
   *
   * @param bookcaseId the ID of the bookcase whose shelves are to be retrieved
   * @return a list of {@link Shelf} domain objects associated with the specified bookcase, each
   *     populated with its corresponding book IDs
   */
  @Override
  public List<Shelf> findByBookcaseId(Long bookcaseId) {
    return jpaRepository.findByBookcaseId(bookcaseId).stream()
        .map(
            shelfEntity ->
                shelfMapper.toDomainFromEntity(
                    shelfEntity, bookAccessPort.getBookIdsByShelfId(shelfEntity.getShelfId())))
        .toList();
  }

  @Override
  public List<Shelf> findAll() {
    return jpaRepository.findAll().stream()
        .map(
            shelfEntity ->
                shelfMapper.toDomainFromEntity(
                    shelfEntity, bookAccessPort.getBookIdsByShelfId(shelfEntity.getShelfId())))
        .toList();
  }
}
