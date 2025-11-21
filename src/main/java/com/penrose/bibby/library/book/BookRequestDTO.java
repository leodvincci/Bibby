package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.Author;

import java.util.List;

public record BookRequestDTO(String title, List<Author> authors) {

}
