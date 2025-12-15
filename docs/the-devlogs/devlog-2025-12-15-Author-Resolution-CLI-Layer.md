# Devlog: Author Resolution at CLI Layer During Book Import

**Date:** December 15, 2025  
**Module:** CLI / Cataloging (Book + Author)  
**Type:** Refactor  
**PR:** #116  
**Status:** 🟡 In Progress (cleanup pending)

---

## 1. High-Level Summary

- **Moved author resolution logic from infrastructure to CLI layer** — enabling interactive duplicate handling during book import
- **Changed `createBookFromMetaData()` signature** — now accepts `List<Long> authorIds` instead of resolving authors internally
- **Added `getAuthorById()` method** across AuthorFacade, AuthorRepository, and implementations
- **Introduced `createAuthorsFromMetaData()`** in BookCommands for explicit author handling with user prompts
- **Temporarily disabled multi-book scan** pending refactor for new author resolution flow

---

## 2. The Underlying Problem or Friction

When importing a book via ISBN scan, the Google Books API returns author names as strings (e.g., "Sam Newman"). The system needs to either:

1. Find an existing author with that name, OR
2. Create a new author record

The complication: **What if multiple authors share the same name?** The user needs to choose.

**The original design flaw:**

Author resolution was buried in `BookMapper.toEntityFromBookMetaDataResponse()`:

```java
// In BookMapper (infrastructure layer)
public BookEntity toEntityFromBookMetaDataResponse(BookMetaDataResponse response, String isbn, Long shelfId) {
    authorFacade.createAuthorsIfNotExist(response.authors());  // ❌ No user interaction possible
    bookEntity.setAuthors(authorFacade.getAuthorsById(response.authors()));
    // ...
}
```

This violated separation of concerns:
- **Mapper** = data transformation (no side effects, no user interaction)
- **CLI** = user interaction (prompts, confirmations, selections)

The mapper was silently creating authors without giving the user any choice about duplicates.

---

## 3. The Behavior Change

### Before

```
User scans ISBN
    → Google Books returns "Sam Newman"
    → BookMapper automatically creates new author (no check for duplicates)
    → Book saved with potentially duplicate author
```

### After

```
User scans ISBN
    → Google Books returns "Sam Newman"
    → CLI checks: "Does Sam Newman exist?"
        → If yes: "Multiple authors found. Select one or create new:"
            [1] Sam Newman (ID: 42) - 3 books
            [2] Sam Newman (ID: 87) - 1 book
            [0] Create new author
        → If no: Create new author automatically
    → Book saved with correct author ID
```

### API Signature Change

```java
// Before
void createBookFromMetaData(BookMetaDataResponse response, String isbn, Long shelfId);

// After
void createBookFromMetaData(BookMetaDataResponse response, List<Long> authorIds, String isbn, Long shelfId);
```

The caller (CLI) is now responsible for resolving authors first.

---

## 4. The Architectural Meaning

This refactor aligns with **hexagonal architecture** principles:

### Responsibility Shift

| Layer | Before | After |
|-------|--------|-------|
| CLI (BookCommands) | Just called facade | Resolves authors, handles user prompts |
| Facade (BookFacade) | Delegated to repository | Receives pre-resolved author IDs |
| Mapper (BookMapper) | Called AuthorFacade, created authors | Pure transformation, no side effects |
| Repository | Persisted with inline author creation | Persists with provided author IDs |

### Dependency Direction

```
Before:
CLI → BookFacade → BookMapper → AuthorFacade
                        ↑
                   User interaction buried here (wrong!)

After:
CLI → AuthorFacade (resolve authors with user prompts)
  ↓
CLI → BookFacade (create book with resolved IDs)
  ↓
BookMapper (pure transformation)
```

### Benefits

1. **Testability** — BookMapper can be unit tested without mocking user prompts
2. **Reusability** — Same book creation logic works for web API (no prompts needed)
3. **Clarity** — Each layer has one job
4. **Flexibility** — Different UIs can implement different duplicate handling strategies

---

## 5. The Flow (Detailed)

