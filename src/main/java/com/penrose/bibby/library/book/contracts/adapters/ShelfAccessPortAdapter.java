package com.penrose.bibby.library.book.contracts.adapters;

import com.penrose.bibby.library.book.contracts.ports.outbound.ShelfAccessPort;
import com.penrose.bibby.library.shelf.contracts.dtos.ShelfDTO;
import com.penrose.bibby.library.shelf.contracts.ports.inbound.ShelfFacade;
import com.penrose.bibby.library.shelf.core.application.ShelfService;
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
