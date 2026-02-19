package com.penrose.bibby.library.cataloging.book.infrastructure.adapter;

import com.penrose.bibby.library.cataloging.book.core.domain.Book;
import com.penrose.bibby.library.cataloging.book.core.domain.BookDomainRepository;
import com.penrose.bibby.library.stacks.shelf.core.ports.BookAccessPort;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Adapter that implements the Shelf module's BookAccessPort. This allows the Stacks context to
 * access book data without depending on the Cataloging context's domain models.
 *
 * <p>Note: The dependency arrow points FROM Book TO Shelf's port interface, following the
 * Dependency Inversion Principle.
 */
@Component
public class BookAccessAdapter implements BookAccessPort {

  private final BookDomainRepository bookDomainRepository;

  public BookAccessAdapter(BookDomainRepository bookDomainRepository) {
    this.bookDomainRepository = bookDomainRepository;
  }

  @Override
  public List<Long> getBookIdsByShelfId(Long shelfId) {
    List<Book> books = bookDomainRepository.getBooksByShelfId(shelfId);
    return books.stream().map(book -> book.getBookId().getId()).collect(Collectors.toList());
  }
}
