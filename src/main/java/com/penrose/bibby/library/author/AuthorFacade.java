package com.penrose.bibby.library.author;

public interface AuthorFacade {
    AuthorEntity getOrCreateAuthorEntity(Author author);
}
