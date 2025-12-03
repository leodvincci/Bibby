package com.penrose.bibby.library.shelf;

import org.springframework.stereotype.Service;

import com.penrose.bibby.library.book.repository.BookRepository;
import com.penrose.bibby.library.bookcase.persistence.BookcaseEntity;
import com.penrose.bibby.library.bookcase.repository.BookcaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShelfService {

    ShelfJpaRepository shelfJpaRepository;
    BookRepository bookRepository;
    BookcaseRepository bookcaseRepository;

    public ShelfService(ShelfJpaRepository shelfJpaRepository, BookRepository bookRepository, BookcaseRepository bookcaseRepository) {
        this.shelfJpaRepository = shelfJpaRepository;
        this.bookRepository = bookRepository;
        this.bookcaseRepository = bookcaseRepository;
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

    public List<ShelfOptionResponse> getShelfOptions() {
        return shelfJpaRepository.findAll().stream()
                .map(this::toShelfOption)
                .collect(Collectors.toList());
    }

    private ShelfOptionResponse toShelfOption(ShelfEntity shelf) {
        long bookCount = bookRepository.countByShelfId(shelf.getShelfId());
        BookcaseEntity bookcase = bookcaseRepository.findById(shelf.getBookcaseId()).orElse(null);
        String bookcaseLabel = bookcase != null ? bookcase.getBookcaseLabel() : "Unknown Case";
        boolean hasSpace = bookCount < shelf.getBookCapacity();
        return new ShelfOptionResponse(
                shelf.getShelfId(),
                shelf.getShelfLabel(),
                bookcaseLabel,
                shelf.getBookCapacity(),
                bookCount,
                hasSpace
        );
    }
}
