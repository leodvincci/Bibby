# Dev Log: Expanding Value Objects Across Domain Model

**Date:** December 9, 2025  
**Focus Area:** Domain Model Strengthening - Comprehensive Value Objects  
**Related Modules:** Book, Author, Shelf

---

## Summary

Extended the value object pattern from the previous `BookId` work to cover more of the domain model. Introduced
`AuthorId`, `Title`, and `Isbn` value objects, and replaced the problematic `List<String>` author representation with
`List<AuthorRef>`. Also fixed a Spring annotation placement issue on the `AuthorFacade`.

---

## What Changed

### 1. AuthorId Value Object

Applied the same pattern from `BookId` to the Author module:

```java
// Before
private Long authorId;
public Long getAuthorId() { return authorId; }

// After
private AuthorId authorId;
public AuthorId getAuthorId() { return authorId; }
```

Updated in:

- `Author` domain class
- `AuthorFactory.createDomain()`
- `AuthorMapper.toDomain()` and `toDTO()` methods
- `AuthorRepositoryImpl.createAuthor()`

### 2. Title and Isbn Value Objects in Book

The `Book` domain class now wraps two more primitives:

```java
// Before
private String title;
private String isbn;

// After
private Title title;
private Isbn isbn;
```

The mapper now handles wrapping at the boundary:

```java
book.setBookId(new BookId(bookDTO.id()));
book.setTitle(new Title(bookDTO.title()));
book.setIsbn(new Isbn(bookDTO.isbn()));
```

And unwrapping when going back to entity:

```java
bookEntity.setBookId(book.getBookId().getId());
bookEntity.setTitle(book.getTitle().title());
bookEntity.setIsbn(book.getIsbn().isbn);
```

### 3. List\<String\> → List\<AuthorRef\>

This was the most significant conceptual change. Previously, authors on a Book were just concatenated name strings:

```java
// Before
private List<String> authors;  // ["John Smith", "Jane Doe"]

// After
private List<AuthorRef> authors;  // [AuthorRef(firstName, lastName), ...]
```

The mapper signature changed accordingly:

```java
// Before
public Book toDomain(BookEntity e, Set<AuthorDTO> authorDTOs, ShelfEntity shelfDTO)

// After  
public Book toDomain(BookEntity e, Set<AuthorRef> authorRefs, ShelfEntity shelfDTO)
```

And author extraction no longer does string splitting:

```java
// Before - fragile string parsing
for (String author : book.getAuthors()) {
    String[] names = author.split(" ");
    String firstName = names[0];
    String lastName = names.length > 1 ? names[1] : "";
    // ...
}

// After - proper typed access
for (AuthorRef author : book.getAuthors()) {
    String firstName = author.getAuthorFirstName();
    String lastName = author.getAuthorLastName();
    // ...
}
```

### 4. @Component Annotation Fix

Moved `@Component` from the interface to the implementation:

```java
// Before (wrong - annotation on interface)
@Component
public interface AuthorFacade { ... }

// After (correct - annotation on implementation)
public interface AuthorFacade { ... }

@Component
public class AuthorFacadeImpl implements AuthorFacade { ... }
```

### 5. Logging Configuration

Quieted the verbose Spring/Hibernate logging:

```properties
logging.level.root=OFF
logging.level.org.springframework=OFF
logging.level.org.hibernate=OFF
```

---

## Why This Matters

### Eliminating String Manipulation in Domain Logic

The `List<String>` author representation was a code smell hiding in plain sight. Problems:

1. **Lost structure** - First and last name were concatenated, then split apart elsewhere
2. **Fragile parsing** - `split(" ")` breaks on middle names, suffixes, single names
3. **No type safety** - A string could be anything; `AuthorRef` is explicitly an author reference
4. **Duplicated logic** - Every consumer had to know the "firstName lastName" convention

`AuthorRef` preserves the structure throughout the domain, eliminating round-trip serialization.

### Value Objects as Domain Documentation

Each value object communicates intent:

- `BookId` - "This is a book identifier, not just any Long"
- `AuthorId` - "This is an author identifier"
- `Title` - "This is a book title with title-specific semantics"
- `Isbn` - "This is an ISBN with potential validation rules"

Even before adding validation, the types make the code self-documenting.

### Interface vs Implementation Annotations

`@Component` on an interface is technically valid but semantically wrong:

- Interfaces define contracts, not Spring beans
- The implementation is what gets instantiated and injected
- Placing `@Component` on the interface obscures which implementation is active

This matters more when you have multiple implementations or use `@Qualifier`.

---

## Struggles & Decisions

### Accessor Naming Inconsistency

Noticed inconsistent accessor patterns in the new value objects:

