package com.penrose.bibby.library.bookcase;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class BookcaseService {

    BookcaseRepository bookcaseRepository;

    public BookcaseService(BookcaseRepository bookcaseRepository) {
        this.bookcaseRepository = bookcaseRepository;
    }

    public void  createNewBookCase(BookcaseDTO bookcaseDTO){
        //make sure does not already exist
        BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(bookcaseDTO.bookcaseLabel());
        if(bookcaseEntity == null){
            bookcaseEntity = new BookcaseEntity(bookcaseDTO.bookcaseLabel());
            bookcaseRepository.save(bookcaseEntity);
            System.out.println("Service: Telling the DB to Create New Case" + bookcaseDTO);
            System.out.println("Saved");
        }else{
            System.out.println("Service: Bookcase already exist");
        }



    }

}
