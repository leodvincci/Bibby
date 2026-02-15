package com.penrose.bibby.library.cataloging.book.api.dtos;

import com.penrose.bibby.library.cataloging.author.api.AuthorDTO;
import java.util.List;

public record BookRequestDTO(String title, String isbn, List<AuthorDTO> authors, Long bookshelfId) {
  public BookRequestDTO(String title, String isbn, List<AuthorDTO> authors) {
    this(title, isbn, authors, null);
  }
}
