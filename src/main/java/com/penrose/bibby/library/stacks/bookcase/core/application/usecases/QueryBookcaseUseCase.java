package com.penrose.bibby.library.stacks.bookcase.core.application.usecases;

import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.core.ports.outbound.BookcaseRepository;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class QueryBookcaseUseCase {

  private final BookcaseRepository bookcaseRepository;

  public QueryBookcaseUseCase(BookcaseRepository bookcaseRepository) {
    this.bookcaseRepository = bookcaseRepository;
  }

  public Bookcase findBookCaseById(Long id) {
      return bookcaseRepository.findById(id);
  }

  public List<Bookcase> getAllBookcases() {
      return bookcaseRepository.findAll();
  }

  public List<String> getAllBookcaseLocations() {
    return bookcaseRepository.getAllBookCaseLocations();
  }

  public Bookcase findById(Long bookcaseId) {
    return bookcaseRepository.findById(bookcaseId);
  }

  public List<Bookcase> getAllBookcasesByLocation(String location) {
    return bookcaseRepository.findByLocation(location);
  }

  public List<Bookcase> getAllBookcasesByUserId(Long appUserId) {
    return bookcaseRepository.findByAppUserId(appUserId);
  }
}
