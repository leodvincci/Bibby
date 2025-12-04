# Devlog: ISBN Search Feature

**Date:** 2025-12-04  
**Focus:** Adding ISBN-based book lookup to the CLI search options  
**Commit Type:** `feat(cli)`

---

## Summary

Extended the book search functionality with ISBN lookup. Users can now search their library by ISBN in addition to the existing title and author search options.

---

## What Changed

### New Search Option in Menu

**CliPromptService.java:**
```java
options.put("""
                ISBN                 (Search by ISBN)""", "isbn");
```

The search menu now shows:
```
? How would you like to search?
> Show all books       (View the complete library)
  ISBN                 (Search by ISBN)
  Title or keyword     (Search by words in the title)
  Author               (Find books by a specific author)
```

### Search Handler

**BookCommandLine.java:**
```java
}else if(searchType.equalsIgnoreCase("isbn")){
    searchByIsbn();
}

private void searchByIsbn() {
    System.out.println("\n\u001B[95mSearch by ISBN");
    String isbn = cliPrompt.promptForIsbnScan();
    BookEntity bookEntity = bookService.findBookByIsbn(isbn);
    if(bookEntity == null){
        System.out.println("\n</>: No book found with ISBN: " + isbn + "\n");
    }else{
        System.out.println("\n</>: Book found: " + bookEntity.getTitle() + "\n");
    }
}
```

### Service and Repository Layer

**BookService.java:**
```java
public BookEntity findBookByIsbn(String isbn) {
    return bookRepository.findByIsbn(isbn);
}
```

**BookRepository.java:**
```java
BookEntity findByIsbn(String isbn);
```

Spring Data JPA derives the query automatically from the method name—no `@Query` annotation needed.

---

## User Flow

```
bibby> book search

? How would you like to search?
> ISBN                 (Search by ISBN)

Search by ISBN
ISBN Number:_ 9780134685991

</>: Book found: Effective Java
```

Or if not found:
```
</>: No book found with ISBN: 9781234567890
```

---

## Technical Notes

### Reusing `promptForIsbnScan()`

The ISBN input prompt already existed for the scan feature. Reused it here rather than creating a separate prompt—same input shape, same validation needs.

### Spring Data Query Derivation

```java
BookEntity findByIsbn(String isbn);
```

Spring Data JPA parses the method name:
- `findBy` → SELECT query
- `Isbn` → WHERE isbn = ?

No implementation needed. The framework generates:
```sql
SELECT * FROM book_entity WHERE isbn = ?
```

### Return Type: Entity vs Optional

Chose to return `BookEntity` directly (nullable) rather than `Optional<BookEntity>`.

**Trade-off:**
- Direct return: Simpler null check in caller
- Optional: More explicit about possible absence

Current pattern matches existing `findBookByTitle()` behavior in the codebase. Consistency wins here.

---

## Struggle Journal

### No Struggles This Time

This was a straightforward vertical slice:
1. Add menu option
2. Add search handler
3. Add service method
4. Add repository method

Each layer is thin and follows established patterns. The kind of feature that goes in cleanly when the architecture is working well.

---

## Interview Talking Points

### "How does findByIsbn work without an implementation?"

> Spring Data JPA uses query derivation. It parses the method name—`findBy` tells it to do a SELECT, and `Isbn` tells it to filter by the isbn field. The framework generates the query at runtime. For simple lookups, you don't need to write any SQL or JPQL.

### "Why return the entity instead of Optional?"

> For consistency with the existing codebase—`findBookByTitle()` also returns a nullable entity. If I were starting fresh, I'd probably use Optional everywhere to make null handling explicit. But matching the existing pattern reduces cognitive load when working across the codebase.

---

## What's Next

1. **Richer search results**: Show more than just title (authors, shelf location, availability)
2. **ISBN validation**: Validate format before querying (ISBN-10 vs ISBN-13)
3. **Partial ISBN search**: Support lookup by partial ISBN (last 4 digits, etc.)

---

## Files Changed

| File | Changes |
|------|---------|
| `BookCommandLine.java` | Added ISBN branch and `searchByIsbn()` method |
| `CliPromptService.java` | Added ISBN option to search menu |
| `BookService.java` | Added `findBookByIsbn()` method |
| `BookRepository.java` | Added `findByIsbn()` query method |

---

## Feature Status

```
[✓] ISBN search option in menu
[✓] ISBN input prompt
[✓] Database lookup by ISBN
[✓] Found/not-found feedback
[ ] Rich result display (authors, location)
[ ] ISBN format validation
[ ] Partial ISBN matching
```

Clean vertical slice—search by ISBN works end-to-end.