```
BookCommands.scanBook()
    │
    ├── cliPrompt.promptForIsbnScan()
    │       → User scans barcode or enters ISBN
    │
    ├── bookFacade.findBookMetaDataByIsbn(isbn)
    │       → Calls Google Books API
    │       → Returns BookMetaDataResponse with author names
    │
    ├── createAuthorsFromMetaData(response.authors())
    │       │
    │       └── For each author name:
    │               ├── Parse "Sam Newman" → firstName: "Sam", lastName: "Newman"
    │               ├── authorFacade.authorExistFirstNameLastName(first, last)
    │               │       → If exists: prompt user to select or create new
    │               │       → If not exists: create new author
    │               └── Collect author IDs
    │
    ├── cliPrompt.promptBookConfirmation()
    │       → "Add 'Building Microservices' to library? [Y/n]"
    │
    └── bookFacade.createBookFromMetaData(response, authorIds, isbn, shelfId)
            │
            └── bookDomainRepository.createBookFromMetaData(...)
                    │
                    └── bookJpaRepository.save(bookEntity)
```

---

## 6. Key Code Changes

### New Method: `createAuthorsFromMetaData()`

```java
public List<Long> createAuthorsFromMetaData(List<String> authorNames) {
    List<Long> authorIds = new ArrayList<>();
    
    for (String name : authorNames) {
        // Parse name (simple first/last split)
        String[] parts = name.trim().split(" ");
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[parts.length - 1] : "";
        
        AuthorDTO authorDTO = new AuthorDTO(null, firstName, lastName);
        
        if (authorFacade.authorExistFirstNameLastName(firstName, lastName)) {
            // Duplicate found - prompt user
            Long selectedId = cliPrompt.promptMultipleAuthorConfirmation(authorDTO);
            if (selectedId == 0) {  // Magic number for "create new"
                authorIds.add(authorFacade.saveAuthor(authorDTO).id());
            } else {
                authorIds.add(selectedId);
            }
        } else {
            // No duplicate - create new
            authorIds.add(authorFacade.saveAuthor(authorDTO).id());
        }
    }
    
    return authorIds;
}
```

### New Method: `getAuthorById()`

Added to `AuthorFacade`, `AuthorRepository`, and implementations:

```java
// AuthorFacade.java
AuthorDTO getAuthorById(Long authId);

// AuthorRepositoryImpl.java
@Override
public AuthorEntity getAuthorById(Long authId) {
    return authorJpaRepository.findById(authId)
        .orElseThrow(() -> new AuthorNotFoundException(authId));  // TODO: Currently uses unsafe .get()
}
```

### Signature Change Propagation

```java
// BookFacade.java
void createBookFromMetaData(BookMetaDataResponse response, List<Long> authorIds, String isbn, Long shelfId);

// BookFacadeAdapter.java
@Override
public void createBookFromMetaData(BookMetaDataResponse response, List<Long> authorIds, String isbn, Long shelfId) {
    bookDomainRepository.createBookFromMetaData(response, authorIds, isbn, shelfId);
}

// BookDomainRepository.java
void createBookFromMetaData(BookMetaDataResponse response, List<Long> authorIds, String isbn, Long shelfId);
```

---

## 7. Known Issues (To Fix Before Merge)

### Issue 1: Commented-Out Code
```java
// if (multi) multiBookScan();  // ❌ Should delete or throw exception
```
**Fix:** Delete commented code, use `UnsupportedOperationException` if feature disabled.

### Issue 2: Magic Number
```java
if (authorId == 0) {  // ❌ Magic number for "create new"
```
**Fix:** Use `Optional<Long>` from prompt, or named constant.

### Issue 3: Unsafe `.get()`
```java
return authorJpaRepository.findById(authId).get();  // ❌ Throws if not found
```
**Fix:** Return `Optional` or throw domain exception.

### Issue 4: Empty Stub
```java
public Optional<AuthorDTO> getByFirstNameAndLastNameDTO(...) {
    return Optional.empty();  // ❌ Always returns empty
}
```
**Fix:** Implement or remove.

### Issue 5: Console Output in Repository
```java
// In BookDomainRepositoryImpl
System.out.println("Book Successfully Imported");  // ❌ Should be in CLI
```
**Fix:** Move to `BookCommands`.

---

## 8. Talking Points (Interview / Portfolio)

- **Improved separation of concerns** by moving user-interactive logic from infrastructure layer to presentation layer, enabling proper duplicate handling during book import

- **Refactored method signatures to make dependencies explicit** — callers now provide resolved author IDs rather than relying on hidden side effects in mappers

- **Applied hexagonal architecture principles** by ensuring mappers perform pure data transformation without calling external services or triggering user prompts

