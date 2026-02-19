package com.penrose.bibby.library.cataloging.book.api.adapters;

import com.penrose.bibby.library.cataloging.author.api.dtos.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.core.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorName;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
import com.penrose.bibby.library.cataloging.book.core.port.outbound.AuthorAccessPort;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AuthorAccessPortAdapter implements AuthorAccessPort {
  private final AuthorFacade authorFacade;

  public AuthorAccessPortAdapter(AuthorFacade authorFacade) {
    this.authorFacade = authorFacade;
  }

  @Override
  public AuthorRef findOrCreateAuthor(String namePart, String namePart1) {
    AuthorDTO authorDTO = authorFacade.findOrCreateAuthor(namePart, namePart1);
    return new AuthorRef(
        authorDTO.id(), new AuthorName(authorDTO.firstName(), authorDTO.lastName()));
  }

  @Override
  public Set<AuthorDTO> findByBookId(Long id) {
    return authorFacade.findByBookId(id);
  }
}
