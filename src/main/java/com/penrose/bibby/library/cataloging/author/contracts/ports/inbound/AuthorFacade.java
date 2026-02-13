package com.penrose.bibby.library.cataloging.author.contracts.ports.inbound;

import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.infrastructure.entity.AuthorEntity;
import java.util.List;
import java.util.Set;

public interface AuthorFacade {

  Set<AuthorDTO> findByBookId(Long id);

  /**
   * Finds an existing Author by first and last name, or creates a new one if none exists.
   *
   * @param namePart the author's first name
   * @param namePart1 the author's last name
   * @return the found or newly created AuthorDTO
   */
  AuthorDTO findOrCreateAuthor(String namePart, String namePart1);

  void registerAuthor(AuthorDTO authorDTO);

  void updateAuthor(AuthorDTO authorDTO);

  void saveAllAuthors(List<AuthorDTO> authors);

  AuthorDTO saveAuthor(AuthorDTO authorDTO);

  AuthorDTO findById(Long authorId);

  //    List<AuthorDTO> createAuthorsIfNotExist(List<String> authors);

  Set<AuthorEntity> getAuthorsById(List<String> authors);

  /**
   * Checks if an Author exists with the given first and last name.
   *
   * @param firstName the author's first name
   * @param lastName the author's last name
   * @return true if an Author with the given names exists, false otherwise
   */
  boolean authorExistFirstNameLastName(String firstName, String lastName);

  List<AuthorDTO> getAllAuthorsByName(String firstName, String lastName);

  AuthorDTO getAuthorById(Long authId);
}