- **Designed for multiple UI contexts** — the same book creation logic can now work for CLI (with prompts) or web API (with different duplicate strategies) without modification

- **Identified and documented technical debt** with actionable micro-slice specs for each cleanup item

---

## 9. Potential Interview Questions

### Design Rationale

1. Why did you move author resolution from the mapper to the CLI layer?
2. What problems did the original design create for user interaction?
3. How does passing `List<Long> authorIds` instead of resolving internally improve the design?

### Architecture

4. How does this change align with hexagonal architecture?
5. What would happen if a web API needed different duplicate handling than the CLI?
6. Why shouldn't mappers call facades or perform side effects?

### Implementation

7. How do you handle the case where multiple authors share the same name?
8. What's wrong with using a magic number like `0` to mean "create new"?
9. Why is `.get()` on an Optional dangerous without checking `isPresent()`?

### Testing

10. How would you unit test the author resolution logic?
11. What mocks would you need for `createAuthorsFromMetaData()`?
12. How does moving logic to CLI affect testability of the mapper?

### Trade-offs

13. What are the downsides of making the caller responsible for author resolution?
14. How would you handle author resolution in a batch import (no user prompts)?
15. What edge cases does simple first/last name parsing miss?

---

## 10. Areas for Deeper Learning

### Command Pattern / CLI Design

**Why relevant:** The CLI is accumulating logic for user flows. Understanding command patterns and CLI UX would help structure this better.

**Resources:**
- *The Pragmatic Programmer* — Section on Domain Languages
- Picocli documentation (alternative to Spring Shell)

### Optional and Null Safety in Java

**Why relevant:** The unsafe `.get()` call and magic number issues both relate to proper Optional usage.

**Resources:**
- *Effective Java* (3rd Ed) — Item 55: Return optionals judiciously
- [Oracle's Optional Guide](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html)

### Name Parsing and Internationalization

**Why relevant:** The simple first/last split fails for many real-world names.

**Resources:**
- [Falsehoods Programmers Believe About Names](https://www.kalzumeus.com/2010/06/17/falsehoods-programmers-believe-about-names/)
- Name parsing libraries (e.g., `nameparser`)

### Separation of Concerns in Layered Architecture

**Why relevant:** This refactor was fundamentally about putting logic in the right layer.

**Resources:**
- *Clean Architecture* — Robert C. Martin
- *Implementing Domain-Driven Design* — Vaughn Vernon (Chapter on Application Services)

---

## 11. Cleanup Micro-Slices (Prioritized)

| # | Slice | Priority | Status |
|---|-------|----------|--------|
| 1 | Remove commented-out code | P1 | ⬜ TODO |
| 2 | Fix magic number (use Optional) | P2 | ⬜ TODO |
| 3 | Fix unsafe `.get()` | P2 | ⬜ TODO |
| 4 | Remove empty method stub | P3 | ⬜ TODO |
| 5 | Document name parsing limits | P4 | ⬜ TODO |
| 6 | Extract `resolveOrCreateAuthor()` | P3 | ⬜ TODO |
| 7 | Move console output to CLI | P2 | ⬜ TODO |
| 8 | Standardize logging | P4 | ⬜ TODO |

Complete 1-4 before merging PR #116.

---

## 12. Commit History (Planned)

```
fix: remove commented-out code from author resolution refactor
fix(author): handle missing author in getAuthorById safely
fix(author): remove unused getByFirstNameAndLastNameDTO stub
refactor(cli): replace magic number with Optional for author selection
refactor(book): move console output from repository to CLI layer
refactor(cli): extract resolveOrCreateAuthor helper method
docs(cli): document name parsing limitations in author import
refactor: standardize logging across codebase
```

---

## 13. Final Commit Message (After Cleanup)

```
refactor(book): handle author resolution at CLI layer during import

Move author creation/resolution logic from BookMapper to BookCommands
to enable interactive duplicate handling and explicit author verification
before book creation.

Changes:
- Add createAuthorsFromMetaData() in BookCommands for explicit author handling
- Update createBookFromMetaData() signature to accept List<Long> authorIds
- Add getAuthorById() method to AuthorFacade, AuthorRepository, and implementations
- Add AuthorMapper.toDTOFromEntity() helper method
- Disable multi-book scan until refactored for new flow

Architecture:
- Mappers now perform pure transformation (no AuthorFacade calls)
- CLI layer handles user interaction for duplicate authors
- Improved testability and separation of concerns
```
