package com.penrose.bibby.library.cataloging.book.contracts.adapters;

import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;
import com.penrose.bibby.library.cataloging.author.contracts.ports.inbound.AuthorFacade;
import com.penrose.bibby.library.cataloging.book.contracts.ports.outbound.AuthorAccessPort;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorName;
import com.penrose.bibby.library.cataloging.book.core.domain.AuthorRef;
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
