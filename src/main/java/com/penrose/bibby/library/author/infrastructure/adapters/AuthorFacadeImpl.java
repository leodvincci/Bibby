package com.penrose.bibby.library.author.infrastructure.adapters;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.author.core.application.AuthorService;
import com.penrose.bibby.library.author.core.domain.Author;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.author.infrastructure.mapping.AuthorMapper;

import java.util.Set;

public class AuthorFacadeImpl implements AuthorFacade {

    final AuthorService authorService;


    public AuthorFacadeImpl(AuthorService authorService) {
        this.authorService = authorService;
    }

    @Override
    public Set<AuthorDTO> findByBookId(Long id) {
        Set<Author> authors = authorService.findAuthorsByBookId(id);
        return AuthorMapper.toDTOSet(authors);
    }

    @Override
    public AuthorDTO findOrCreateAuthor(String namePart, String namePart1) {
         Author author = authorService.findOrCreateAuthor(namePart, namePart1);
        return AuthorMapper.toDTO(author);
    }

    @Override
    public void updateAuthor(AuthorDTO authorDTO) {
        Author author = AuthorMapper.toDomain(authorDTO.id(), authorDTO.firstName(), authorDTO.lastName());
        authorService.updateAuthor(author);
    }
}
