package com.penrose.bibby.library.shelf.application;

import com.penrose.bibby.library.shelf.api.ShelfDTO;
import com.penrose.bibby.library.shelf.api.ShelfFacade;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository;
import com.penrose.bibby.library.shelf.api.ShelfOptionResponse;
import com.penrose.bibby.library.shelf.api.ShelfSummary;
import org.springframework.stereotype.Service;

import com.penrose.bibby.library.book.infrastructure.repository.BookRepository;
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShelfService implements ShelfFacade {

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

    @Override
    public List<ShelfDTO> getAllDTOShelves(Long bookcaseId) {
        return getAllShelves(bookcaseId).stream()
                .map(ShelfDTO::fromEntity)
                .toList();
    }
}
