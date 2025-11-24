package com.penrose.bibby.library.shelf;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShelfService {

    ShelfJpaRepository shelfJpaRepository;

    public ShelfService(ShelfJpaRepository shelfJpaRepository) {
        this.shelfJpaRepository = shelfJpaRepository;
    }

    public List<ShelfEntity> getAllShelves(Long bookCaseId){
        return shelfJpaRepository.findByBookcaseId(bookCaseId);
    }

    public Optional<ShelfEntity> findShelfById(Long shelfId) {
        return shelfJpaRepository.findById(shelfId);
    }

    public List<ShelfEntity> findByBookcaseId(Long bookcaseId) {
        return shelfJpaRepository.findByBookcaseId(bookcaseId);
    }

    public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
        return shelfJpaRepository.findShelfSummariesByBookcaseId(bookcaseId);
    }
}
