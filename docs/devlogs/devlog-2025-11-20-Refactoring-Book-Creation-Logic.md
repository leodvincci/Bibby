👨‍💻 Dev Log: Refactoring Book Creation Logic

**Date:** 2025-11-21
 **Author:** @leodvincci
 **Commit:** [aa3c504](https://github.com/leodvincci/Bibby/commit/aa3c504a2ec0a42dd99b8c112a0a22d78925fe3c)
 **PR:** #46 — Refactor/extracting logic from create-new-book method

------

## 📝 Context

The `BookService` had evolved into a god-class—handling DTO parsing, author lookup, author creation, book instantiation, validation, and persistence all within a single method. This created several problems:

- **Tight coupling** between book and author concerns
- **Poor testability** due to mixed responsibilities
- **Unclear boundaries** between domain logic and orchestration
- **Low cohesion** making it harder to reason about behavior

As the system grows, this pattern would only compound. This micro-refactor was about giving each responsibility a proper home and establishing clearer service boundaries.

Additionally, the CLI had artificial `Thread.sleep()` calls scattered throughout the book-addition flow—decorative pauses that made the interface feel sluggish. With this refactor, I took the opportunity to remove them entirely.

------

## 🛠 Technical Changes

### 1. **Extracted Author Logic → `AuthorService`**

Created a dedicated service to encapsulate author-related operations:

```java
public class AuthorService {
    public AuthorEntity findByAuthorFirstNameLastName(String firstName, String lastName) { ... }
    public AuthorEntity createNewAuthor(String firstName, String lastName) { ... }
}
```

**Responsibilities:**

- Finding authors by name
- Creating and persisting new authors
- Owning author-specific business rules

This moves author management out of `BookService` entirely.

------

### 2. **Introduced `BookFactory` for Object Construction**

Centralized book instantiation into a factory class:

```java
public class BookFactory {
    public static BookEntity createBook(String title, AuthorEntity author) {
        BookEntity bookEntity = new BookEntity();
        bookEntity.setTitle(title);
        bookEntity.setAuthors(author);
        return bookEntity;
    }
}
```

**Purpose:**
 Standardizes how `BookEntity` objects are created, removing construction logic from the service layer.

------

### 3. **Refactored `BookService.createNewBook()`**

The method is now a lean orchestrator:

**Before:**

```java
public void createNewBook(BookRequestDTO dto) {
    AuthorEntity author = authorRepository.findByFirstNameAndLastName(...);
    if (author == null) {
        author = new AuthorEntity(firstName, lastName);
        authorRepository.save(author);
    }
    BookEntity book = new BookEntity();
    book.setTitle(title);
    book.setAuthors(author);
    bookRepository.save(book);
}
```

**After:**

```java
@Transactional
public void createNewBook(BookRequestDTO dto) {
    BookEntity existingBook = findBookByTitleIgnoreCase(dto.title());
    AuthorEntity author = authorService.findByAuthorFirstNameLastName(dto.firstName(), dto.lastName());
    
    if (author == null) {
        author = authorService.createNewAuthor(dto.firstName(), dto.lastName());
    }
    
    if (existingBook == null) {
        BookEntity newBook = BookFactory.createBook(dto.title(), author);
        saveBook(newBook);
    }
}
```

**Changes:**

- Delegates author resolution to `AuthorService`
- Uses `BookFactory` for instantiation
- Adds duplicate-book guard with case-insensitive title check
- Introduces `saveBook()` helper method
- Reduces branching complexity

------

### 4. **Improved Dependency Management**

Made fields `final` in `BookService` to enforce immutability:

```java
private final BookRepository bookRepository;
private final AuthorService authorService;
private final AuthorRepository authorRepository;
```

This makes dependencies explicit and prevents accidental reassignment.

------

### 5. **CLI Experience Overhaul**

Removed all `Thread.sleep()` calls from `BookCommands.addBook()`:

- **10 deletions** of artificial delays (ranging from 1–3.8 seconds)
- Result: **Snappier, real-time responsiveness**
- The CLI now feels like a production tool, not a proof-of-concept demo

------

## 🔍 Critical Analysis & Reflection

While this refactor moves in the right direction, there are **unfinished edges** that need attention:

### ❌ **Incomplete Extraction**

`BookService` still directly depends on `AuthorRepository`. After introducing `AuthorService`, the repository should be removed from `BookService` entirely. The service boundary isn't fully respected yet.

**Fix:**

```java
public BookService(BookRepository bookRepository, AuthorService authorService) {
    this.bookRepository = bookRepository;
    this.authorService = authorService;
}
```

------

### ❌ **"Find or Create" Logic Still in BookService**

The pattern of checking `if (author == null)` and then creating belongs in `AuthorService`, not the orchestrator:

**Should be:**

```java
// In AuthorService
public AuthorEntity findOrCreateAuthor(String firstName, String lastName) {
    AuthorEntity existing = findByAuthorFirstNameLastName(firstName, lastName);
    return (existing != null) ? existing : createNewAuthor(firstName, lastName);
}
```

Then `BookService` just calls `authorService.findOrCreateAuthor(...)`.

------

### ❌ **Static Factory Violates Spring Principles**

`BookFactory` uses static methods, which:

- Makes testing harder (can't mock)
- Bypasses Spring's dependency injection
- Limits future extensibility

**Should be:**

```java
@Component
public class BookFactory {
    public BookEntity createBook(String title, AuthorEntity author) { ... }
}
```

Then inject it like any other dependency.

------

### ❌ **Silent Failure on Duplicate Books**

The original code printed "Book Already Exists." Now it just... does nothing. Options:

1. **Throw an exception** (`BookAlreadyExistsException`)
2. **Return a result object** indicating success/failure
3. **Log the attempt** for debugging

Silently ignoring duplicates is a hidden bug waiting to happen.

------

### ❌ **Inconsistent Method Naming**

- `findByAuthorFirstNameLastName()` → too verbose; should be `findByName()`
- `findBookByTitleIgnoreCase()` vs `findBookByTitle()` → case sensitivity should be at the repository layer, not exposed in naming

------

## 🚀 Outcome

**What Improved:** ✅ Service responsibilities are now clearer
 ✅ Author logic is encapsulated in `AuthorService`
 ✅ Book creation follows a factory pattern
 ✅ CLI is significantly faster and more responsive
 ✅ Code is closer to being unit-testable

**What's Still Needed:**

- Complete the extraction: remove `AuthorRepository` from `BookService`
- Move "find or create" pattern into `AuthorService`
- Convert `BookFactory` to a Spring-managed component
- Add proper error handling for duplicate books
- Standardize method naming conventions

------

## 📚 Lessons for the Textbook

This refactor demonstrates the **evolution from procedural to domain-driven design**:

1. **God-method → Service orchestration** (separation of concerns)
2. **Direct instantiation → Factory pattern** (centralized creation logic)
3. **Mixed responsibilities → Single Responsibility Principle** (each class does one thing)

However, it also shows that **refactoring is iterative**. This commit is progress, not perfection. The next micro-slice should complete the extraction and tighten the service boundaries.

For readers: Refactoring isn't about getting it perfect in one shot. It's about making intentional improvements, reflecting on what's left, and continuing to evolve the design.

------

## 🔄 Next Steps

- [ ] Remove `AuthorRepository` dependency from `BookService`
- [ ] Add `findOrCreateAuthor()` method to `AuthorService`
- [ ] Convert `BookFactory` to a Spring `@Component`
- [ ] Implement exception handling for duplicate books
- [ ] Write unit tests for the new service methods
- [ ] Document the service boundaries in architecture diagrams

------

**Status:** ✅ Merged
 **Branch:** `refactor/extracting-logic-create-new-book`
 **Files Changed:** 4 (+54, -21)