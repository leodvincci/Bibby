package com.penrose.bibby.library.cataloging.author.infrastructure.repository;

import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.core.domain.Author;
import com.penrose.bibby.library.cataloging.author.core.domain.AuthorRepository;
import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.cataloging.author.infrastructure.mapping.AuthorMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class AuthorRepositoryImpl implements AuthorRepository {

    private final AuthorJpaRepository authorJpaRepository;
    Logger logger = org.slf4j.LoggerFactory.getLogger(AuthorRepositoryImpl.class);

    public AuthorRepositoryImpl(AuthorJpaRepository authorJpaRepository) {
        this.authorJpaRepository = authorJpaRepository;
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
    @Override
    public Set<Author> findAuthorsByBookId(Long id) {
        Set<AuthorEntity> authorEntities = authorJpaRepository.findByBooks_BookId(id);
        if (!authorEntities.isEmpty()) {
            return AuthorMapper.toDomainSet(authorEntities);
    }
        return Set.of();
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
    @Override
    public Optional<Author> findAuthorById(Long id) {
        AuthorEntity authorEntity = authorJpaRepository.findById(id).orElse(null);
        return Optional.of(AuthorMapper.toDomain(id, authorEntity.getFirstName(), authorEntity.getLastName()));
    }

    /**
     * Creates and persists a new Author in the system.
     *
     * <p>This operation constructs a new Author from the provided first and last
     * name, saves it to the underlying persistence store, and returns the
     * newly-created domain Author.</p>
     *
     * @param authorFirstName the author's given name
     * @param authorLastName the author's family name
     * @return the newly created Author domain object, including its generated ID
     */


    @Override
    public Optional<Author> createAuthor(String authorFirstName, String authorLastName) {
        AuthorEntity authorEntity = new AuthorEntity(authorFirstName, authorLastName);
        logger.info("Creating author: {} {}", authorFirstName, authorLastName);
        authorJpaRepository.save(authorEntity);
        logger.info("Created author with ID: {}", authorEntity.getAuthorId());
        //todo remove mapper and return entity letting client map to domain
        Optional<AuthorEntity> newAuthorEntity = authorJpaRepository.getByFirstNameAndLastName(authorFirstName, authorLastName);
        return Optional.of(AuthorMapper.toDomain(newAuthorEntity.get().getAuthorId(), newAuthorEntity.get().getFirstName(), newAuthorEntity.get().getLastName()));
    }


    @Override
    public void updateAuthor(Author author) {
        AuthorEntity authorEntity = AuthorMapper.toEntity(author);
        authorJpaRepository.save(authorEntity);
    }

    @Override
    public Optional<Author> getByFirstNameAndLastName(String firstName, String lastName) {
        AuthorEntity authorEntity = authorJpaRepository.getByFirstNameAndLastName(firstName, lastName).orElse(null);
        if (authorEntity != null) {
            return Optional.of(AuthorMapper.toDomain(authorEntity.getAuthorId(), authorEntity.getFirstName(), authorEntity.getLastName()));
        }
        return Optional.empty();
    }

    @Override
    public void saveAll(List<AuthorDTO> authors) {
        List<AuthorEntity> authorEntities = AuthorMapper.toEntityList(authors);
        authorJpaRepository.saveAll(authorEntities);

    }

    @Override
    public AuthorDTO saveAuthor(Author author) {
        AuthorEntity authorEntity = new AuthorEntity();
        authorEntity.setFirstName(author.getFirstName());
        authorEntity.setLastName(author.getLastName());
        authorJpaRepository.save(authorEntity);
        logger.info("Saved author with ID: {}", authorEntity.getAuthorId());
        return AuthorMapper.toDTO(AuthorMapper.toDomain(authorEntity.getAuthorId(), authorEntity.getFirstName(), authorEntity.getLastName()));
    }

    @Override
    public Set<AuthorEntity> getAuthorsById(List<String> authors) {
        logger.info("Fetching authors by IDs: {}", authors);
        Set<AuthorEntity> authorEntities = new java.util.HashSet<>();
        for(String authorIdStr : authors){
            Long authorId = Long.parseLong(authorIdStr);
            logger.info("Fetching author with ID: {}", authorId);
            authorEntities.add(authorJpaRepository.findById(authorId).get());
        }
        return authorEntities;
    }

    @Override
    public List<AuthorDTO> findAllByFirstNameLastName(String firstName, String lastName) {
        List<AuthorEntity> authorEntities = authorJpaRepository.getAllByFirstNameAndLastName(firstName, lastName);
        return authorEntities.stream()
                .map(entity -> AuthorMapper.toDTO(AuthorMapper.toDomain(entity.getAuthorId(), entity.getFirstName(), entity.getLastName())))
                .toList();
    }

    @Override
    public boolean authorExistFirstNameLastName(String firstName, String lastName) {
        logger.info("RepoImpl: Checking existence of author: {} {}", firstName, lastName);
        return !authorJpaRepository.findByFirstNameAndLastName(firstName, lastName).isEmpty();
    }

    @Override
    public Optional<AuthorDTO> getByFirstNameAndLastNameDTO(String firstName, String lastName) {

        return Optional.empty();
    }

    @Override
    public AuthorEntity getAuthorById(Long authId) {
        return authorJpaRepository.findById(authId).get();
    }
}
