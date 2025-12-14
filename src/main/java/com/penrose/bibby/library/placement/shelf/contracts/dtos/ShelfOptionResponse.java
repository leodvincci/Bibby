package com.penrose.bibby.library.placement.shelf.contracts.dtos;

public record ShelfOptionResponse(
        Long shelfId,
        String shelfLabel,
        String bookcaseLabel,
        int bookCapacity,
        long bookCount,
        boolean hasSpace
) {
}
