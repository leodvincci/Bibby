package com.penrose.bibby.library.stacks.shelf.infrastructure.adapter.outbound;

import com.penrose.bibby.library.cataloging.book.core.domain.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.BookDomainRepository;
import com.penrose.bibby.library.stacks.shelf.core.domain.Shelf;
import com.penrose.bibby.library.stacks.shelf.core.domain.ShelfDomainRepository;
import com.penrose.bibby.library.stacks.shelf.core.domain.valueobject.ShelfId;
import com.penrose.bibby.library.stacks.shelf.infrastructure.entity.ShelfEntity;
import com.penrose.bibby.library.stacks.shelf.infrastructure.repository.ShelfJpaRepository;
import com.penrose.bibby.library.stacks.shelf.infrastructure.mapping.ShelfMapper;
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
    public Shelf getById(ShelfId id) {
        ShelfEntity entity = jpaRepository.findById(id.shelfId()).orElse(null);
        List<Book> books = bookDomainRepository.getBooksByShelfId(id.shelfId());
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
