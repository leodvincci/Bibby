package com.penrose.bibby.library.book.contracts.ports.outbound;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.book.core.domain.AuthorRef;

import java.util.Set;

public interface AuthorAccessPort {
    AuthorRef findOrCreateAuthor(String namePart, String namePart1);

    Set<AuthorDTO> findByBookId(Long id);
}
