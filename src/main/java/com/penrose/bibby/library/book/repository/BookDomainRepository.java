package com.penrose.bibby.library.book.repository;

import com.penrose.bibby.library.book.domain.Book;

import java.util.List;

public interface BookDomainRepository {

    List<Book> getBooksByShelfId(Long shelfId);
}
