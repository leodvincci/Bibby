package com.penrose.bibby.library.cataloging.book.core.domain;


public record BookMetaData(String title, String[] authors, String publisher, String description, String isbn_13, String[] categories) {
}
