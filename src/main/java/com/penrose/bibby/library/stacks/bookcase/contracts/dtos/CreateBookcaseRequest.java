package com.penrose.bibby.library.stacks.bookcase.contracts.dtos;

public record CreateBookcaseRequest(
    String location, String zone, String indexId, int shelfCount, int shelfCapacity) {}
