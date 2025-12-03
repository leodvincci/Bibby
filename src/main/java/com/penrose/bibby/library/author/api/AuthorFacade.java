package com.penrose.bibby.library.author.api;

import com.penrose.bibby.library.author.domain.Author;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;

public interface AuthorFacade {
    Author getOrCreateAuthorEntity(Author author);
}
