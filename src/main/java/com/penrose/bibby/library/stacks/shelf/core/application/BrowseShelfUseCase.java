package com.penrose.bibby.library.stacks.shelf.core.application;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.cataloging.book.contracts.dtos.BriefBibliographicRecord;
import com.penrose.bibby.library.cataloging.book.contracts.ports.inbound.BookFacade;

import java.util.List;

public class BrowseShelfUseCase {

    BriefBibliographicRecord briefBibliographicRecord;
    BookFacade bookFacade;

    public BrowseShelfUseCase(BookFacade bookFacade) {
        this.bookFacade = bookFacade;
    }

    public List<BriefBibliographicRecord> browseShelf(ShelfId shelfId){
        return bookFacade.getBriefBibliographicRecordsByShelfId(shelfId.shelfId());
    }



}
