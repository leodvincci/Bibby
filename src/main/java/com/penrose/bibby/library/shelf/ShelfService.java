package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.book.BookEntity;
import com.penrose.bibby.library.bookcase.BookcaseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShelfService {

    ShelfRepository shelfRepository;

    public ShelfService(ShelfRepository shelfRepository) {
        this.shelfRepository = shelfRepository;
    }

    public List<ShelfEntity> getAllShelves(Long bookCaseId){
        return shelfRepository.findByBookcaseId(bookCaseId);
    }

}
