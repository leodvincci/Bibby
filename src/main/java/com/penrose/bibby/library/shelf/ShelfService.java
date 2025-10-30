package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.book.BookEntity;
import org.springframework.stereotype.Service;

@Service
public class ShelfService {

    ShelfRepository shelfRepository;

    public ShelfService(ShelfRepository shelfRepository) {
        this.shelfRepository = shelfRepository;
    }

    public void addToShelf(Book book, String shelfLabel){
        ShelfEntity shelfEntity = new ShelfEntity();
        shelfEntity.setShelfLabel(shelfLabel);
        shelfEntity.setBook(book.getTitle());
        shelfRepository.save(shelfEntity);
    }


}
