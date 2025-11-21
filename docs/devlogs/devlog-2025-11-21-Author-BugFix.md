# Devlog: Multiple Authors Bug Fix

## Git Commit Message

```
fix: resolve duplicate book creation when adding multiple authors

Refactored authorNameComponentFlow to separate data collection from 
persistence. Method now returns Author object instead of creating 
BookEntity directly, allowing proper batch processing of multiple 
authors before single book creation.

- Changed authorNameComponentFlow return type from void to Author
- Moved book creation outside author input loop
- Updated BookRequestDTO to accept List<Author> instead of individual names
- Eliminated duplicate title error when processing multiple authors

Fixes issue where component flow would attempt to save same book title
multiple times during multi-author input, violating unique constraint.
```

## Problem Discovery

Hit a critical bug in the multi-author book creation flow. When adding a book with multiple authors, the system would crash with a "Book Already Exists" error. Root cause: `authorNameComponentFlow()` was creating and persisting a complete `BookEntity` on *each* author input iteration.

**The flow:**

1. User enters title + author count
2. Loop iterates N times for N authors
3. First iteration: collects author data, creates book ✓
4. Second iteration: collects author data, tries to create book with same title ✗
5. Duplicate title constraint violation throws exception

## The Fix

Classic separation of concerns issue. The component flow method had two responsibilities:

- **Data collection** (getting author names from user)
- **Business logic** (creating book records)

**Refactoring strategy:**

```java
// BEFORE: Side-effect-laden flow method
public void authorNameComponentFlow(String title) {
    // ... collect author input ...
    BookRequestDTO bookRequestDTO = new BookRequestDTO(title, firstName, lastName);
    bookService.createNewBook(bookRequestDTO);  // ← Persistence side effect!
}

// AFTER: Pure data collection
public Author authorNameComponentFlow(String title) {
    // ... collect author input ...
    return new Author(firstName, lastName);  // ← Just return data
}
```

Moved book creation to the calling context where we have full visibility of all authors:

```java
List<Author> authors = new ArrayList<>();
for(int i = 0; i < authorCount; i++){
    authors.add(authorNameComponentFlow(title));  // Collect all authors first
}
BookRequestDTO bookRequestDTO = new BookRequestDTO(title, authors);
bookService.createNewBook(bookRequestDTO);  // Create once with complete data
```

## Key Learning

**Separation of concerns isn't just theoretical architecture talk** - it has immediate practical implications for correctness. When a method mixes UI interaction with persistence logic, debugging becomes harder and the code becomes brittle to changes in control flow.

Component flow methods should be **pure data collectors** that return structured data, letting the orchestrating code handle business logic and persistence. This also makes the flow methods more reusable and testable.

## Ripple Changes

- Updated `BookRequestDTO` from `(String title, String firstName, String lastName)` to `(String title, List<Author> authors)`
- Modified `BookService.createNewBook()` to iterate over author list
- Added proper `Set<AuthorEntity>` handling in `BookFactory`

**Time investment:** ~20 minutes to identify, fix, and verify **Prevention for future:** Consider this pattern for all component flows that interact with persistence