```java
book.getBookId().getId()      // BookId uses getId()
book.getTitle().title()       // Title uses record-style accessor
book.getIsbn().isbn           // Isbn uses direct field access (record)
author.getAuthorId().id()     // AuthorId uses record-style accessor
```

This suggests the value objects were created at different times or with different patterns. Should standardize -
probably all as records with consistent accessors.

**TODO:** Audit value object implementations for consistency.

### AuthorRef vs AuthorDTO vs Author

The domain now has three author-related types:

- `Author` - Full domain entity with ID, names, potentially more
- `AuthorRef` - Lightweight reference for cross-module communication
- `AuthorDTO` - Data transfer at API/infrastructure boundaries

This feels right but needs clear documentation on when to use each:

- `Author` - Within the Author module's domain layer
- `AuthorRef` - When Book module needs to reference authors without depending on Author internals
- `AuthorDTO` - At facade boundaries and external APIs

### ShelfDomainRepositoryImpl Loop vs Stream

Changed from stream to explicit loop:

```java
// Before
List<Long> books = books.stream().map(Book::getBookId).toList();

// After
List<Long> books = new ArrayList<>();
for(Book book : books){
    books.add(book.getBookId().getId());
}
```

The stream version no longer works because `getBookId()` returns `BookId`, not `Long`. Could have done:

```java
books.stream().map(b -> b.getBookId().getId()).toList();
```

The explicit loop is arguably clearer here. Not a strong opinion either way.

---

## Testing Considerations

### Value Object Equality

Need to verify each value object has proper `equals()`/`hashCode()`:

- [ ] `AuthorId` equality
- [ ] `Title` equality
- [ ] `Isbn` equality
- [ ] All implemented as records? (Records get this for free)

### AuthorRef Behavior

- [ ] `AuthorRef` equality semantics - by ID? by name? both?
- [ ] Null handling in `getAuthorFirstName()` / `getAuthorLastName()`

### Mapper Boundary Tests

The mappers are now doing more work (wrapping/unwrapping). Worth testing:

- [ ] Null handling at boundaries
- [ ] Round-trip: Domain → Entity → Domain preserves values
- [ ] DTO → Domain → DTO preserves values

---

## Patterns Emerging

### The Wrap/Unwrap Boundary

A clear pattern is emerging in the mappers:

```
Infrastructure (primitives) ←→ Mapper ←→ Domain (value objects)
```

The mapper is the translation layer where:

- Inbound: primitives get wrapped in value objects
- Outbound: value objects get unwrapped to primitives

This is the right place for this logic - keeps domain pure, keeps infrastructure ignorant of domain types.

### Consistency Enables Automation

Once all IDs are value objects with consistent accessors, could potentially:

- Generate mappers with tools like MapStruct
- Create generic repository base classes
- Build consistent error handling around ID validation

The upfront investment in consistency pays dividends.

---

## Learnings

1. **String concatenation is a red flag** - When you're concatenating values to store them and splitting to retrieve
   them, you've lost structure. The data model should preserve structure.

2. **Value objects compound** - Once you have `BookId`, adding `AuthorId`, `Title`, `Isbn` feels natural. The pattern
   creates momentum.

3. **Annotations belong on implementations** - Interfaces define contracts; implementations define beans. Keep Spring
   concerns out of your interfaces.

4. **Mappers absorb complexity** - All the wrapping/unwrapping lives in one place. The domain stays clean, the entities
   stay simple, the mapper handles the translation.

5. **Inconsistency surfaces during refactoring** - The accessor naming inconsistency (`getId()` vs `id()` vs direct
   field) only became visible when touching all these files at once. Good time to standardize.

---

## Next Steps

- [ ] Audit value object implementations for consistency (all records? same accessor pattern?)
- [ ] Document AuthorRef vs AuthorDTO vs Author usage guidelines
- [ ] Consider `ShelfId` value object for full consistency
- [ ] Add validation logic to `Isbn` (checksum validation?)
- [ ] Add validation logic to `Title` (non-blank?)
- [ ] Write unit tests for value object equality
- [ ] Clean up the commented-out code in BookMapper

---

## Interview Angle

**Story:** "After introducing BookId, I saw the pattern should extend across the domain. The most interesting change was
replacing `List<String>` for authors with `List<AuthorRef>`. We were concatenating names with spaces, then splitting
them apart elsewhere - classic 'stringly typed' code. The AuthorRef type preserves structure and eliminates fragile
string parsing."

**Follow-up depth:** Can discuss the trade-offs of value objects (more classes, more mapping code) vs benefits (type
safety, validation encapsulation, self-documenting code). Can also discuss the facade pattern and how AuthorRef enables
cross-module references without tight coupling.

**Architecture insight:** "The mappers became the natural boundary for wrapping and unwrapping. Domain stays pure with
value objects, infrastructure stays simple with primitives, and the mapper handles translation. This separation makes
each layer independently testable."
