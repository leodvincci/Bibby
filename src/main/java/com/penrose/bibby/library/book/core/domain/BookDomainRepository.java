package com.penrose.bibby.library.book.core.domain;

import com.penrose.bibby.library.book.infrastructure.entity.BookEntity;

import java.util.List;

public interface BookDomainRepository {

    List<Book> getBooksByShelfId(Long shelfId);

    void registerBook(Book book);

    void updateBook(Book book);

    BookEntity findBookEntityByTitle(String bookTitle);
}
