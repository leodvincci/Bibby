package com.penrose.bibby.library.author.infrastructure.adapters;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.author.core.application.AuthorService;
import com.penrose.bibby.library.author.core.domain.Author;
import com.penrose.bibby.library.author.core.domain.AuthorRepository;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;
import com.penrose.bibby.library.author.infrastructure.mapping.AuthorMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
@Component
public class AuthorFacadeImpl implements AuthorFacade {

    final AuthorService authorService;
    private final AuthorRepository authorRepository;


    public AuthorFacadeImpl(AuthorService authorService, AuthorRepository authorRepository) {
        this.authorService = authorService;
        this.authorRepository = authorRepository;
    }

    @Override
    public Set<AuthorDTO> findByBookId(Long id) {
        Set<Author> authors = authorService.findAuthorsByBookId(id);
        return AuthorMapper.toDTOSet(authors);
    }

    @Override
    public AuthorDTO findOrCreateAuthor(String namePart, String namePart1) {
        Optional<Author> author = authorRepository.findByFirstNameAndLastName(namePart, namePart1);
        if(author.isEmpty()){
            return AuthorMapper.toDTO(authorRepository.createAuthor(namePart, namePart1).get());

        }
        return AuthorMapper.toDTO(author.get());
    }

    @Override
    public void updateAuthor(AuthorDTO authorDTO) {
        Author author = AuthorMapper.toDomain(authorDTO.id(), authorDTO.firstName(), authorDTO.lastName());
        authorService.updateAuthor(author);
    }

    @Override
    public void saveAllAuthors(List<AuthorDTO> authors) {
        authorRepository.saveAll(authors);
    }

    @Override

    public AuthorDTO saveAuthor(AuthorDTO authorDTO) {
        Author author = AuthorMapper.toDomain(authorDTO.id(), authorDTO.firstName(), authorDTO.lastName());
        return authorRepository.saveAuthor(author);

    }

    @Override
    public AuthorEntity findById(Long authorId) {
        return authorRepository.findAuthorById(authorId).map(AuthorMapper::toEntity).orElse(null);
    }
}
