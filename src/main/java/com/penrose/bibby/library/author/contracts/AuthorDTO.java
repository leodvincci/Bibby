package com.penrose.bibby.library.author.contracts;


import com.penrose.bibby.library.author.infrastructure.entity.AuthorEntity;

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
}
