package com.penrose.bibby.library.cataloging.book.api.dtos;

public record BookDetailView(
    Long bookId,
    String title,
    String authors,
    String bookcaseLabel,
    String shelfLabel,
    String bookStatus) {}
