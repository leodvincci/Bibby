package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.book.BookEntity;
import com.penrose.bibby.library.bookcase.BookcaseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShelfService {

    ShelfRepository shelfRepository;

    public ShelfService(ShelfRepository shelfRepository) {
        this.shelfRepository = shelfRepository;
    }

    public List<ShelfEntity> getAllShelves(Long bookCaseId){
        return shelfRepository.findByBookcaseId(bookCaseId);
    }

    public Optional<ShelfEntity> findShelfById(Long shelfId) {
        return shelfRepository.findById(shelfId);
    }

    public List<ShelfEntity> findByBookcaseId(Long bookcaseId) {
        return shelfRepository.findByBookcaseId(bookcaseId);
    }

//
//    public BookcaseEntity getBookCase() {
//        return shelfRepository.
//    }

//    public ShelfEntity findShelfById(Long id) {
//        return shelfRepository.
//    }
}
