package com.penrose.bibby.library.author.core.contracts;


import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;

import java.util.Optional;
import java.util.Set;

public record AuthorDTO (Long id, String firstName, String lastName) {


    public static Set<AuthorDTO> toDTOSet(Set<AuthorEntity> authorEntities) {
        return authorEntities.stream()
                .map(authorEntity -> new AuthorDTO(
                        authorEntity.getAuthorId(),
                        authorEntity.getFirstName(),
                        authorEntity.getLastName()
                ))
                .collect(java.util.stream.Collectors.toSet());
    }

    public static Optional<AuthorDTO> toDTOfromEntity(AuthorEntity authorEntity) {
        return Optional.of(new AuthorDTO(
                authorEntity.getAuthorId(),
                authorEntity.getFirstName(),
                authorEntity.getLastName()
        ));
    }

    public static AuthorEntity AuthorDTOtoEntity(AuthorDTO authorDTO) {
        AuthorEntity authorEntity = new AuthorEntity();
        authorEntity.setAuthorId(authorDTO.id());
        authorEntity.setFirstName(authorDTO.firstName());
        authorEntity.setLastName(authorDTO.lastName());
        return authorEntity;
    }
}
