package com.penrose.bibby.library.book.api;

public record BookPlacementResponse(
        Long bookId,
        String title,
        Long shelfId,
        String shelfLabel,
        String bookcaseLabel
) {
}
