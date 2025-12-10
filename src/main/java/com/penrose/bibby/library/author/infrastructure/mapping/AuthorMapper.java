package com.penrose.bibby.library.author.infrastructure.mapping;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.author.core.domain.Author;
import com.penrose.bibby.library.author.core.domain.AuthorId;
import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;

import java.util.List;
import java.util.Set;

public class AuthorMapper {


    public static AuthorEntity toEntity(Author author){
        String firstName = author.getFirstName();
        String lastName = author.getLastName();
        AuthorEntity authorEntity = new AuthorEntity();
        authorEntity.setFirstName(firstName);
        authorEntity.setLastName(lastName);
        return authorEntity;
    }

    public static Author toDomain(Long id, String firstName, String lastName){
        AuthorId authorId = new AuthorId(id);
        return new Author(authorId,firstName,lastName);
    }

    public static Set<Author> toDomainSet(Set<AuthorEntity> authorEntities){
        Set<Author> authors = new java.util.HashSet<>();
        for(AuthorEntity authorEntity : authorEntities){
            authors.add(toDomain(authorEntity.getAuthorId(), authorEntity.getFirstName(), authorEntity.getLastName()));
        }
        return authors;
    }

    public static Set<AuthorDTO> toDTOSet(Set<Author> authors) {
        Set<AuthorDTO> authorDTOs = new java.util.HashSet<>();
        for (Author author : authors) {
            AuthorDTO authorDTO = new AuthorDTO(
                    author.getAuthorId().id(),
                    author.getFirstName(),
                    author.getLastName()
            );
            authorDTOs.add(authorDTO);
        }
        return authorDTOs;
    }

    public static AuthorDTO toDTO(Author author) {
        return new AuthorDTO(
                author.getAuthorId().id(),
                author.getFirstName(),
                author.getLastName()
        );
    }

    public static List<AuthorEntity> toEntityList(List<AuthorDTO> authors) {
        List<AuthorEntity> authorEntities = new java.util.ArrayList<>();
        for (AuthorDTO authorDTO : authors) {
            AuthorEntity authorEntity = new AuthorEntity();
            authorEntity.setFirstName(authorDTO.firstName());
            authorEntity.setLastName(authorDTO.lastName());
            authorEntities.add(authorEntity);
        }
        return authorEntities;
    }
}
