package com.penrose.bibby.library.author.contracts.ports;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import org.springframework.stereotype.Component;

import java.util.Set;
@Component
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

    void updateAuthor(AuthorDTO authorDTO);
}
