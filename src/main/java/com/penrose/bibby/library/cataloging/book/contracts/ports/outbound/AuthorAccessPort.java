package com.penrose.bibby.library.cataloging.book.contracts.ports.outbound;

import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;

import java.util.Set;

public interface AuthorAccessPort {
    AuthorRef findOrCreateAuthor(String namePart, String namePart1);

    Set<AuthorDTO> findByBookId(Long id);
}
