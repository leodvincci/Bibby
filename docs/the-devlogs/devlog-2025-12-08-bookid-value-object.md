# Dev Log: Introducing BookId Value Object

**Date:** December 8, 2024  
**Focus Area:** Domain Model Strengthening - Value Objects  
**Related Modules:** Book, Shelf

---

## Summary

Introduced `BookId` as a typed value object to replace the primitive `Long id` in the `Book` domain class. Also converted `AuthorName` from a traditional class to a Java record. These changes push the domain model toward stronger type safety and better encapsulation of identity.

---

## What Changed

### 1. BookId Value Object Introduction

Replaced the primitive `Long id` field in `Book` with a `BookId` value object:

```java
// Before
private Long id;
public Long getId() { return id; }

// After
private BookId bookId;
public BookId getBookId() { return bookId; }
```

This rippled through:
- `BookFactory.createBookDomain()` - now calls `setBookId()`
- `BookMapper` - all four mapping methods updated
- `ShelfDomainRepositoryImpl.getById()` - updated stream mapping

### 2. AuthorName Converted to Record

Simplified `AuthorName` from a traditional class with explicit constructor and fields to a Java record:

```java
// Before
public class AuthorName {
    private final String firstName;
    private final String lastName;
    
    public AuthorName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}

// After
public record AuthorName(String firstName, String lastName) {
    // toString() override retained
}
```

### 3. Service Layer Boundary Shift

`BookService.createNewBook()` now accepts a `Book` domain object instead of `BookRequestDTO`:

```java
// Before
public void createNewBook(BookRequestDTO bookDTO)

// After
public void createNewBook(Book book)
```

This pushes DTO-to-domain conversion responsibility outward (likely to the CLI/controller layer).

### 4. Dead Code Identified

Commented out `createScannedBook()` method - appears to be superseded by other scanning workflow changes. Marked for removal after confirming no remaining call sites.

---

## Why This Matters

### Primitive Obsession → Value Objects

Using `Long` for identity is a classic "primitive obsession" code smell. Problems with primitives:
- No type safety (can accidentally pass a `shelfId` where a `bookId` is expected)
- No place to attach validation logic
- No domain semantics

`BookId` as a value object enables:
- **Type safety at compile time** - can't mix up ID types
- **Validation encapsulation** - null checks, format validation live in one place
- **Future flexibility** - could switch to UUID without changing the domain API
- **Domain expressiveness** - code reads as domain concepts, not technical primitives

### Records for Immutable Value Objects

Java records are ideal for simple value objects like `AuthorName`:
- Immutability by default
- Automatic `equals()`, `hashCode()`, accessors
- Less boilerplate, same semantics
- Clear signal of intent: "this is data, not behavior"

### Domain Objects at Service Boundaries

Accepting `Book` instead of `BookRequestDTO` in `createNewBook()` follows the principle that the domain layer should speak its own language. The application service orchestrates, but operates on domain concepts.

---

## Struggles & Decisions

### The Ripple Effect

Changing from `getId()` to `getBookId()` touched more files than expected:
- Mapper methods (4 places)
- Factory
- Repository implementation
- Anywhere streaming/mapping book IDs

This is the cost of the refactoring, but also reveals how coupled the codebase was to the primitive. Worth it for the cleaner model going forward.

### What Type Should BookId Wrap?

Current implementation likely wraps `Long` internally. Future consideration: should this be a UUID for new books with database-assigned Long for persisted identity? Deferred for now - the value object provides the seam to make this change later without touching consumers.

### createScannedBook - Delete or Keep?

Commented out rather than deleted. Want to:
1. Verify no remaining call sites
2. Understand if scanning workflow has been fully migrated
3. Avoid losing the logic if I need to reference it

**TODO:** grep for usages, then delete if truly dead.

---

## Testing Considerations

Changes to `Book.equals()` and `hashCode()` now delegate to `BookId`:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Book book = (Book) o;
    return Objects.equals(bookId, book.bookId);  // Was: id
}
```

Need to verify:
- [ ] `BookId` itself has proper `equals()`/`hashCode()` implementation
- [ ] Existing tests for Book equality still pass
- [ ] Collection behavior (Sets, Maps with Book keys) unchanged

---

## Learnings

1. **Value objects create useful friction** - The compiler errors from this refactoring surfaced every place that knew about the internal identity representation. That's exactly where I want friction.

2. **Records are underused** - `AuthorName` as a record is so much cleaner. Should audit other classes that are just "bags of data" and convert where appropriate.

3. **Service boundary decisions matter** - Accepting `Book` vs `BookRequestDTO` isn't just cosmetic. It determines where mapping logic lives and how testable the service is in isolation.

4. **Comment-then-delete for uncertain code** - Rather than immediately deleting `createScannedBook`, commenting it preserves the logic while signaling "this is probably dead." Git has the history, but this makes the uncertainty visible.

---

## Next Steps

- [ ] Implement/verify `BookId` value object class with proper validation
- [ ] Delete `createScannedBook` after confirming no call sites
- [ ] Consider `ShelfId`, `AuthorId` value objects for consistency
- [ ] Add unit tests for `BookId` equality and edge cases
- [ ] Review where `BookRequestDTO` → `Book` conversion now happens

---

## Interview Angle

**Story:** "I noticed we were using primitive Longs for all our identifiers, which is a form of primitive obsession. I introduced typed value objects like BookId to get compile-time safety against mixing up different ID types. The refactoring revealed how many places in the codebase assumed the internal representation - exactly the coupling I wanted to break."

**Technical depth:** Can discuss trade-offs of identity types (Long vs UUID vs composite), where validation belongs, and how value objects enable future changes without rippling through consumers.
