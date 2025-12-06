package com.penrose.bibby.library.book.contracts;

public interface BookFacade {
    void setShelfForBook(Long id, Long shelfId);

    BookDTO findBookByIsbn(String isbn);

    BookDTO findBookByTitle(String title);
}
