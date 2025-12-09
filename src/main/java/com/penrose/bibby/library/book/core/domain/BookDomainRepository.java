package com.penrose.bibby.library.book.core.domain;

import java.util.List;

public interface BookDomainRepository {

    List<Book> getBooksByShelfId(Long shelfId);

    void registerBook(Book book);
}
