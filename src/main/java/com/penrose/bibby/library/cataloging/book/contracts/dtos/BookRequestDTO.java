package com.penrose.bibby.library.cataloging.book.contracts.dtos;

import com.penrose.bibby.library.cataloging.author.contracts.AuthorDTO;

import java.util.List;

public record BookRequestDTO(String title, String isbn, List<AuthorDTO> authors) {
}
