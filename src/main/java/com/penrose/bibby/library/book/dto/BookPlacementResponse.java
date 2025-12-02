package com.penrose.bibby.library.book.dto;

public record BookPlacementResponse(
        Long bookId,
        String title,
        Long shelfId,
        String shelfLabel,
        String bookcaseLabel
) {
}
