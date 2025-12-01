## Devlog Entry

**Date**: 2025-11-21

### Refactor: Extract Helper Methods from BookService.createNewBook()

**Context**

The `createNewBook()` method was doing multiple responsibilities in a single method body: validating uniqueness, extracting author entities, and creating the book. This made the method harder to read and understand at a glance.

**Initial Refactoring Attempt**

**Before:**

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    Optional<BookEntity> bookEntity = findBookByTitleIgnoreCase(bookRequestDTO.title());
    if (bookEntity.isPresent()) {
        throw new IllegalArgumentException("Book Already Exists: " + bookRequestDTO.title());
    }
    Set<AuthorEntity> authorEntities = new HashSet<>();
    for(Author author : bookRequestDTO.authors()){
        authorEntities.add(authorService.findOrCreateAuthor(author.getFirstName(),author.getLastName()));
    }
    saveBook(BookFactory.createBook(bookRequestDTO.title(), authorEntities));
}
```

**First Attempt:**

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    checkIfBookExists(bookRequestDTO);
    Set<AuthorEntity> authorEntities = extractAuthorEntities(bookRequestDTO);
    saveBook(BookFactory.createBook(bookRequestDTO.title(), authorEntities));
}

public Set<AuthorEntity> extractAuthorEntities(BookRequestDTO bookRequestDTO){
    Set<AuthorEntity> authorEntities = new HashSet<>();
    for(Author author : bookRequestDTO.authors()){
        authorEntities.add(authorService.findOrCreateAuthor(author.getFirstName(),author.getLastName()));
    }
    return authorEntities;
}

public Boolean checkIfBookExists(BookRequestDTO bookRequestDTO){
    Optional<BookEntity> bookEntity = findBookByTitleIgnoreCase(bookRequestDTO.title());
    if (bookEntity.isPresent()) {
        throw new IllegalArgumentException("Book Already Exists: " + bookRequestDTO.title());
    }
    return false;
}
```

**Issues Identified During Code Review**

#### 1. Misleading Method Signature

**Problem:**

```java
public Boolean checkIfBookExists(BookRequestDTO bookRequestDTO){
    // ...
    return false;  // Only returns false, never true
}
```

The method name `checkIfBookExists` suggests it returns a boolean answer to "does this exist?" but it actually *validates* that the book doesn't exist. It can only return `false` because if the book exists, it throws an exception.

**Root Cause:** Confusion between checking (query) vs validating (command) operations.

#### 2. Incorrect Access Modifiers

**Problem:**

```java
public Set<AuthorEntity> extractAuthorEntities(...)
public Boolean checkIfBookExists(...)
```

These are private implementation details of `createNewBook()` and shouldn't be exposed as part of the public API. Making them `public` allows other classes to call them directly, which:

- Exposes internal implementation
- Makes refactoring harder
- Creates unintended coupling

#### 3. Minor Formatting Inconsistency

```java
public Set<AuthorEntity> extractAuthorEntities(BookRequestDTO bookRequestDTO){
    // Missing space before opening brace
```

**Final Implementation**

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    validateBookDoesNotExist(bookRequestDTO);
    Set<AuthorEntity> authorEntities = extractAuthorEntities(bookRequestDTO);
    saveBook(BookFactory.createBook(bookRequestDTO.title(), authorEntities));
}

private void validateBookDoesNotExist(BookRequestDTO bookRequestDTO) {
    findBookByTitleIgnoreCase(bookRequestDTO.title())
        .ifPresent(book -> {
            throw new IllegalArgumentException("Book Already Exists: " + bookRequestDTO.title());
        });
}

private Set<AuthorEntity> extractAuthorEntities(BookRequestDTO bookRequestDTO) {
    Set<AuthorEntity> authorEntities = new HashSet<>();
    for (Author author : bookRequestDTO.authors()) {
        authorEntities.add(
            authorService.findOrCreateAuthor(author.getFirstName(), author.getLastName())
        );
    }
    return authorEntities;
}
```

**Changes in Final Version**

1. **Renamed Method**: `checkIfBookExists()` → `validateBookDoesNotExist()`
   - Name now accurately reflects that it's a validation (throws on failure)
   - Return type changed from `Boolean` to `void`
2. **Simplified Validation Logic**:
   - Uses `Optional.ifPresent()` for cleaner, more idiomatic Java
   - Removes unreachable `return false` statement
3. **Fixed Access Modifiers**: Changed both helper methods from `public` to `private`
4. **Added Spacing**: Consistent formatting with space before opening braces

**Key Learnings**

- **Method naming matters**: A method that validates should say "validate", not "check"
- **Return types should reflect reality**: If a method can only return one value, it probably shouldn't return anything
- **Encapsulation**: Helper methods used only within a class should be private
- **Query vs Command**: Methods either ask questions (return values) or do things (void/throw), rarely both

**Benefits**

- **Readability**: `createNewBook()` now reads like a high-level workflow
- **Testability**: Each helper method can be tested independently
- **Maintainability**: Business rules are isolated in focused methods
- **Encapsulation**: Implementation details are properly hidden

**Files Modified**

- `BookService.java` - extracted and refined helper methods

------

## Git Commit Message

```
refactor: extract helper methods from BookService.createNewBook()

- Extract validateBookDoesNotExist() from inline validation logic
- Extract extractAuthorEntities() from inline author processing
- Use Optional.ifPresent() for cleaner validation
- Mark helper methods as private to hide implementation details
- Simplify createNewBook() to read as high-level workflow

This improves readability by separating concerns and makes each
component easier to test and maintain independently.
```