package com.penrose.bibby.library.bookcase.application;
import com.penrose.bibby.library.bookcase.contracts.BookcaseDTO;
import com.penrose.bibby.library.bookcase.contracts.BookcaseFacade;
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseRepository;
import com.penrose.bibby.library.shelf.core.domain.ShelfFactory;
import com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class BookcaseService implements BookcaseFacade {
    private final ShelfFactory shelfFactory;
    private static final Logger log = LoggerFactory.getLogger(BookcaseService.class);
    private final BookcaseRepository bookcaseRepository;
    private final ResponseStatusException existingRecordError = new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase with the label already exist");
    private final ShelfJpaRepository shelfJpaRepository;

    public BookcaseService(BookcaseRepository bookcaseRepository, ShelfJpaRepository shelfJpaRepository, ShelfFactory shelfFactory) {
        this.bookcaseRepository = bookcaseRepository;
        this.shelfJpaRepository = shelfJpaRepository;
        this.shelfFactory = shelfFactory;
    }

    public String createNewBookCase(String label, int shelfCapacity, int bookCapacity) {
        BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
        if (bookcaseEntity != null) {
            log.error("Failed to save Record - Record already exist", existingRecordError);
            throw existingRecordError;
        } else {
            bookcaseEntity = new BookcaseEntity(label, shelfCapacity, bookCapacity * shelfCapacity);
            bookcaseRepository.save(bookcaseEntity);

            for (int i = 0; i < bookcaseEntity.getShelfCapacity(); i++) {
                addShelf(bookcaseEntity, i, i, bookCapacity);
            }
            log.info("Created new bookcase: {}", bookcaseEntity.getBookcaseLabel());
            return "Created New Bookcase " + label + " with shelf shelfCapacity of " + shelfCapacity;
        }
    }

    public void addShelf(BookcaseEntity bookcaseEntity, int label, int position, int bookCapacity) {
        shelfJpaRepository.save(shelfFactory.createEntity(bookcaseEntity.getBookcaseId(), position, "Shelf " + label, bookCapacity));
    }

    public List<BookcaseDTO> getAllBookcases() {
        List<BookcaseEntity> bookcaseEntities = bookcaseRepository.findAll();
        return bookcaseEntities.stream()
                .map(entity -> new BookcaseDTO(
                        entity.getBookcaseId(),
                        entity.getBookcaseLabel(),
                        entity.getShelfCapacity(),
                        entity.getBookCapacity()
                ))
                .toList();
    }


    public Optional<BookcaseDTO> findBookCaseById(Long id) {
        Optional<BookcaseEntity> bookcaseEntity = bookcaseRepository.findById(id);

        if (bookcaseEntity.isEmpty()) {
            return Optional.empty();
        }
        return BookcaseDTO.fromEntity(bookcaseEntity);
    }

}
