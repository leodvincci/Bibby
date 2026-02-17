package com.penrose.bibby.library.cataloging.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.penrose.bibby.library.cataloging.core.domain.valueobject.BookIdentifier;
import com.penrose.bibby.library.cataloging.core.domain.valueobject.BooklistId;
import com.penrose.bibby.library.cataloging.core.domain.valueobject.BooklistName;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BooklistTest {

  @Test
  void renameBooklist_ShouldUpdateBooklistName_WhenValidNewNameProvided() {
    // Arrange
    BooklistName initialName = new BooklistName("Old Booklist Name");
    BooklistId listId = new BooklistId(123L);
    Set<BookIdentifier> bookIdentifiers = Set.of(new BookIdentifier(233L));
    Booklist booklist = new Booklist(listId, initialName, bookIdentifiers);

    BooklistName newName = new BooklistName("New Booklist Name");

    // Act
    booklist.renameBooklist(newName);

    // Assert
    assertEquals(newName, booklist.getBooklistName());
  }

  @Test
  void renameBooklist_ShouldUpdateUpdatedAt_WhenCalled() {
    // Arrange
    BooklistName initialName = new BooklistName("Old Booklist Name");
    BooklistId listId = new BooklistId(123L);
    Set<BookIdentifier> bookIdentifiers = Set.of(new BookIdentifier(233L));
    Booklist booklist = new Booklist(listId, initialName, bookIdentifiers);

    Instant initialUpdatedAt = booklist.getUpdatedAt();
    BooklistName newName = new BooklistName("New Booklist Name");

    // Act
    booklist.renameBooklist(newName);

    // Assert
    assertTrue(booklist.getUpdatedAt().isAfter(initialUpdatedAt));
  }

  @Test
  void renameBooklist_ShouldThrowException_WhenNewNameIsNull() {
    // Arrange
    BooklistName initialName = new BooklistName("Old Booklist Name");
    BooklistId listId = new BooklistId(123L);
    Set<BookIdentifier> bookIdentifiers = Set.of(new BookIdentifier(233L));
    Booklist booklist = new Booklist(listId, initialName, bookIdentifiers);

    // Assert
    assertThrows(IllegalArgumentException.class, () -> booklist.renameBooklist(null));
  }

  @Test
  void renameBooklist_ShouldThrowException_WhenNewNameExceedsMaxLength() {
    // Arrange
    BooklistName initialName = new BooklistName("Old Booklist Name");
    BooklistId listId = new BooklistId(123L);
    Set<BookIdentifier> bookIdentifiers = Set.of(new BookIdentifier(233L));
    Booklist booklist = new Booklist(listId, initialName, bookIdentifiers);

    StringBuilder longName = new StringBuilder();
    for (int i = 0; i < 101; i++) {
      longName.append("a");
    }

    // Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> booklist.renameBooklist(new BooklistName(longName.toString())));
  }

  @Test
  void renameBooklist_ShouldNotUpdateUpdatedAt_WhenNameIsUnchanged() {
    // Arrange
    BooklistName initialName = new BooklistName("Unchanged Booklist Name");
    BooklistId listId = new BooklistId(123L);
    Set<BookIdentifier> bookIdentifiers = Set.of(new BookIdentifier(233L));
    Booklist booklist = new Booklist(listId, initialName, bookIdentifiers);

    Instant initialUpdatedAt = booklist.getUpdatedAt();

    // Act
    booklist.renameBooklist(new BooklistName("Unchanged Booklist Name"));

    // Assert
    assertEquals(initialUpdatedAt, booklist.getUpdatedAt());
    assertEquals("Unchanged Booklist Name", booklist.getBooklistName().value());
  }

  @Test
  @DisplayName("Should throw exception for invalid special characters in BooklistName")
  void renameBooklist_ShouldThrowExceptionForInvalidSpecialCharacters() {
    // Arrange
    BooklistName initialName = new BooklistName("Valid Name");
    BooklistId listId = new BooklistId(123L);
    Set<BookIdentifier> bookIdentifiers = Set.of(new BookIdentifier(233L));
    Booklist booklist = new Booklist(listId, initialName, bookIdentifiers);

    // Act & Assert
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> booklist.renameBooklist(new BooklistName("Invalid@Characters")));
    assertEquals("Booklist name contains invalid characters", exception.getMessage());
  }

  @ParameterizedTest
  @DisplayName("Should rename with valid special characters in BooklistName")
  @ValueSource(strings = {"Valid_Name", "Experiment 42", "Title Fine!", "Done.", "Book (2025)"})
  void renameBooklist_ShouldAllowValidSpecialCharacters(String validSpecialName) {
    // Arrange
    BooklistName initialName = new BooklistName("Valid Name");
    BooklistId listId = new BooklistId(123L);
    Set<BookIdentifier> bookIdentifiers = Set.of(new BookIdentifier(233L));
    Booklist booklist = new Booklist(listId, initialName, bookIdentifiers);

    // Act
    booklist.renameBooklist(new BooklistName(validSpecialName));

    // Assert
    assertEquals(validSpecialName, booklist.getBooklistName().value());
  }

  @Test
  void renameBooklist_ShouldUpdateUpdatedAtWithNewTimestamp_WhenCalledTwice()
      throws InterruptedException {
    // Arrange
    BooklistName initialName = new BooklistName("First Name");
    BooklistId listId = new BooklistId(123L);
    Set<BookIdentifier> bookIdentifiers = Set.of(new BookIdentifier(233L));
    Booklist booklist = new Booklist(listId, initialName, bookIdentifiers);

    BooklistName secondName = new BooklistName("Second Name");

    // Act
    booklist.renameBooklist(secondName);
    Instant afterFirstRename = booklist.getUpdatedAt();
    Thread.sleep(10); // Simulate a delay to ensure timestamps differ
    booklist.renameBooklist(new BooklistName("Third Name"));

    // Assert
    assertTrue(booklist.getUpdatedAt().isAfter(afterFirstRename));
  }

  @Test
  void renameBooklist_ShouldThrowException_WhenNewNameContainsOnlySpaces() {
    // Arrange
    BooklistName initialName = new BooklistName("Valid Name");
    BooklistId listId = new BooklistId(123L);
    Set<BookIdentifier> bookIdentifiers = Set.of(new BookIdentifier(233L));
    Booklist booklist = new Booklist(listId, initialName, bookIdentifiers);

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> booklist.renameBooklist(new BooklistName("   ")));
  }

  @Test
  void shouldThrowExceptionWhenValueIsNullOrBlank() {
    IllegalArgumentException exception1 =
        assertThrows(IllegalArgumentException.class, () -> new BooklistName(null));
    assertEquals("Booklist name cannot be null or blank", exception1.getMessage());

    IllegalArgumentException exception2 =
        assertThrows(IllegalArgumentException.class, () -> new BooklistName(""));
    assertEquals("Booklist name cannot be null or blank", exception2.getMessage());

    IllegalArgumentException exception3 =
        assertThrows(IllegalArgumentException.class, () -> new BooklistName("   "));
    assertEquals("Booklist name cannot be null or blank", exception3.getMessage());
  }

  @Test
  @DisplayName("Should create BooklistName when value is valid")
  void shouldCreateBooklistNameWhenValueIsValid() {
    String validName = "My Summer Reading 2024!";
    BooklistName booklistName = new BooklistName(validName);
    assertEquals(validName, booklistName.value());
  }

  @ParameterizedTest
  @ValueSource(strings = {"History", "Sci-Fi", "Books (2023)", "Read, then return?", "Done."})
  @DisplayName("Should allow valid special characters from the allowed list")
  void shouldAllowValidSpecialCharacters(String validValue) {
    assertDoesNotThrow(() -> new BooklistName(validValue));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "  "})
  @DisplayName("Should throw exception when value is blank or empty")
  void shouldThrowExceptionWhenValueIsBlank(String invalidValue) {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new BooklistName(invalidValue));
    assertEquals("Booklist name cannot be null or blank", exception.getMessage());
  }

  @Test
  @DisplayName("Should throw exception when value is null")
  void shouldThrowExceptionWhenValueIsNull() {
    assertThrows(IllegalArgumentException.class, () -> new BooklistName(null));
  }

  @Test
  @DisplayName("Should throw exception when value exceeds 100 characters")
  void shouldThrowExceptionWhenValueIsTooLong() {
    String longName = "a".repeat(101);
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new BooklistName(longName));
    assertEquals("Booklist name cannot exceed 100 characters", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"Reading @ Home", "Books #1", "List & More", "Price: $10", "Name\nNewline"})
  @DisplayName("Should throw exception when value contains invalid characters")
  void shouldThrowExceptionWhenValueContainsInvalidCharacters(String invalidValue) {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new BooklistName(invalidValue));
    assertEquals("Booklist name contains invalid characters", exception.getMessage());
  }

  @Test
  void shouldReplaceMultipleWhitespaceWithSingleSpace() {
    BooklistName booklistName = new BooklistName("This   is   a   test");
    assertEquals("This is a test", booklistName.value());
  }

  @Test
  void shouldNotModifyStringWithSingleSpaces() {
    BooklistName booklistName = new BooklistName("This is a test");
    assertEquals("This is a test", booklistName.value());
  }

  @Test
  void shouldHandleLeadingAndTrailingWhitespace() {
    BooklistName booklistName = new BooklistName("   Leading and trailing   ");
    assertEquals("Leading and trailing", booklistName.value());
  }

  @Test
  void shouldHandleEmptyStringAfterWhitespaceReplacement() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new BooklistName("   "));
    assertEquals("Booklist name cannot be null or blank", exception.getMessage());
  }
}
