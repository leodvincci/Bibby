package com.penrose.bibby.library.bookcase;
import com.penrose.bibby.library.shelf.Shelf;
import com.penrose.bibby.library.shelf.ShelfEntity;
import com.penrose.bibby.library.shelf.ShelfRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class BookcaseService {
    private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);
    private final BookcaseRepository bookcaseRepository;
    private final ResponseStatusException existingRecordError = new ResponseStatusException(HttpStatus.CONFLICT,"Bookcase with the label already exist");
    private final ShelfRepository shelfRepository;
    public BookcaseService(BookcaseRepository bookcaseRepository, ShelfRepository shelfRepository) {
        this.bookcaseRepository = bookcaseRepository;
        this.shelfRepository = shelfRepository;
    }

    public String createNewBookCase(String label, int capacity){
        BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
        if(bookcaseEntity !=null){
            log.error("Failed to save Record - Record already exist",existingRecordError);
            throw existingRecordError;
        }
        else{
            bookcaseEntity = new BookcaseEntity(label,capacity);
            bookcaseRepository.save(bookcaseEntity);

            for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
                addShelf(bookcaseEntity,i,i);
            }
            log.info("Created new bookcase: {}",bookcaseEntity.getBookcaseLabel());
            return "Created New Bookcase " + label + " with shelf capacity of " + capacity;
        }
    }

    public void addShelf(BookcaseEntity bookcaseEntity, int label, int position){
        ShelfEntity shelfEntity = new ShelfEntity();
        shelfEntity.setBookcaseId(bookcaseEntity.getBookcaseId());
        shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());
        shelfEntity.setShelfPosition(position);
        shelfRepository.save(shelfEntity);
    }

    public List<BookcaseEntity> getAllBookcases(){
        return bookcaseRepository.findAll();
    }

    public Optional<BookcaseEntity> findBookCaseById(Long id){
        return bookcaseRepository.findById(id);
    }






}
