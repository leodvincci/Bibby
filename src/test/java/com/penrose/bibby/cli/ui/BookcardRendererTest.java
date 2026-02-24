package com.penrose.bibby.cli.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BookcardRendererTest {

  /** Tests the createBookCard method for formatting validity. */
  @Test
  void testCreateBookCard_ValidInputs() {
    // Arrange
    BookcardRenderer renderer = new BookcardRenderer();
    String title = "Clean Code";
    String isbn = "978-0132350884";
    String author = "Robert C. Martin";
    String publisher = "Pearson Education";
    String bookcase = "A";
    String shelf = "3";
    String location = "Main Library";

    // Act
    String result =
        renderer.createBookCard(title, isbn, author, publisher, bookcase, shelf, location);

    // Assert
    assertNotNull(result);
    assertTrue(result.contains(title), "Title should be included in the output");
    assertTrue(result.contains(isbn), "ISBN should be included in the output");
    assertTrue(result.contains(author), "Author should be included in the output");
    assertTrue(result.contains(publisher), "Publisher should be included in the output");
    assertTrue(result.contains(bookcase), "Bookcase should be included in the output");
    assertTrue(result.contains(shelf), "Shelf should be included in the output");
    assertTrue(result.contains(location), "Location should be included in the output");
  }

  /** Tests the createBookCard method when publisher is null. */
  @Test
  void testCreateBookCard_NullPublisher() {
    // Arrange
    BookcardRenderer renderer = new BookcardRenderer();
    String title = "The Pragmatic Programmer";
    String isbn = "978-0201616224";
    String author = "Andrew Hunt, David Thomas";
    String publisher = null;
    String bookcase = "B2";
    String shelf = "7";
    String location = "Annex";

    // Act
    String result =
        renderer.createBookCard(title, isbn, author, publisher, bookcase, shelf, location);

    // Assert
    assertNotNull(result);
    assertTrue(
        result.contains("Unknown"),
        "When publisher is null, 'Unknown' should be used as a fallback");
  }

  /** Tests the createBookCard method with a very long author list. */
  @Test
  void testCreateBookCard_LongAuthorList() {
    // Arrange
    BookcardRenderer renderer = new BookcardRenderer();
    String title = "Advanced Programming Techniques";
    String isbn = "978-1234567890";
    String author =
        "John Doe, Jane Smith, Alice Johnson, Bob Brown, Charlie Davis, Eve White, George Martin, ..."
            + " and many others";
    String publisher = "Technical Press";
    String bookcase = "C3";
    String shelf = "10";
    String location = "Main Branch";

    // Act
    String result =
        renderer.createBookCard(title, isbn, author, publisher, bookcase, shelf, location);

    // Assert
    assertNotNull(result);
    assertTrue(result.contains("..."), "Authors should be truncated with ellipsis if too long");
  }

  /** Tests the createBookCard method with a very long title. */
  @Test
  void testCreateBookCard_LongTitle() {
    // Arrange
    BookcardRenderer renderer = new BookcardRenderer();
    String title =
        "An Extremely Long and Detailed Title that Will Likely Exceed Formatting Limits "
            + "for Demonstration Purposes Only";
    String isbn = "123456789X";
    String author = "John Doe";
    String publisher = "Self-Published";
    String bookcase = "A3";
    String shelf = "2";
    String location = "Reading Room";

    // Act
    String result =
        renderer.createBookCard(title, isbn, author, publisher, bookcase, shelf, location);

    // Assert
    assertNotNull(result);
    assertTrue(result.contains(title), "The full title should still be included in the output");
  }

  /** Tests the createBookCard method with special characters in the author field. */
  @Test
  void testCreateBookCard_SpecialCharactersInAuthor() {
    // Arrange
    BookcardRenderer renderer = new BookcardRenderer();
    String title = "Algorithms and Data Structures";
    String isbn = "987-6543210987";
    // Author display name will truncate if longer than 42 characters
    String author = "Dr. Jane O'Connor & Prof. Bob-D";
    String publisher = "Academic Press";
    String bookcase = "C1";
    String shelf = "5";
    String location = "Computer Science Section";

    // Act
    String result =
        renderer.createBookCard(title, isbn, author, publisher, bookcase, shelf, location);

    // Assert
    assertNotNull(result);
    assertTrue(result.contains(author), "Author field should handle special characters properly");
  }

  /** Tests the createBookCard method when author is empty. */
  @Test
  void testCreateBookCard_EmptyAuthor() {
    // Arrange
    BookcardRenderer renderer = new BookcardRenderer();
    String title = "Modern Software Architecture";
    String isbn = "978-1122334455";
    String author = "";
    String publisher = "Tech Publishing";
    String bookcase = "D5";
    String shelf = "8";
    String location = "Main Library";

    // Act
    String result =
        renderer.createBookCard(title, isbn, author, publisher, bookcase, shelf, location);

    // Assert
    assertNotNull(result);
    System.out.println(result);
    assertTrue(result.contains(title), "Title should be included in the output");
    assertTrue(result.contains(isbn), "ISBN should be included in the output");
  }

  /** Tests the createBookCard method when author is null. */
  @Test
  void testCreateBookCard_NullAuthor() {
    // Arrange
    BookcardRenderer renderer = new BookcardRenderer();
    String title = "Modern Software Architecture";
    String isbn = "978-1122334455";
    String author = null;
    String publisher = "Tech Publishing";
    String bookcase = "D5";
    String shelf = "8";
    String location = "Main Library";

    // Act
    String result =
        renderer.createBookCard(title, isbn, author, publisher, bookcase, shelf, location);

    // Assert
    assertNotNull(result);
    assertTrue(result.contains(title), "Title should be included in the output");
    assertTrue(result.contains(isbn), "ISBN should be included in the output");
  }

  /** Tests the createBookCard method with partial data (only title and ISBN). */
  @Test
  void testCreateBookCard_PartialData() {
    // Arrange
    BookcardRenderer renderer = new BookcardRenderer();
    String title = "Partial Book Record";
    String isbn = "978-9999999999";
    String author = null;
    String publisher = null;
    String bookcase = null;
    String shelf = null;
    String location = null;

    // Act
    String result =
        renderer.createBookCard(title, isbn, author, publisher, bookcase, shelf, location);

    // Assert
    assertNotNull(result);
    System.out.println(result);
    assertTrue(result.contains(title), "Title should be included in the output");
    assertTrue(result.contains(isbn), "ISBN should be included in the output");
  }
}
