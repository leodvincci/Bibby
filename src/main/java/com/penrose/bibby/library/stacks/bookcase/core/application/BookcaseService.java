package com.penrose.bibby.library.stacks.bookcase.core.application;

import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.core.application.usecases.CreateBookcaseUseCase;
import com.penrose.bibby.library.stacks.bookcase.core.application.usecases.DeleteBookcaseUseCase;
import com.penrose.bibby.library.stacks.bookcase.core.application.usecases.QueryBookcaseUseCase;
import com.penrose.bibby.library.stacks.bookcase.core.domain.model.Bookcase;
import com.penrose.bibby.library.stacks.bookcase.core.ports.inbound.BookcaseFacade;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookcaseService implements BookcaseFacade {

  private final CreateBookcaseUseCase createBookcaseUseCase;
  private final DeleteBookcaseUseCase deleteBookcaseUseCase;
  private final QueryBookcaseUseCase queryBookcaseUseCase;

  public BookcaseService(
      CreateBookcaseUseCase createBookcaseUseCase,
      DeleteBookcaseUseCase deleteBookcaseUseCase,
      QueryBookcaseUseCase queryBookcaseUseCase) {
    this.createBookcaseUseCase = createBookcaseUseCase;
    this.deleteBookcaseUseCase = deleteBookcaseUseCase;
    this.queryBookcaseUseCase = queryBookcaseUseCase;
  }

  @Override
  public CreateBookcaseResult createNewBookCase(
      Long userId,
      String label,
      String bookcaseZone,
      String bookcaseZoneIndex,
      int shelfCapacity,
      int bookCapacity,
      String location) {
    return createBookcaseUseCase.createNewBookCase(
        userId, label, bookcaseZone, bookcaseZoneIndex, shelfCapacity, bookCapacity, location);
  }

  @Override
  public Bookcase findBookCaseById(Long bookcaseId) {
    return queryBookcaseUseCase.findBookCaseById(bookcaseId);
  }

  @Override
  public List<Bookcase> getAllBookcases() {
    return queryBookcaseUseCase.getAllBookcases();
  }

  @Override
  public List<String> getAllBookcaseLocations() {
    return queryBookcaseUseCase.getAllBookcaseLocations();
  }

  @Override
  public Bookcase findById(Long bookcaseId) {
    return queryBookcaseUseCase.findById(bookcaseId);
  }

  @Override
  public List<Bookcase> getAllBookcasesByLocation(String location) {
    return queryBookcaseUseCase.getAllBookcasesByLocation(location);
  }

  @Override
  public List<Bookcase> getAllBookcasesByUserId(Long appUserId) {
    return queryBookcaseUseCase.getAllBookcasesByUserId(appUserId);
  }

  @Override
  public void deleteBookcase(Long bookcaseId) {
    deleteBookcaseUseCase.deleteBookcase(bookcaseId);
  }
}
