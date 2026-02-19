package com.penrose.bibby.library.cataloging.book.core.port.outbound;

import com.penrose.bibby.library.cataloging.author.api.dtos.AuthorDTO;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
import java.util.Set;

public interface AuthorAccessPort {
  AuthorRef findOrCreateAuthor(String namePart, String namePart1);

  Set<AuthorDTO> findByBookId(Long id);
}
