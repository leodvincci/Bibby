package com.penrose.bibby.library.book;

import java.util.List;

public interface BookDomainRepository {

    List<Book> getBooksByShelfId(Long shelfId);
}
