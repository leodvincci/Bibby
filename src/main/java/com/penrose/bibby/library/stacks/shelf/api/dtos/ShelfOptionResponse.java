package com.penrose.bibby.library.stacks.shelf.api.dtos;

public record ShelfOptionResponse(
    Long shelfId,
    String shelfLabel,
    String bookcaseLabel,
    int bookCapacity,
    long bookCount,
    boolean hasSpace) {}
