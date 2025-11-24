package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.book.BookDomainRepository;
import com.penrose.bibby.library.book.BookRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShelfDomainRepositoryImpl implements ShelfDomainRepository{

    private final ShelfJpaRepository jpaRepository;
    private final ShelfMapper shelfMapper;
    private final BookDomainRepository bookDomainRepository;
    private final ShelfJpaRepository shelfJpaRepository;
    public ShelfDomainRepositoryImpl(ShelfJpaRepository jpaRepository,
                                     ShelfMapper shelfMapper, BookDomainRepository bookDomainRepository, ShelfJpaRepository shelfJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.shelfMapper = shelfMapper;
        this.bookDomainRepository = bookDomainRepository;
        this.shelfJpaRepository = shelfJpaRepository;
    }

    @Override
    public Shelf getById(Long id) {
        ShelfEntity entity = jpaRepository.findById(id).orElse(null);
        List<Book> books = bookDomainRepository.getBooksByShelfId(id);
        return shelfMapper.toDomain(entity,books);
    }

    @Override
    public void save(Shelf shelf) {
        ShelfEntity entity = shelfJpaRepository.findById(shelf.getId()).get();
//        entity = shelfMapper.updateEntity(shelf);
        shelfJpaRepository.save(entity);

    }
}
