package com.penrose.bibby.library.cataloging.book.contracts.adapters;

import com.penrose.bibby.library.cataloging.book.contracts.ports.outbound.ShelfAccessPort;
import com.penrose.bibby.library.stacks.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.stacks.shelf.contracts.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.stacks.shelf.core.application.ShelfService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ShelfAccessPortAdapter implements ShelfAccessPort {

    private final ShelfFacade shelfFacade;
    ShelfService shelfService;

    public ShelfAccessPortAdapter(ShelfFacade shelfFacade){
        this.shelfFacade = shelfFacade;
    }

    @Override
    public Optional<ShelfDTO> findShelfById(Long shelfId) {
        return shelfFacade.findShelfById(shelfId);
    }
}
