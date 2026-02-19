package com.penrose.bibby.library.cataloging.book.api.dtos;

import com.penrose.bibby.library.cataloging.author.api.dtos.AuthorDTO;
import java.util.List;

public record BookRequestDTO(
    String title, String isbn, List<AuthorDTO> authors, Long bookshelfId, String publisher) {
  public BookRequestDTO(String title, String isbn, List<AuthorDTO> authors, String publisher) {
    this(title, isbn, authors, null, publisher);
  }
}
