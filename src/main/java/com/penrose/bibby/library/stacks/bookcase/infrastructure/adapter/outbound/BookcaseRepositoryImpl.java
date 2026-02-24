package com.penrose.bibby.library.stacks.bookcase.infrastructure.adapter.outbound;

import com.penrose.bibby.library.stacks.bookcase.core.domain.BookcaseMapper;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.BookcaseRepository;
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
  public Bookcase findBookcaseByBookcaseLocation(String bookcaseLocation) {
    if (bookcaseJpaRepository.findBookcaseEntityByBookcaseLocation(bookcaseLocation) != null) {;
      return BookcaseMapper.toDomain(
          bookcaseJpaRepository.findBookcaseEntityByBookcaseLocation(bookcaseLocation));
    } else {
      logger.warn("No bookcase found at location: {}", bookcaseLocation);
      return null;
    }
  }

  @Override
  public List<Bookcase> findAll() {
    return bookcaseJpaRepository.findAll().stream().map(BookcaseMapper::toDomain).toList();
  }

  @Override
  public List<String> getAllBookCaseLocations() {
    List<String> locations = new ArrayList<>();
    List<Bookcase> bookcaseEntities =
        bookcaseJpaRepository.findAll().stream().map(BookcaseMapper::toDomain).toList();
    for (Bookcase entity : bookcaseEntities) {
      locations.add(entity.getBookcaseLocation());
    }
    return locations;
  }

  @Override
  public Bookcase save(Bookcase bookcase) {
    Bookcase savedBookcase = BookcaseMapper.toDomain(bookcaseJpaRepository.save(BookcaseMapper.toEntity(bookcase)));
    logger.info("Bookcase saved successfully with ID: {}", savedBookcase.getBookcaseId());
    return savedBookcase;
  }

  @Override
  public Bookcase findById(Long id) {
    return Optional.ofNullable(bookcaseJpaRepository.findById(id).orElse(null))
        .map(BookcaseMapper::toDomain)
        .orElse(null);
  }

  @Override
  public List<Bookcase> findByLocation(String location) {
    return bookcaseJpaRepository.findAllByBookcaseLocation(location).stream()
        .map(BookcaseMapper::toDomain)
        .toList();
  }

  @Override
  public List<Bookcase> findByAppUserId(Long appUserId) {
    return bookcaseJpaRepository.findAllByUserId(appUserId).stream()
        .map(BookcaseMapper::toDomain)
        .toList();
  }

  @Override
  public void deleteById(Long bookcaseId) {
    bookcaseJpaRepository.deleteById(bookcaseId);
    logger.info("Deleted bookcase with Id: {}", bookcaseId);
  }
}
