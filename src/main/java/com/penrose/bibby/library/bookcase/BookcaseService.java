package com.penrose.bibby.library.bookcase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookcaseService {
    private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);

    BookcaseRepository bookcaseRepository;
    ResponseStatusException existingRecordError = new ResponseStatusException(HttpStatus.CONFLICT,"Bookcase with the label already exist");
    public BookcaseService(BookcaseRepository bookcaseRepository) {
        this.bookcaseRepository = bookcaseRepository;
    }

    public String createNewBookCase(BookcaseDTO bookcaseDTO){
        BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(bookcaseDTO.bookcaseLabel());
        if(bookcaseEntity !=null){
            log.error("Failed to save Record - Record already exist",existingRecordError);
            throw existingRecordError;

        }
        else{
            bookcaseEntity = new BookcaseEntity(bookcaseDTO.bookcaseLabel(),bookcaseDTO.shelfCapacity());
            bookcaseRepository.save(bookcaseEntity);
            log.info("Created new bookcase: {}",bookcaseEntity.getBookcaseLabel());
            return "Created New Bookcase " + bookcaseDTO.bookcaseLabel() + " with shelf capacity of " + bookcaseDTO.shelfCapacity();
        }


    }



}
