package com.penrose.bibby.library.author.contracts;

import com.penrose.bibby.library.author.domain.Author;

public interface AuthorFacade {
    Author getOrCreateAuthorEntity(Author author);
}
