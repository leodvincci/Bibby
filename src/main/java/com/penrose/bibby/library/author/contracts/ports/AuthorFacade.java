package com.penrose.bibby.library.author.contracts;

import org.springframework.stereotype.Component;

import java.util.Set;
@Component
public interface AuthorFacade {

    Set<AuthorDTO> findByBookId(Long id);

    AuthorDTO findOrCreateAuthor(String namePart, String namePart1);

    void updateAuthor(AuthorDTO authorDTO);
}
