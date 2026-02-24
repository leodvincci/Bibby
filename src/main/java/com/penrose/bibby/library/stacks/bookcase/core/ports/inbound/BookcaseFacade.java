package com.penrose.bibby.library.stacks.bookcase.core.ports.inbound;

import com.penrose.bibby.library.stacks.bookcase.api.CreateBookcaseResult;
import com.penrose.bibby.library.stacks.bookcase.api.dtos.BookcaseDTO;
import com.penrose.bibby.library.stacks.bookcase.infrastructure.entity.BookcaseEntity;
import java.util.List;
import java.util.Optional;

public interface BookcaseFacade {

  /**
   * Finds a bookcase by its unique identifier and returns it as a DTO.
   *
   * @param aLong the unique identifier of the bookcase to find.
   * @return an {@link Optional} containing the {@link BookcaseDTO} if found, or empty if not.
   */
  Optional<BookcaseDTO> findBookCaseById(Long aLong);

  /**
   * Retrieves a list of all bookcases in the library.
   *
   * @return A list of BookcaseDTO objects representing all bookcases.
   */
  List<BookcaseDTO> getAllBookcases();

  /**
   * Creates a new bookcase with the specified configuration and shelving layout.
   *
   * @param userId the ID of the user creating the bookcase.
   * @param bookcaseLabel a descriptive label for the bookcase.
   * @param bookcaseZone the zone where the bookcase is located.
   * @param bookcaseZoneIndex the index position of the bookcase within its zone.
   * @param shelfCount the number of shelves to create in the bookcase.
   * @param bookCapacity the book capacity per shelf.
   * @param location the physical location of the bookcase.
   * @return a {@link CreateBookcaseResult} indicating the outcome of the creation.
   */
  CreateBookcaseResult createNewBookCase(
      Long userId,
      String bookcaseLabel,
      String bookcaseZone,
      String bookcaseZoneIndex,
      int shelfCount,
      int bookCapacity,
      String location);

  /**
   * Retrieves all distinct bookcase locations in the library.
   *
   * @return a list of location strings.
   */
  List<String> getAllBookcaseLocations();

  /**
   * Finds a bookcase entity by its unique identifier.
   *
   * @param bookcaseId the unique identifier of the bookcase.
   * @return an {@link Optional} containing the {@link BookcaseEntity} if found, or empty if not.
   */
  Optional<BookcaseEntity> findById(Long bookcaseId);

  /**
   * Retrieves all bookcases at the specified location.
   *
   * @param location the physical location to filter by.
   * @return a list of {@link BookcaseDTO} objects at the given location.
   */
  List<BookcaseDTO> getAllBookcasesByLocation(String location);

  /**
   * Retrieves all bookcases owned by the specified user.
   *
   * @param appUserId the unique identifier of the user.
   * @return a list of {@link BookcaseDTO} objects belonging to the user.
   */
  List<BookcaseDTO> getAllBookcasesByUserId(Long appUserId);

  /**
   * Deletes a bookcase by its unique identifier.
   *
   * @param bookcaseId the unique identifier of the bookcase to delete.
   */
  void deleteBookcase(Long bookcaseId);
}
