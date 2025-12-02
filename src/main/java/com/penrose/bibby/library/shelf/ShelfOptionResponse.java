package com.penrose.bibby.library.shelf;

public record ShelfOptionResponse(
        Long shelfId,
        String shelfLabel,
        String bookcaseLabel,
        int bookCapacity,
        long bookCount,
        boolean hasSpace
) {
}
