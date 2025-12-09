package com.penrose.bibby.library.author.core.domain;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface AuthorRepository {

    Set<Author> findAuthorsByBookId(Long id);

    /**
     * Retrieves an Author by its unique identifier.
     *
     * <p><strong>Domain Meaning:</strong><br>
     * This method looks up an Author in the persistence store using its ID,
     * returning the corresponding domain Author if found.
     *
     * <p><strong>Contract:</strong>
     * <ul>
     *   <li>Returns an {@code Optional} containing the Author if found.</li>
     *   <li>Returns {@code Optional.empty()} if no Author exists with the given ID.</li>
     *   <li>Never returns {@code null}.</li>
     *   <li>Always returns a domain-level {@code Author} object, not an entity.</li>
     * </ul>
     *
     * @param id the unique identifier of the Author to retrieve; must not be {@code null}.
     * @return an {@code Optional} containing the domain {@code Author} if found,
     *         or {@code Optional.empty()} if not found
     */
    Optional<Author> findAuthorById(Long id);

    /**
     Creates a new Author using the provided first and last name, persists it, and returns the resulting domain Author.
     *
     * @param authorFirstName the author's given name
     * @param authorLastName the author's family name
     * @return the newly created Author domain object, including its generated ID
     */
    Author createAuthor(String authorFirstName, String authorLastName);

    void updateAuthor(Author author);

    Optional<Author> findByFirstNameAndLastName(String firstName, String lastName);
}
