package com.penrose.bibby.library.author.contracts;

import com.penrose.bibby.library.author.domain.Author;

import java.util.Set;

public interface AuthorFacade {

    Set<AuthorDTO> findByBookId(Long id);

}
