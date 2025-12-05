package com.penrose.bibby.library.bookcase.contracts;

public record BookcaseDTO(Long bookcaseId, String bookcaseLabel, int shelfCapacity, int bookCapacity) {
}
