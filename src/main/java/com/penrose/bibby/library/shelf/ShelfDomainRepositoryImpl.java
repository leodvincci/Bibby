package com.penrose.bibby.library.shelf;

import com.penrose.bibby.library.book.BookRepository;
import org.springframework.stereotype.Component;

@Component
public class ShelfDomainRepositoryImpl implements ShelfDomainRepository{

    private final ShelfJpaRepository jpaRepository;
    private final ShelfMapper shelfMapper;
    private final BookRepository bookRepository;
    private final ShelfJpaRepository shelfJpaRepository;
    public ShelfDomainRepositoryImpl(ShelfJpaRepository jpaRepository,
                                     ShelfMapper shelfMapper, BookRepository bookRepository, ShelfJpaRepository shelfJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.shelfMapper = shelfMapper;
        this.bookRepository = bookRepository;
        this.shelfJpaRepository = shelfJpaRepository;
    }

    @Override
    public Shelf getById(Long id) {
        ShelfEntity entity = jpaRepository.findById(id).orElse(null);
        return shelfMapper.toDomain(entity,bookRepository.findByShelfId(id));
    }

    @Override
    public void save(Shelf shelf) {
        ShelfEntity entity = shelfJpaRepository.findById(shelf.getId()).get();
//        entity = shelfMapper.updateEntity(shelf);
        shelfJpaRepository.save(entity);

    }
}
