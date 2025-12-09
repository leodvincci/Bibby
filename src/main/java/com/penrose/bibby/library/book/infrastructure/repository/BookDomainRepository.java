package com.penrose.bibby.library.book.infrastructure.repository;

import com.penrose.bibby.library.book.core.domain.Book;

import java.util.List;

public interface BookDomainRepository {

    List<Book> getBooksByShelfId(Long shelfId);
}
