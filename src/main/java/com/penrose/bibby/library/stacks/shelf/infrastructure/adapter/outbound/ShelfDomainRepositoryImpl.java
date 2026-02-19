package com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound;

import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.ports.outbound.ShelfDomainRepository;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ShelfDomainRepositoryImpl implements ShelfDomainRepository {

  private final ShelfJpaRepository jpaRepository;
  private final ShelfMapper shelfMapper;
  private final BookAccessPort bookAccessPort;
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(ShelfDomainRepositoryImpl.class);

  public ShelfDomainRepositoryImpl(
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
  public void save(Long bookcaseId, int position, String shelfLabel, int bookCapacity) {
    ShelfEntity entity = new ShelfEntity();
    entity.setBookcaseId(bookcaseId);
    entity.setShelfPosition(position);
    entity.setShelfLabel(shelfLabel);
    entity.setBookCapacity(bookCapacity);
    jpaRepository.save(entity);
    logger.info("Shelf created with ID: {} for bookcase: {}", entity.getShelfId(), bookcaseId);
  }

  public Long getBookcaseIdByShelfId(Long shelfId) {
    return jpaRepository
        .findById(shelfId)
        .map(ShelfEntity::getBookcaseId)
        .orElseThrow(() -> new RuntimeException("Shelf not found with ID: " + shelfId));
  }

  @Override
  public List<Shelf> findByBookcaseId(Long bookCaseId) {
    return jpaRepository.findByBookcaseId(bookCaseId).stream()
        .map(
            shelfEntity ->
                shelfMapper.toDomainFromEntity(
                    shelfEntity, bookAccessPort.getBookIdsByShelfId(shelfEntity.getShelfId())))
        .toList();
  }

  @Override
  public List<Shelf> findShelfSummariesByBookcaseId(Long bookcaseId) {
    return jpaRepository.findByBookcaseId(bookcaseId).stream()
        .map(
            shelfEntity ->
                shelfMapper.toDomainFromEntity(
                    shelfEntity, bookAccessPort.getBookIdsByShelfId(shelfEntity.getShelfId())))
        .toList();
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

  @Override
  public List<Shelf> getShelfShelfOptionResponse(Long bookcaseId) {
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
