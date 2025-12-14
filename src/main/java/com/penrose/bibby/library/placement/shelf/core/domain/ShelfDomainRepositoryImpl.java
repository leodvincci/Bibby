package com.penrose.bibby.library.placement.shelf.core.domain;

import com.penrose.bibby.library.catalog.book.core.domain.Book;
import com.penrose.bibby.library.catalog.book.core.domain.BookDomainRepository;
import com.penrose.bibby.library.placement.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.placement.shelf.infrastructure.repository.ShelfJpaRepository;
import com.penrose.bibby.library.placement.shelf.infrastructure.mapping.ShelfMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        List<Long> bookIds = new ArrayList<>();
        for(Book book : books){
            bookIds.add(book.getBookId().getId());
        }
        return shelfMapper.toDomainFromDTO(entity,bookIds);
    }

    @Override
    public void save(Shelf shelf) {
        ShelfEntity entity = shelfJpaRepository.findById(shelf.getId()).get();
//        entity = shelfMapper.updateEntity(shelf);
        shelfJpaRepository.save(entity);

    }


}
