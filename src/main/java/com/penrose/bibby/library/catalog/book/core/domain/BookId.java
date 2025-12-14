package com.penrose.bibby.library.catalog.book.core.domain;

public class BookId {
    private final Long id;

    public BookId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
