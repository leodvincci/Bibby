package com.penrose.bibby.library.stacks.bookcase.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BookcaseRepositoryImpl implements BookcaseRepository {
  BookcaseJpaRepository bookcaseJpaRepository;

  public BookcaseRepositoryImpl(BookcaseJpaRepository bookcaseJpaRepository) {
    this.bookcaseJpaRepository = bookcaseJpaRepository;
  }

  @Override
  public BookcaseEntity findBookcaseEntityByBookcaseLabel(String s) {
    return bookcaseJpaRepository.findBookcaseEntityByBookcaseLabel(s);
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
}
