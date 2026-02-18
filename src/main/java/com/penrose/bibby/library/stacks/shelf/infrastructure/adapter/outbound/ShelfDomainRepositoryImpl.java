package com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound;

import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfOptionResponse;
import com.penrose.bibby.library.stacks.shelf.api.dtos.ShelfSummary;
import com.penrose.bibby.library.stacks.shelf.core.domain.ShelfDomainRepository;
import com.penrose.bibby.library.stacks.shelf.core.domain.model.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.ports.BookAccessPort;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
import org.slf4j.Logger;

import java.util.List;
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
    return shelfMapper.toDomainFromDTO(entity, bookIds);
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
    return jpaRepository
        .findByBookcaseId(bookCaseId)
        .stream()
        .map(shelfEntity -> shelfMapper.toDomainFromEntity(shelfEntity) )
        .toList();
  }

  @Override
  public List<ShelfSummary> findShelfSummariesByBookcaseId(Long bookcaseId) {
    return jpaRepository
        .findByBookcaseId(bookcaseId)
        .stream()
        .map(shelfEntity -> shelfMapper.toSummaryFromEntity(shelfEntity))
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
    if(entity == null) {
      throw new RuntimeException("Shelf not found with ID: " + shelfId);
    }
    return shelfMapper.toDomainFromEntity(entity);
  }

  @Override
  public List<ShelfOptionResponse> getShelfShelfOptionResponse(Long bookcaseId) {
    return jpaRepository.findByBookcaseId(bookcaseId)
        .stream()
        .map(shelfEntity -> {
          Long shelfId = shelfEntity.getShelfId();
          String shelfLabel = shelfEntity.getShelfLabel();
          int bookCapacity = shelfEntity.getBookCapacity();
          long bookCount = bookAccessPort.getBookIdsByShelfId(shelfId).size();
          boolean hasSpace = bookCount < bookCapacity;
          return new ShelfOptionResponse(shelfId, shelfLabel, bookCapacity, bookCount, hasSpace);
        })
        .toList();
  }

  @Override
  public List<Shelf> findAll() {
    return jpaRepository.findAll()
        .stream()
        .map(shelfEntity -> shelfMapper.toDomainFromEntity(shelfEntity))
        .toList();
  }
}
