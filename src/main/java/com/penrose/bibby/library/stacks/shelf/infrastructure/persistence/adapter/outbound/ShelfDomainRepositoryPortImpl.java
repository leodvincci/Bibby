package com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.adapter.outbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepositoryPort;
import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.mapping.ShelfMapper;
import com.penrose.bibby.library.stacks.shelf.infrastructure.persistence.repository.ShelfJpaRepository;
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

  /**
   * {@inheritDoc}
   *
   * @throws RuntimeException if no shelf entity exists for the given ID
   */
  @Override
  public Shelf getShelfByShelfId(ShelfId id) {
    ShelfEntity entity = jpaRepository.findById(id.shelfId()).orElse(null);
    if (entity == null) {
      throw new RuntimeException("Shelf not found with ID: " + id);
    }
    List<Long> bookIds = bookAccessPort.getBookIdsByShelfId(id.shelfId());
    return shelfMapper.toDomainFromEntity(entity, bookIds);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Persists the shelf entity via JPA and logs the newly assigned shelf ID.
   */
  @Override
  public void createNewShelfInBookcase(Shelf shelf) {
    ShelfEntity shelfEntity = shelfMapper.toEntity(shelf);
    jpaRepository.save(shelfEntity);
    logger.info(
        "Shelf created with ID: {} for bookcase: {}",
        shelfEntity.getShelfId(),
        shelf.getBookcaseId());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Deletes all shelf rows matching the bookcase ID and logs the operation.
   */
  @Override
  public void deleteByBookcaseId(Long bookcaseId) {
    jpaRepository.deleteByBookcaseId(bookcaseId);
    logger.info("Deleted shelves for bookcase with ID: {}", bookcaseId);
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

  /**
   * {@inheritDoc}
   *
   * <p>Loads every shelf entity from the database and enriches each with its associated book IDs
   * before mapping to the domain model.
   */
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
