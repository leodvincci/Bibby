package com.penrose.bibby.library.book.domain;


public record BookMetaData(String title, String[] authors, String publisher, String description, String isbn_13, String[] categories) {
}
