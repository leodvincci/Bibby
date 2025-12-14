package com.penrose.bibby.library.catalog.book.contracts.dtos;

public record BookPlacementResponse(
        Long bookId,
        String title,
        Long shelfId,
        String shelfLabel,
        String bookcaseLabel
) {
}
