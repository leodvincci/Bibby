package com.penrose.bibby.library.cataloging.book.api.dtos;

public record BookLocationResponse(
    String bookcaseLocation, String bookcaseLabel, String shelfLabel) {}
