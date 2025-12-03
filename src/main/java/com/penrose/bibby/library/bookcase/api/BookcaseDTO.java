package com.penrose.bibby.library.bookcase.api;

public record BookcaseDTO(Long bookcaseId, String bookcaseLabel, int shelfCapacity, int bookCapacity) {
}
