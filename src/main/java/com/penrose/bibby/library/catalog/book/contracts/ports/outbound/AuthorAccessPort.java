package com.penrose.bibby.library.catalog.book.contracts.ports.outbound;

import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;
import com.penrose.bibby.library.catalog.book.core.domain.AuthorRef;

import java.util.Set;

public interface AuthorAccessPort {
    AuthorRef findOrCreateAuthor(String namePart, String namePart1);

    Set<AuthorDTO> findByBookId(Long id);
}
