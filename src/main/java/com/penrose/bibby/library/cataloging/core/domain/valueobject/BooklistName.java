package com.penrose.bibby.library.cataloging.core.domain.valueobject;

public record BooklistName(String value) {

  public BooklistName {

    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Booklist name cannot be null or blank");
    }
    if (value.length() > 60) {
      throw new IllegalArgumentException("Booklist name cannot exceed 100 characters");
    }
    if (!value.matches("[a-zA-Z0-9 \\-_,.!?()]+")) {
      throw new IllegalArgumentException("Booklist name contains invalid characters");
    }
    value = value.replaceAll("\\s+", " ");
    value = value.trim();
  }
}
