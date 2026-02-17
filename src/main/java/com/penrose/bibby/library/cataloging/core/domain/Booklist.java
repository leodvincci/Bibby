package com.penrose.bibby.library.cataloging.core.domain;

import com.penrose.bibby.library.cataloging.core.domain.valueobject.BookIdentifier;
import com.penrose.bibby.library.cataloging.core.domain.valueobject.BooklistId;
import com.penrose.bibby.library.cataloging.core.domain.valueobject.BooklistName;
import java.time.Instant;
import java.util.Set;

/**
 * Aggregate root representing a named list of books in the Cataloging domain.
 *
 * <p>A {@code Booklist} groups a set of {@link BookIdentifier book identifiers} under a stable
 * identity ({@link BooklistId}) and a human-readable {@link BooklistName}. It also tracks lifecycle
 * metadata such as creation and last-update timestamps.
 *
 * <h2>Purpose</h2>
 *
 * <ul>
 *   <li>Provide a single domain object to represent and evolve a booklist over time.
 *   <li>Encapsulate rename behavior so timestamp updates happen consistently.
 * </ul>
 *
 * <h2>Invariants</h2>
 *
 * <ul>
 *   <li>{@code listId} is required and must not change after construction.
 *   <li>{@code booklistName} is required; renaming to {@code null} is rejected.
 *   <li>{@code createdAt} represents the creation time and should not move forward/backward once
 *       set.
 *   <li>{@code updatedAt} is the last modification time and should be {@code >= createdAt}.
 *   <li>{@code bookIdentifier} represents the current membership of the list; callers should treat
 *       it as a set (no duplicates) and avoid {@code null} elements.
 * </ul>
 *
 * <p><strong>Note:</strong> Some invariants (e.g., non-null fields, timestamp ordering) are domain
 * rules and should be enforced by construction and mutation methods; setters that allow arbitrary
 * values may bypass these guarantees.
 */
public class Booklist {
  private final BooklistId listId;
  private BooklistName booklistName;
  private final Instant createdAt;
  private Instant updatedAt;
  private Set<BookIdentifier> bookIdentifier;

  public Booklist(
      BooklistId listId, BooklistName booklistName, Set<BookIdentifier> bookIdentifier) {
    this.booklistName = booklistName;
    this.listId = listId;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
    this.bookIdentifier = bookIdentifier;
  }

  public BooklistId getListId() {
    return listId;
  }

  public BooklistName getBooklistName() {
    return booklistName;
  }

  public void renameBooklist(BooklistName newBooklistName) {
    if (newBooklistName == null) {
      throw new IllegalArgumentException("New booklist name cannot be null");
    }

    if (newBooklistName.equals(this.booklistName)) {
      return; // No change needed
    }

    this.booklistName = newBooklistName;
    this.updatedAt = Instant.now();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Set<BookIdentifier> getBookIdentifier() {
    return bookIdentifier;
  }

  public void setBookIdentifier(Set<BookIdentifier> bookIdentifier) {
    this.bookIdentifier = bookIdentifier;
  }
}
