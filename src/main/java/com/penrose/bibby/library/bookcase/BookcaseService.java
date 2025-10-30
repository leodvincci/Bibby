package com.penrose.bibby.library.bookcase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookcaseService {

    BookcaseRepository bookcaseRepository;

    public BookcaseService(BookcaseRepository bookcaseRepository) {
        this.bookcaseRepository = bookcaseRepository;
    }

    public String createNewBookCase(BookcaseDTO bookcaseDTO){
        BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(bookcaseDTO.bookcaseLabel());
        if(bookcaseEntity !=null){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Bookcase with the label:  " + bookcaseDTO.bookcaseLabel() + " already exist");
        }
        else{
            bookcaseEntity = new BookcaseEntity(bookcaseDTO.bookcaseLabel(),bookcaseDTO.shelfCapacity());
            bookcaseRepository.save(bookcaseEntity);
//            System.out.println("Service: Telling the DB to Create New Case" + bookcaseDTO);
           return "Created New Bookcase " + bookcaseDTO.bookcaseLabel() + " with shelf capacity of " + bookcaseDTO.shelfCapacity();
        }



    }

}
