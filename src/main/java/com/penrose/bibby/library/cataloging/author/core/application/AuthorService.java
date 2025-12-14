package com.penrose.bibby.library.cataloging.author.core.application;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.Set;
import com.penrose.bibby.library.cataloging.author.core.domain.Author;
import com.penrose.bibby.library.cataloging.author.core.domain.AuthorRepository;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    /**
     * Retrieves all Authors associated with the given Book.
     *
     * <p><strong>Domain Meaning:</strong><br>
     * A Book may have zero, one, or multiple Authors. This method resolves that
     * relationship by querying the underlying persistence store for all Author
     * records linked to the specified Book ID.
     *
     * <p><strong>Contract:</strong>
     * <ul>
     *   <li>Returns an immutable empty Set if the Book has no associated Authors.</li>
     *   <li>Never returns {@code null}.</li>
     *   <li>Always returns domain-level {@code Author} objects, not entities.</li>
     *   <li>If the Book ID does not exist, the result is also an empty Set.</li>
     * </ul>
     *
     * @param id the unique identifier of the Book whose Authors should be retrieved;
     *           must not be {@code null}.
     * @return a Set of domain {@code Author} objects associated with the Book,
     *         or an empty Set if none exist
     */
    public Set<Author> findAuthorsByBookId(Long id){
        return authorRepository.findAuthorsByBookId(id);
    }

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
   public Optional<Author> findAuthorById(Long id){
       return authorRepository.findAuthorById(id);
   }

    /**
     Creates a new Author using the provided first and last name, persists it, and returns the resulting domain Author.
     */
   public void createAuthor(String authorFirstName, String authorLastName){
         authorRepository.createAuthor(authorFirstName,authorLastName);
   }


   /**    * Updates the details of an existing Author in the system.
     *
     * <p>This operation takes a domain-level {@code Author} object containing
     * updated information, and persists those changes to the underlying
     * persistence store.
     *
     * <p><strong>Contract:</strong>
     * <ul>
     *   <li>The provided {@code Author} must already exist in the system.</li>
     *   <li>If the Author does not exist, an exception may be thrown.</li>
     *   <li>No value is returned; the operation is void.</li>
     * </ul>
     *
     * @param author the domain {@code Author} object with updated details;
     *               must not be {@code null} and must represent an existing Author
     */
   public void updateAuthor(Author author){
        authorRepository.updateAuthor(author);
   }

//   /**
//    * Finds an existing Author by first and last name, or creates a new one if none exists.
//    *
//    * @param authorFirstName the author's given name
//    * @param authorLastName the author's family name
//    * @return the existing or newly created Author domain object
//    */
//   public Author findOrCreateAuthor(String authorFirstName, String authorLastName){
//        Optional<Author> existingAuthor = findAuthorByName(authorFirstName,authorLastName);
//        return existingAuthor.orElseGet(()-> createAuthor(authorFirstName,authorLastName));
//   }

    public Optional<Author> findAuthorByName(String firstName, String lastName){
        return authorRepository.getByFirstNameAndLastName(firstName, lastName);
    }
}
