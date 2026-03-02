package com.penrose.bibby.library.cataloging.author.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AuthorNameTest {

  @Test
  void parseMiddleName_shouldReturnEmptyStringWhenSingleName() {
    AuthorName authorName = new AuthorName("Jane");

    assertEquals("", authorName.parseMiddleName());
  }

  @Test
  void constructor_shouldNormalizeIncomingName() {
    AuthorName name = new AuthorName("  J.  R. R.   Tolkien.  ");

    // periods removed, extra spaces collapsed, trimmed
    assertEquals("J R R Tolkien", name.getAuthorName());
  }

  @Test
  void getAuthorName_shouldReturnNormalizedName() {
    AuthorName name = new AuthorName("  Jane   A.   Doe  ");

    assertEquals("Jane A Doe", name.getAuthorName());
  }

  @Test
  void parseFirstName_shouldReturnFirstToken() {
    AuthorName name = new AuthorName("  Isaac   Asimov  ");

    assertEquals("Isaac", name.parseFirstName());
  }

  @Test
  void parseLastName_shouldReturnLastTokenWhenMultipleParts() {
    AuthorName name = new AuthorName("  Neil   de   Grasse   Tyson  ");

    assertEquals("Tyson", name.parseLastName());
  }

  @Test
  void parseLastName_shouldReturnEmptyStringWhenSingleName() {
    AuthorName name = new AuthorName("Madonna");

    assertEquals("", name.parseLastName());
  }

  @Test
  void parseMiddleName_shouldReturnEmptyStringWhenNoMiddleName() {
    AuthorName name = new AuthorName("Ada Lovelace");

    assertEquals("", name.parseMiddleName());
  }

  @Test
  void parseMiddleName_shouldReturnSingleMiddleName() {
    AuthorName name = new AuthorName("Jane Amanda Doe");

    assertEquals("Amanda", name.parseMiddleName());
  }

  @Test
  void parseMiddleName_shouldReturnAllMiddleNamesJoinedBySingleSpaces() {
    AuthorName name = new AuthorName("John Ronald Reuel Tolkien");

    assertEquals("Ronald Reuel", name.parseMiddleName());
  }

  @Test
  void normalized_shouldReturnAlreadyNormalizedNameAndBeIdempotent() {
    AuthorName name = new AuthorName("  J.   K.   Rowling   ");

    String firstCall = name.normalized();
    String secondCall = name.normalized();

    assertEquals("J K Rowling", firstCall);
    assertEquals(firstCall, secondCall); // idempotent
    assertEquals(name.getAuthorName(), firstCall);
  }

  @Test
  void normalizeString_shouldNormalizeArbitraryInputString() {
    AuthorName name = new AuthorName("Placeholder"); // just to get an instance

    String normalized = name.normalizeString("  G.   R. R.   Martin. ");

    assertEquals("G R R Martin", normalized);
  }

  @Test
  void toString_shouldReturnNormalizedAuthorName() {
    AuthorName name = new AuthorName("  Chinua   Achebe. ");

    assertEquals("Chinua Achebe", name.toString());
  }
}
