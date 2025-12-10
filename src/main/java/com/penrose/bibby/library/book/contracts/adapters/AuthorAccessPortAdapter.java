package com.penrose.bibby.library.book.contracts.adapters;

import com.penrose.bibby.library.author.contracts.AuthorDTO;
import com.penrose.bibby.library.author.contracts.ports.AuthorFacade;
import com.penrose.bibby.library.book.core.domain.AuthorName;
import com.penrose.bibby.library.book.core.domain.AuthorRef;
import com.penrose.bibby.library.book.contracts.ports.outbound.AuthorAccessPort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AuthorAccessPortAdapter implements AuthorAccessPort {
    private final AuthorFacade authorFacade;

    public AuthorAccessPortAdapter(AuthorFacade authorFacade) {
        this.authorFacade = authorFacade;
    }


    @Override
    public AuthorRef findOrCreateAuthor(String namePart, String namePart1) {
        AuthorDTO authorDTO = authorFacade.findOrCreateAuthor(namePart, namePart1);
        return new AuthorRef(authorDTO.id(), new AuthorName(authorDTO.firstName(), authorDTO.lastName()));
    }

    @Override
    public Set<AuthorDTO> findByBookId(Long id) {
        return authorFacade.findByBookId(id);
    }
}
