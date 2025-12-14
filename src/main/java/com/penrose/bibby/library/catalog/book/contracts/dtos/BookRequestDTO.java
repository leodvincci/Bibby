package com.penrose.bibby.library.catalog.book.contracts.dtos;

import com.penrose.bibby.library.catalog.author.contracts.AuthorDTO;

import java.util.List;

public record BookRequestDTO(String title, String isbn, List<AuthorDTO> authors) {
}
