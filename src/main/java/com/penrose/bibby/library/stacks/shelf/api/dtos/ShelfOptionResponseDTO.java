package com.penrose.bibby.library.stacks.shelf.api.dtos;

public record ShelfOptionResponseDTO(
    Long shelfId, String shelfLabel, int bookCapacity, long bookCount, boolean hasSpace) {}
