package com.penrose.bibby.library.cataloging.author.core.domain;

import com.penrose.bibby.library.cataloging.author.api.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository {

  Set<Author> findAuthorsByBookId(Long id);

  /**
   * Retrieves an Author by its unique identifier.
   *
   * <p><strong>Domain Meaning:</strong><br>
   * This method looks up an Author in the persistence store using its ID, returning the
   * corresponding domain Author if found.
   *
   * <p><strong>Contract:</strong>
   *
   * <ul>
   *   <li>Returns an {@code Optional} containing the Author if found.
   *   <li>Returns {@code Optional.empty()} if no Author exists with the given ID.
   *   <li>Never returns {@code null}.
   *   <li>Always returns a domain-level {@code Author} object, not an entity.
   * </ul>
   *
   * @param id the unique identifier of the Author to retrieve; must not be {@code null}.
   * @return an {@code Optional} containing the domain {@code Author} if found, or {@code
   *     Optional.empty()} if not found
   */
  Optional<Author> findAuthorById(Long id);

  /**
   * Creates a new Author using the provided first and last name, persists it, and returns the
   * resulting domain Author.
   *
   * @param authorFirstName the author's given name
   * @param authorLastName the author's family name
   * @return the newly created Author domain object, including its generated ID
   */
  Optional<Author> createAuthor(String authorFirstName, String authorLastName);

  void updateAuthor(Author author);

  Optional<Author> getByFirstNameAndLastName(String firstName, String lastName);

  void saveAll(List<AuthorDTO> authors);

  AuthorDTO saveAuthor(Author author);

  Set<AuthorEntity> getAuthorsById(List<String> authors);

  List<AuthorDTO> findAllByFirstNameLastName(String firstName, String lastName);

  boolean authorExistFirstNameLastName(String firstName, String lastName);

  Optional<AuthorDTO> getByFirstNameAndLastNameDTO(String firstName, String lastName);

  AuthorEntity getAuthorById(Long authId);
}
