package com.penrose.bibby.library.cataloging.book.api.dtos;

public record BookPlacementResponse(
    Long bookId, String title, Long shelfId, String shelfLabel, String bookcaseLabel) {}
