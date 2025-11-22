## Devlog Entry

**Date**: 2025-11-22

### Refactor: Introduce Optional Pattern for Null-Safe Author Lookups

**Context**

The `AuthorService` was using null checks to handle cases where authors might not exist in the database. This approach is error-prone and less expressive than modern Java's `Optional` pattern. The repository layer was returning nullable `AuthorEntity` objects, requiring manual null checking throughout the service layer.

**Motivation**

- **Null safety**: Make it explicit when a method might not return a value
- **Code clarity**: Use functional programming patterns to reduce boilerplate
- **Spring Data JPA best practices**: Repository methods should return `Optional` for single results that might not exist
- **Consistency**: Align with modern Java idioms used elsewhere in Spring applications

**Changes Made**

#### 1. Updated Repository to Return Optional

**Before:**

```java
@Repository
public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {
    AuthorEntity getByFullName(String fullName);
    AuthorEntity findByFirstNameAndLastName(String firstName, String lastName);
    
    @Query("SELECT a FROM AuthorEntity a JOIN a.books b WHERE b.bookId = :bookId")
    List<AuthorEntity> findByBooks_BookId(@Param("bookId") Long bookId);
}
```

**After:**

```java
@Repository
public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {
    AuthorEntity getByFullName(String fullName);
    Optional<AuthorEntity> findByFirstNameAndLastName(String firstName, String lastName);
    
    @Query("SELECT a FROM AuthorEntity a JOIN a.books b WHERE b.bookId = :bookId")
    List<AuthorEntity> findByBooks_BookId(@Param("bookId") Long bookId);
}
```

**Impact:** The method signature now explicitly communicates that the result might be empty, forcing callers to handle that case.

#### 2. Introduced findAuthorByName() Service Method

**Added to AuthorService:**

```java
public Optional<AuthorEntity> findAuthorByName(String firstName, String lastName) {
    return authorRepository.findByFirstNameAndLastName(firstName, lastName);
}
```

**Benefits:**

- Provides cleaner, more semantic method name for service layer consumers
- Abstracts repository implementation details
- Creates extension point for future business logic (validation, caching, etc.)
- Maintains consistent Optional return type

#### 3. Refactored findOrCreateAuthor() to Use Optional Pattern

**Before:**

```java
public AuthorEntity findOrCreateAuthor(String authorFirstName, String authorLastName){
    AuthorEntity authorEntity = findByAuthorFirstNameLastName(authorFirstName, authorLastName);
    if (authorEntity == null) {
        return createNewAuthor(authorFirstName, authorLastName);
    }
    return authorEntity;
}
```

**After:**

```java
public AuthorEntity findOrCreateAuthor(String authorFirstName, String authorLastName){
    Optional<AuthorEntity> existingAuthor = findAuthorByName(authorFirstName, authorLastName);
    return existingAuthor.orElseGet(() -> createAuthor(authorFirstName, authorLastName));
}
```

**Improvements:**

- Reduced from 5 lines to 2 lines
- Eliminated manual null checking
- Used `orElseGet()` with lazy evaluation (factory only called if needed)
- More declarative: "return existing author, or get from creation"
- Self-documenting code that clearly expresses intent

#### 4. Simplified Method Naming

**Renamed methods for clarity:**

- `createNewAuthor()` → `createAuthor()` (shorter, "create" already implies new)
- `findByAuthorFirstNameLastName()` → `findAuthorByName()` (more semantic, less redundant)

**Final method:**

```java
public AuthorEntity createAuthor(String authorFirstName, String authorLastName) {
    return authorRepository.save(authorEntityFactory.createEntity(authorFirstName, authorLastName));
}
```

#### 5. Added Proper Imports

```java
import java.util.Optional;
```

Added to support the new Optional return types throughout the service.

**Code Quality Improvements**

- **Type Safety**: Compiler now enforces handling of potentially absent values
- **Expressiveness**: Code reads more like natural language
- **Modern Java**: Leverages Java 8+ functional programming features
- **Defensive Programming**: Impossible to forget null checks when using Optional

**Pattern Comparison**

**Old Pattern (Null Checking):**

```java
AuthorEntity author = repository.find(...);
if (author == null) {
    // handle missing case
}
// use author
```

**New Pattern (Optional):**

```java
Optional<AuthorEntity> author = repository.find(...);
return author.orElseGet(() -> createNew());
// or
author.ifPresent(a -> doSomething(a));
```

**Migration Path for Other Services**

This pattern can be applied to other services:

- `BookService.findBookByTitle()` → return `Optional<BookEntity>`
- `ShelfService.findShelfById()` → return `Optional<ShelfEntity>`
- Any "find or create" operations can use `orElseGet()` pattern

**Testing Benefits**

Optional makes test assertions clearer:

```java
// Before
AuthorEntity author = service.findByName("John", "Doe");
assertNull(author); // Less expressive

// After
Optional<AuthorEntity> author = service.findAuthorByName("John", "Doe");
assertTrue(author.isEmpty()); // Intent is clear
```

**Files Modified**

- `AuthorRepository.java` - changed return type to Optional
- `AuthorService.java` - refactored to use Optional pattern, renamed methods
- `AuthorEntity.java` - formatting (added newline)

**Key Learnings**

- Optional should be used for return types that might not have a value
- `orElseGet()` uses lazy evaluation (better than `orElse()` for factory methods)
- Functional patterns can significantly reduce boilerplate code
- Method naming should be semantic and clear about what they return

**Next Steps**

Consider applying this pattern to:

- Other repository methods that return single entities
- Service methods that perform lookups
- Any code with manual null checking that could benefit from Optional

------

## Git Commit Message

```
refactor: introduce Optional pattern for null-safe author lookups

- Change AuthorRepository.findByFirstNameAndLastName() to return Optional
- Add AuthorService.findAuthorByName() wrapper method
- Refactor findOrCreateAuthor() to use Optional.orElseGet() pattern
- Rename createNewAuthor() to createAuthor() for clarity
- Remove manual null checking in favor of Optional idioms

This change improves null safety by making potentially absent values
explicit in the type system and simplifies code by using functional
programming patterns instead of manual null checks.
```