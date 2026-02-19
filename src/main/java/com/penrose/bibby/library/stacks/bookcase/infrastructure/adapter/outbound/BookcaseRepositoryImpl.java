package com.penrose.bibby.library.stacks.bookcase.infrastructure.adapter.outbound;

import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.BookcaseRepository;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.repository.BookcaseJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BookcaseRepositoryImpl implements BookcaseRepository {
  BookcaseJpaRepository bookcaseJpaRepository;
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(BookcaseRepositoryImpl.class);

  public BookcaseRepositoryImpl(BookcaseJpaRepository bookcaseJpaRepository) {
    this.bookcaseJpaRepository = bookcaseJpaRepository;
  }

  @Override
  public BookcaseEntity findBookcaseEntityByBookcaseLocation(String s) {
    return bookcaseJpaRepository.findBookcaseEntityByBookcaseLocation(s);
  }

  @Override
  public List<BookcaseEntity> findAll() {
    return bookcaseJpaRepository.findAll();
  }

  @Override
  public List<String> getAllBookCaseLocations() {
    List<String> locations = new ArrayList<>();
    List<BookcaseEntity> bookcaseEntities = bookcaseJpaRepository.findAll();
    for (BookcaseEntity entity : bookcaseEntities) {
      locations.add(entity.getBookcaseLocation());
    }
    return locations;
  }

  @Override
  public BookcaseEntity save(BookcaseEntity bookcaseEntity) {
    return bookcaseJpaRepository.save(bookcaseEntity);
  }

  @Override
  public Optional<BookcaseEntity> findById(Long id) {
    return bookcaseJpaRepository.findById(id);
  }

  @Override
  public List<BookcaseEntity> findByLocation(String location) {
    return bookcaseJpaRepository.findAllByBookcaseLocation(location);
  }

  @Override
  public List<BookcaseEntity> findByAppUserId(Long appUserId) {
    return bookcaseJpaRepository.findAllByUserId((appUserId));
  }

  @Override
  public void deleteById(Long bookcaseId) {
    bookcaseJpaRepository.deleteById(bookcaseId);
    logger.info("Deleted bookcase with Id: {}", bookcaseId);
  }
}
