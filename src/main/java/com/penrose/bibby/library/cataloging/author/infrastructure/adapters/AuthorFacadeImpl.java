package com.penrose.bibby.library.cataloging.author.infrastructure.adapters;

import com.penrose.bibby.library.cataloging.author.api.dtos.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.core.domain.Author;
import com.penrose.bibby.library.cataloging.author.core.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.author.core.ports.outbound.AuthorRepository;
import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.cataloging.author.infrastructure.mapping.AuthorMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class AuthorFacadeImpl implements AuthorFacade {

  //    final AuthorService authorService;
  private final AuthorRepository authorRepository;
  Logger log = org.slf4j.LoggerFactory.getLogger(AuthorFacadeImpl.class);

  public AuthorFacadeImpl(AuthorRepository authorRepository, AuthorMapper authorMapper) {
    //        this.authorService = authorService;
    this.authorRepository = authorRepository;
  }

  @Override
  public Set<AuthorDTO> findByBookId(Long id) {
    //        Set<Author> authors = authorService.findAuthorsByBookId(id);
    Set<Author> authors = authorRepository.findAuthorsByBookId(id);
    return AuthorMapper.toDTOSet(authors);
  }

  @Override
  public void updateAuthor(AuthorDTO authorDTO) {
    Author author =
        AuthorMapper.toDomain(authorDTO.id(), authorDTO.firstName(), authorDTO.lastName());
    //        authorService.updateAuthor(author);
    authorRepository.updateAuthor(author);
  }

  @Override
  public void saveAllAuthors(List<AuthorDTO> authors) {
    authorRepository.saveAll(authors);
  }

  @Override
  public AuthorDTO saveAuthor(AuthorDTO authorDTO) {
    Author author =
        AuthorMapper.toDomain(authorDTO.id(), authorDTO.firstName(), authorDTO.lastName());
    log.info("Saving author: {} {}", authorDTO.firstName(), authorDTO.lastName());
    return authorRepository.saveAuthor(author);
  }

  @Override
  public AuthorDTO findById(Long authorId) {
    return authorRepository.findAuthorById(authorId).map(AuthorMapper::toDTO).orElse(null);
  }

  //    @Override
  //    public List<AuthorDTO> createAuthorsIfNotExist(List<String> authors) {
  //        List<AuthorDTO> createdAuthors = new ArrayList<>();
  //        for(String author : authors){
  //            String[] nameParts = author.split(" ");
  //            String firstName = nameParts[0];
  //            String lastName = nameParts.length > 1 ? nameParts[1] : "";
  //            Optional<AuthorDTO> existingAuthor =
  // authorRepository.getByFirstNameAndLastNameDTO(firstName, lastName);
  //            createdAuthors.add(existingAuthor.get());
  //            if(existingAuthor.isEmpty()){
  //                authorRepository.createAuthor(firstName, lastName);
  //            }
  //        }
  //    }

  @Override
  public AuthorDTO findOrCreateAuthor(String namePart, String namePart1) {
    Optional<Author> author = authorRepository.getByFirstNameAndLastName(namePart, namePart1);
    if (author.isEmpty()) {
      return AuthorMapper.toDTO(authorRepository.createAuthor(namePart, namePart1).get());
    }
    return AuthorMapper.toDTO(author.get());
  }

  @Override
  public void registerAuthor(AuthorDTO authorDTO) {
    authorRepository.createAuthor(authorDTO.firstName(), authorDTO.lastName());
  }

  @Override
  public Set<AuthorEntity> getAuthorsById(List<String> authors) {

    return authorRepository.getAuthorsById(authors);
  }

  /**
   * Checks if an Author exists with the given first and last name.
   *
   * @param firstName the author's first name
   * @param lastName the author's last name
   * @return true if an Author with the given names exists, false otherwise
   */
  @Override
  public boolean authorExistFirstNameLastName(String firstName, String lastName) {
    log.info("Checking existence of author: {} {}", firstName, lastName);
    return authorRepository.authorExistFirstNameLastName(firstName, lastName);
  }

  @Override
  public List<AuthorDTO> getAllAuthorsByName(String firstName, String lastName) {
    return authorRepository.findAllByFirstNameLastName(firstName, lastName);
  }

  @Override
  public AuthorDTO getAuthorById(Long authId) {
    AuthorEntity authorEntity = authorRepository.getAuthorById(authId);
    return AuthorMapper.toDTOFromEntity(authorEntity);
  }
}
