package com.penrose.bibby.library.book.contracts.dtos;

import com.penrose.bibby.library.author.contracts.AuthorDTO;

import java.util.List;

public record BookRequestDTO(String title, List<AuthorDTO> authors) {
}
