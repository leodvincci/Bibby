package com.penrose.bibby.library.shelf.domain;

import com.penrose.bibby.library.book.core.domain.Book;
import com.penrose.bibby.library.book.infrastructure.repository.BookDomainRepository;
import com.penrose.bibby.library.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.shelf.infrastructure.repository.ShelfJpaRepository;
import com.penrose.bibby.library.shelf.infrastructure.mapping.ShelfMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShelfDomainRepositoryImpl implements ShelfDomainRepository {

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
        List<Long> bookIds = books.stream().map(Book::getBookId).toList();
        return shelfMapper.toDomainFromDTO(entity,bookIds);
    }

    @Override
    public void save(Shelf shelf) {
        ShelfEntity entity = shelfJpaRepository.findById(shelf.getId()).get();
//        entity = shelfMapper.updateEntity(shelf);
        shelfJpaRepository.save(entity);

    }
}
