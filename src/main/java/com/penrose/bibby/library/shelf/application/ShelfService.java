package com.penrose.bibby.library.shelf.application;

import com.penrose.bibby.library.book.contracts.BookDTO;
import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;
import com.penrose.bibby.library.shelf.contracts.ShelfDTO;
import com.penrose.bibby.library.shelf.contracts.ShelfFacade;
import com.penrose.bibby.library.shelf.domain.Shelf;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.shelf.infrastructure.mapping.ShelfMapper;
import com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository;
import com.penrose.bibby.library.shelf.contracts.ShelfOptionResponse;
import com.penrose.bibby.library.shelf.contracts.ShelfSummary;
import org.springframework.stereotype.Service;

import com.penrose.bibby.library.book.infrastructure.repository.BookRepository;
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseEntity;
import com.penrose.bibby.library.bookcase.infrastructure.BookcaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShelfService implements ShelfFacade {

    ShelfJpaRepository shelfJpaRepository;
    BookRepository bookRepository;
    BookcaseRepository bookcaseRepository;
    ShelfMapper shelfMapper;

    public ShelfService(ShelfMapper shelfMapper,ShelfJpaRepository shelfJpaRepository, BookRepository bookRepository, BookcaseRepository bookcaseRepository) {
        this.shelfJpaRepository = shelfJpaRepository;
        this.bookRepository = bookRepository;
        this.bookcaseRepository = bookcaseRepository;
        this.shelfMapper = new ShelfMapper();
    }

    public List<ShelfEntity> getAllShelves(Long bookCaseId){
        return shelfJpaRepository.findByBookcaseId(bookCaseId);
    }

    public Optional<ShelfDTO> findShelfById(Long shelfId) {
        ShelfEntity shelfEntity = shelfJpaRepository.findById(shelfId).orElse(null);
        return Optional.of(ShelfDTO.fromEntity(shelfEntity));
    }

    public List<ShelfDTO> findByBookcaseId(Long bookcaseId) {
        List<ShelfEntity> shelves = shelfJpaRepository.findByBookcaseId(bookcaseId);
        return shelves.stream()
                .map(ShelfDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public List<BookDTO> findBooksByShelf(Long aLong) {
        List<BookEntity> books = bookRepository.findByShelfId(aLong);
        return books.stream()
                .map(BookDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
        return shelfJpaRepository.findShelfSummariesByBookcaseId(bookcaseId);
    }

    public Shelf mapToDomain(ShelfDTO shelfDTO) {
        return new Shelf(
                shelfDTO.shelfLabel(),
                shelfDTO.shelfPosition(),
                shelfDTO.bookCapacity()
        );
    }

    @Override
    public Boolean isFull(ShelfDTO shelfDTO) {
        Optional<ShelfEntity> shelfEntity = shelfJpaRepository.findById(shelfDTO.shelfId());
        Shelf shelf = shelfEntity.map(shelfMapper::toDomain).orElse(null);
        assert shelf != null;
        return shelf.isFull();
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
    public List<com.penrose.bibby.library.shelf.contracts.ShelfDTO> getAllDTOShelves(Long bookcaseId) {
        return getAllShelves(bookcaseId).stream()
                .map(com.penrose.bibby.library.shelf.contracts.ShelfDTO::fromEntity)
                .toList();
    }
}
