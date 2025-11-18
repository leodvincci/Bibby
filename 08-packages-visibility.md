# Section 8: Packages & Visibility

**Estimated Time:** 90 minutes
**Prerequisites:** Sections 1-7 (especially Section 5: Encapsulation)
**Complexity:** ⭐⭐⭐⭐ (Intermediate-Advanced)

---

## Learning Objectives

By the end of this section, you will:

1. ✅ Understand Java's package system and how it enforces architectural boundaries
2. ✅ Master the difference between `public` and package-private (default) class visibility
3. ✅ Identify and fix layering violations in the Bibby codebase
4. ✅ Learn when to use package-private to encapsulate implementation details
5. ✅ Apply the principle of "least privilege" to class and member visibility
6. ✅ Organize code using vertical slice architecture
7. ✅ Use `package-info.java` to document package contracts

---

## What Are Packages?

Packages in Java serve **two critical purposes**:

1. **Namespace management** - Prevent class name collisions
2. **Access control** - Define architectural boundaries between modules

Think of packages as **rooms in a house**:
- `public` classes are like the **living room** - anyone can enter
- Package-private classes are like **bedrooms** - only family members (same package) can enter

---

## Bibby's Package Structure

Let's examine the current package organization in your codebase:

```
com.penrose.bibby/
├── cli/                          ← Presentation layer (Spring Shell commands)
│   ├── BookCommands.java
│   ├── BookcaseCommands.java
│   ├── CustomPromptProvider.java
│   └── LoadingBar.java
│
├── library/                      ← Domain layer (core business logic)
│   ├── author/                   ← Author aggregate
│   │   ├── AuthorEntity.java
│   │   ├── Author.java          (domain model)
│   │   ├── AuthorMapper.java
│   │   ├── AuthorRepository.java
│   │   ├── AuthorService.java
│   │   └── AuthorController.java
│   │
│   ├── book/                     ← Book aggregate
│   │   ├── BookEntity.java
│   │   ├── Book.java            (domain model)
│   │   ├── BookMapper.java
│   │   ├── BookRepository.java
│   │   ├── BookService.java
│   │   ├── BookController.java
│   │   ├── BookRequestDTO.java
│   │   ├── BookSummary.java     (record)
│   │   ├── BookDetailView.java  (record)
│   │   └── BookStatus.java      (enum)
│   │
│   ├── bookcase/                 ← Bookcase aggregate
│   │   ├── BookcaseEntity.java
│   │   ├── Bookcase.java
│   │   ├── BookcaseDTO.java
│   │   ├── BookcaseRepository.java
│   │   ├── BookcaseService.java
│   │   └── BookCaseController.java
│   │
│   ├── shelf/                    ← Shelf aggregate
│   │   ├── ShelfEntity.java
│   │   ├── Shelf.java
│   │   ├── ShelfRepository.java
│   │   ├── ShelfService.java
│   │   ├── ShelfController.java
│   │   └── ShelfSummary.java    (record)
│   │
│   ├── catalog/                  ← Catalog aggregate
│   │   ├── CatalogEntity.java
│   │   ├── Catalog.java
│   │   ├── CatalogRepository.java
│   │   ├── CatalogService.java
│   │   └── CatalogController.java
│   │
│   ├── genre/                    ← Genre value object
│   │   └── Genre.java
│   │
│   └── user/                     ← User aggregate (planned)
│       └── User.java
│
└── util/                         ← Utilities
    └── StartupRunner.java
```

**This is called "Vertical Slice Architecture"** - each domain concept gets its own package containing all related classes.

### Architectural Layers

```
┌─────────────────────────────────────┐
│   Presentation Layer (cli/)         │  ← User interface
│   - Commands                         │
│   - Prompt providers                 │
└─────────────────────────────────────┘
            ↓ Uses
┌─────────────────────────────────────┐
│   Service Layer                      │  ← Business logic
│   - *Service.java                    │
│   - *Controller.java (REST API)      │
└─────────────────────────────────────┘
            ↓ Uses
┌─────────────────────────────────────┐
│   Data Access Layer                  │  ← Persistence
│   - *Repository.java                 │
│   - *Entity.java                     │
└─────────────────────────────────────┘
```

**The Golden Rule:**
**Higher layers can depend on lower layers, but NEVER the reverse.**

---

## Class-Level Access Modifiers

In Java, classes can have **two access levels**:

| Modifier          | Visibility                          | Use Case                                    |
|-------------------|-------------------------------------|---------------------------------------------|
| `public`          | Accessible from **any package**     | Public API, DTOs, services, domain models   |
| *(no modifier)*   | Accessible **only within package**  | Implementation details, entities, mappers   |

**Important:** `protected` and `private` **CANNOT** be used on top-level classes (only on nested classes).

---

## Issue #1: Package-Private Field Visibility Bug

### Current Code - Genre.java:6-7

```java
package com.penrose.bibby.library.genre;

public class Genre {

    private Long id;        // ✅ Has private modifier
    String genreName;       // ❌ MISSING private modifier!
    String genreDescription;// ❌ MISSING private modifier!

    public Genre(String genreName, String genreDescription) {
        this.genreName = genreName;
        this.genreDescription = genreDescription;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public String getGenreDescription() {
        return genreDescription;
    }

    public void setGenreDescription(String genreDescription) {
        this.genreDescription = genreDescription;
    }
}
```

### The Problem

**Fields without access modifiers have package-private visibility.**

This means any class in the `com.penrose.bibby.library.genre` package can do this:

```java
// From another class in the genre package
Genre genre = new Genre("Fiction", "Fictional works");
genre.genreName = "Mystery";  // ❌ Bypasses the setter! No validation possible!
```

**Why is this dangerous?**

1. **Breaks encapsulation** - Direct field access bypasses validation logic
2. **Makes refactoring risky** - You can't safely change field names or types
3. **Violates the "information hiding" principle** - Internal state is exposed

### The Fix

**Always explicitly declare field visibility:**

```java
package com.penrose.bibby.library.genre;

public class Genre {

    private Long id;                 // ✅ Explicit private
    private String genreName;        // ✅ Explicit private (FIXED!)
    private String genreDescription; // ✅ Explicit private (FIXED!)

    public Genre(String genreName, String genreDescription) {
        this.genreName = genreName;
        this.genreDescription = genreDescription;
    }

    // ... getters and setters remain the same ...
}
```

**Now the fields are properly encapsulated.**

---

## Issue #2: Major Architectural Violation - CLI Layer Accessing Entities

### Current Code - BookCommands.java:3-14

```java
package com.penrose.bibby.cli;

import com.penrose.bibby.library.author.Author;
import com.penrose.bibby.library.author.AuthorEntity;      // ❌ VIOLATION!
import com.penrose.bibby.library.author.AuthorService;
import com.penrose.bibby.library.book.BookController;
import com.penrose.bibby.library.book.BookEntity;          // ❌ VIOLATION!
import com.penrose.bibby.library.book.BookRequestDTO;
import com.penrose.bibby.library.book.BookService;
import com.penrose.bibby.library.bookcase.BookcaseEntity;  // ❌ VIOLATION!
import com.penrose.bibby.library.bookcase.BookcaseService;
import com.penrose.bibby.library.shelf.Shelf;
import com.penrose.bibby.library.shelf.ShelfEntity;        // ❌ VIOLATION!
import com.penrose.bibby.library.shelf.ShelfService;
```

**This is a serious architectural problem.**

### The Problem

The CLI layer (presentation) is importing **JPA Entity classes** directly.

Let's look at how these entities are being used:

#### BookCommands.java:356-364

```java
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());

String bookDetailsFormatted = String.format(
    """
    ╔════════════════════════════════════════════════════════════════╗
    ║ BOOK ID: \u001B[1m\u001B[38;5;197m%-50s\u001B[22m\u001B[38;5;15m ║
    """,
    bookEntity.getBookId()  // ❌ CLI directly accessing entity methods
);
```

#### BookcaseCommands.java:63-71

```java
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases(); // ❌ Returning entities!
    for (BookcaseEntity b : bookcaseEntities) {
        int shelfBookCount = 0;
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId()); // ❌ Returning entities!
        for(ShelfEntity s : shelves){
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId()); // ❌ Returning entities!
            shelfBookCount += bookList.size();
        }
        options.put(bookcaseRowFormater(b,shelfBookCount), b.getBookcaseId().toString());
    }
    return  options;
}
```

**Why is this a problem?**

### Layering Violations Explained

```
┌─────────────────────────────────────┐
│   CLI Layer (BookCommands)           │
│                                      │
│   ❌ Directly uses:                  │
│   - BookEntity                       │  ← JPA implementation detail!
│   - AuthorEntity                     │  ← Should be hidden!
│   - ShelfEntity                      │  ← Persistence concern!
│   - BookcaseEntity                   │  ← Not part of public API!
└─────────────────────────────────────┘
```

**Problems with this approach:**

1. **Tight Coupling**
   The CLI layer is now **tightly coupled** to your database schema. If you change a JPA annotation or field name in an entity, you might break CLI commands.

2. **Violation of Single Responsibility Principle**
   Entities have JPA responsibilities (mapping to database tables). They shouldn't also be responsible for presentation logic.

3. **Makes Testing Harder**
   You can't test CLI commands without a database, because they depend on JPA entities.

4. **Leaky Abstraction**
   The service layer is supposed to **hide** persistence details. By returning entities, it's exposing them.

5. **Future Migration Risk**
   If you ever switch from JPA to another persistence mechanism (e.g., MongoDB, Redis), you'd have to change your CLI layer too!

### The Solution: Use DTOs and Domain Models

**Entities should NEVER leave the service layer.**

Instead, services should return:
- **DTOs** (Data Transfer Objects) - for simple data transfer
- **Domain Models** - for rich business logic

You already have some DTOs defined:

#### BookSummary.java (Good Example!)

```java
package com.penrose.bibby.library.book;

public record BookSummary(Long bookId, String title) {
}
```

#### BookDetailView.java (Good Example!)

```java
package com.penrose.bibby.library.book;

public record BookDetailView(
    Long bookId,
    String title,
    String authors,
    String bookcaseLabel,
    String shelfLabel,
    String bookStatus
) {}
```

**These records are PERFECT for cross-layer communication.**

### Refactored Approach

#### Step 1: Make Entities Package-Private

**BookEntity.java** (currently public)

```java
package com.penrose.bibby.library.book;

// BEFORE:
public class BookEntity {  // ❌ Public - accessible from CLI layer
    // ...
}

// AFTER:
class BookEntity {  // ✅ Package-private - only accessible within book package
    // ...
}
```

**AuthorEntity.java** (currently public)

```java
package com.penrose.bibby.library.author;

// BEFORE:
public class AuthorEntity {  // ❌ Exposed to CLI
    // ...
}

// AFTER:
class AuthorEntity {  // ✅ Hidden from CLI
    // ...
}
```

**ShelfEntity.java** (currently public)

```java
package com.penrose.bibby.library.shelf;

class ShelfEntity {  // ✅ Package-private
    // ...
}
```

**BookcaseEntity.java** (currently public)

```java
package com.penrose.bibby.library.bookcase;

class BookcaseEntity {  // ✅ Package-private
    // ...
}
```

#### Step 2: Create DTOs for All Service Methods

**BookcaseService.java** - Currently returns entities

```java
// BEFORE (BAD):
public List<BookcaseEntity> getAllBookcases() {  // ❌ Exposing entities!
    return bookcaseRepository.findAll();
}

// AFTER (GOOD):
public List<BookcaseDTO> getAllBookcases() {  // ✅ Returns DTOs!
    return bookcaseRepository.findAll()
        .stream()
        .map(entity -> new BookcaseDTO(
            entity.getBookcaseId(),
            entity.getBookcaseLabel(),
            entity.getShelfCapacity()
        ))
        .toList();
}
```

You already have `BookcaseDTO.java`:

```java
package com.penrose.bibby.library.bookcase;

public record BookcaseDTO(
    Long bookcaseId,
    String bookcaseLabel,
    int shelfCapacity
) {}
```

**ShelfService.java** - Create a DTO for shelf queries

```java
// BEFORE (BAD):
public List<ShelfEntity> findByBookcaseId(Long bookcaseId) {  // ❌ Exposing entities!
    return shelfRepository.findByBookcaseId(bookcaseId);
}

// AFTER (GOOD):
public List<ShelfSummary> findByBookcaseId(Long bookcaseId) {  // ✅ Returns DTOs!
    return shelfRepository.findByBookcaseId(bookcaseId)
        .stream()
        .map(entity -> new ShelfSummary(
            entity.getShelfId(),
            entity.getShelfLabel(),
            0  // Book count would be fetched separately
        ))
        .toList();
}
```

You already have `ShelfSummary.java`:

```java
package com.penrose.bibby.library.shelf;

public record ShelfSummary(
    Long shelfId,
    String label,
    long bookCount
) {}
```

**BookService.java** - Return DTOs instead of entities

```java
// BEFORE (BAD):
public List<BookEntity> findBooksByShelf(Long shelfId) {  // ❌ Exposing entities!
    return bookRepository.findByShelfId(shelfId);
}

// AFTER (GOOD):
public List<BookSummary> findBooksByShelf(Long shelfId) {  // ✅ Returns DTOs!
    return bookRepository.findBookSummariesByShelfIdOrderByTitleAsc(shelfId);
    // Note: Your repository already has this method at line 24!
}
```

#### Step 3: Update CLI Layer to Use DTOs

**BookcaseCommands.java:63-71** - Refactored

```java
// BEFORE (BAD):
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases(); // ❌
    for (BookcaseEntity b : bookcaseEntities) {
        int shelfBookCount = 0;
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId()); // ❌
        for(ShelfEntity s : shelves){
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId()); // ❌
            shelfBookCount += bookList.size();
        }
        options.put(bookcaseRowFormater(b,shelfBookCount), b.getBookcaseId().toString());
    }
    return  options;
}

// AFTER (GOOD):
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    List<BookcaseDTO> bookcases = bookcaseService.getAllBookcases(); // ✅ Using DTO!

    for (BookcaseDTO bookcase : bookcases) {
        int totalBooks = bookcaseService.getBookCountForBookcase(bookcase.bookcaseId()); // ✅ Service handles query
        String label = formatBookcaseRow(bookcase, totalBooks);
        options.put(label, bookcase.bookcaseId().toString());
    }
    return options;
}

// Helper method using DTO
private String formatBookcaseRow(BookcaseDTO bookcase, int bookCount) {
    return String.format(
        " %-12s \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mShelves    \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mBooks ",
        bookcase.bookcaseLabel().toUpperCase(),
        bookcase.shelfCapacity(),
        bookCount
    );
}
```

**BookcaseCommands.java:174-176** - Fix unsafe Optional usage

```java
// BEFORE (BAD):
if(result.getContext().get("optionSelected").equals("1") ){
    Optional<BookEntity> bookEntity = bookService.findBookById(bookId);  // ❌ Using entity
    bookService.checkOutBook(bookEntity.get());  // ❌ Unsafe get()!
}

// AFTER (GOOD):
if(result.getContext().get("optionSelected").equals("1") ){
    bookService.checkOutBook(bookId);  // ✅ Service handles entity lookup internally
}
```

**Update BookService.checkOutBook signature:**

```java
// BEFORE (BAD):
public void checkOutBook(BookEntity bookEntity) {  // ❌ Takes entity parameter
    bookEntity.setBookStatus(BookStatus.CHECKED_OUT.name());
    bookEntity.setCheckoutCount(bookEntity.getCheckoutCount() + 1);
    bookRepository.save(bookEntity);
}

// AFTER (GOOD):
public void checkOutBook(Long bookId) {  // ✅ Takes just the ID
    BookEntity bookEntity = bookRepository.findById(bookId)
        .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));

    bookEntity.setBookStatus(BookStatus.CHECKED_OUT.name());
    bookEntity.setCheckoutCount(bookEntity.getCheckoutCount() + 1);
    bookRepository.save(bookEntity);
}
```

### Benefits After Refactoring

```
┌─────────────────────────────────────┐
│   CLI Layer (BookCommands)           │  ← Only knows about DTOs
│                                      │
│   ✅ Uses:                            │
│   - BookSummary (DTO)                │
│   - BookDetailView (DTO)             │
│   - BookcaseDTO (DTO)                │
│   - ShelfSummary (DTO)               │
│                                      │
│   ❌ Cannot access:                   │
│   - BookEntity (package-private)     │
│   - AuthorEntity (package-private)   │
└─────────────────────────────────────┘
            ↓ Uses public API
┌─────────────────────────────────────┐
│   Service Layer                      │  ← Public API (DTOs in/out)
│   - BookService                      │
│   - ShelfService                     │
│   - BookcaseService                  │
└─────────────────────────────────────┘
            ↓ Uses internally
┌─────────────────────────────────────┐
│   Data Access Layer                  │  ← Hidden (package-private)
│   - BookEntity                       │
│   - BookRepository                   │
└─────────────────────────────────────┘
```

**Advantages:**

1. ✅ **Decoupled layers** - CLI doesn't depend on JPA
2. ✅ **Easier testing** - Can test CLI with mock DTOs (no database needed)
3. ✅ **Safer refactoring** - Change entity internals without breaking CLI
4. ✅ **Enforced boundaries** - Compiler prevents architectural violations
5. ✅ **Better performance** - DTOs can be optimized (no lazy-loading issues)

---

## Issue #3: Controller Accessing Repository Directly

### Current Code - BookController.java:11-16

```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorRepository;  // ❌ Cross-package repository access!
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class BookController {

    final BookService bookService;
    final AuthorRepository authorRepository;  // ❌ Controller should NOT inject repositories!

    public BookController(BookService bookService, AuthorRepository authorRepository, BookRepository bookRepository){
        this.bookService = bookService;
        this.authorRepository = authorRepository;  // ❌ Why does controller need this?
    }

    @PostMapping("api/v1/books")
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
        bookService.createNewBook(requestDTO);
        return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
    }

    @GetMapping("api/v1/books")
    public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
        System.out.println("Controller Search For " + requestDTO.title());
        bookService.findBookByTitle(requestDTO.title());
    }
}
```

### The Problem

**Controllers should ONLY depend on services, not repositories.**

```
❌ BAD:
Controller → Repository (bypasses service layer)

✅ GOOD:
Controller → Service → Repository
```

**Why is this a problem?**

1. **Duplicated Business Logic**
   If the controller accesses the repository directly, you might duplicate validation or business rules that should be in the service.

2. **Harder to Test**
   Controllers should be tested by mocking services, not repositories.

3. **Violates Single Responsibility**
   Controllers should handle HTTP concerns (request/response mapping), not data access.

4. **Cross-Package Coupling**
   `BookController` is in the `book` package but depends on `AuthorRepository` from the `author` package. This creates unnecessary coupling.

### The Fix

**Step 1: Remove the repository dependency**

```java
package com.penrose.bibby.library.book;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class BookController {

    private final BookService bookService;  // ✅ Only depend on service

    public BookController(BookService bookService) {  // ✅ Simplified constructor
        this.bookService = bookService;
    }

    @PostMapping("api/v1/books")
    public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
        bookService.createNewBook(requestDTO);
        return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
    }

    @GetMapping("api/v1/books")
    public ResponseEntity<List<BookSummary>> findBookByTitle(@RequestParam String title) {  // ✅ Fixed: use @RequestParam for GET
        List<BookSummary> books = bookService.findBooksByTitle(title);  // ✅ Returns DTOs
        return ResponseEntity.ok(books);
    }
}
```

**Step 2: If author data is needed, delegate to AuthorService**

If `BookController` needs author information, it should inject `AuthorService`, not `AuthorRepository`:

```java
public class BookController {

    private final BookService bookService;
    private final AuthorService authorService;  // ✅ Service, not repository!

    public BookController(BookService bookService, AuthorService authorService) {
        this.bookService = bookService;
        this.authorService = authorService;
    }

    // ... endpoints ...
}
```

---

## Issue #4: Repositories Should Be Package-Private

### Current Code - BookRepository.java:11-12

```java
package com.penrose.bibby.library.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository <BookEntity, Long> {  // ❌ Public!
    // ...
}
```

### The Problem

**Repositories are public, which allows them to be injected anywhere.**

This is what enabled the violation we saw in `BookController` (it injected `AuthorRepository` from another package).

**Repositories should be an implementation detail of the service layer.**

### The Solution

**Make repositories package-private:**

```java
package com.penrose.bibby.library.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface BookRepository extends JpaRepository <BookEntity, Long> {  // ✅ Package-private!

    BookEntity findBookEntityByTitle(String title);

    BookEntity findByTitle(String title);

    BookEntity findByTitleIgnoreCase(String title);

    List<BookEntity> findByTitleContaining(String title);

    List<BookEntity> findByShelfId(Long id);

    List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);

    @Query(value = """
        SELECT b.book_id, b.title,
               STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
               bc.bookcase_label, s.shelf_label, b.book_status
        FROM books b
        JOIN book_authors ba ON b.book_id = ba.book_id
        JOIN authors a ON ba.author_id = a.author_id
        JOIN shelves s ON s.shelf_id = b.shelf_id
        JOIN bookcases bc ON bc.bookcase_id = s.bookcase_id
        WHERE b.book_id = :bookId
        GROUP BY b.book_id, b.title, bc.bookcase_label, s.shelf_label, b.book_status
    """, nativeQuery = true)
    BookDetailView getBookDetailView(Long bookId);
}
```

**Now only classes in the `book` package can access `BookRepository`.**

Apply the same pattern to:
- `AuthorRepository` → package-private
- `ShelfRepository` → package-private
- `BookcaseRepository` → package-private
- `CatalogRepository` → package-private

---

## Issue #5: Mappers Should Be Package-Private

### Current Code - BookMapper.java:7

```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.genre.Genre;
import com.penrose.bibby.library.shelf.Shelf;

public class BookMapper {  // ❌ Public - but only used by BookService

    public static Book toDomain(BookEntity e, AuthorEntity authorEntity, Shelf shelf, Genre genre){
        // ... mapper logic ...
    }

    public static BookEntity toEntity(Book book){
        // ... mapper logic ...
    }
}
```

### The Problem

**Mappers are implementation details of the service layer.**

If `BookMapper` is public, other packages could call it directly and bypass service layer logic.

### The Solution

**Make mappers package-private:**

```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.genre.Genre;
import com.penrose.bibby.library.shelf.Shelf;

class BookMapper {  // ✅ Package-private!

    static Book toDomain(BookEntity e, AuthorEntity authorEntity, Shelf shelf, Genre genre){
        if (e == null){
            return null;
        }
        Book book = new Book();
        book.setId(e.getBookId());
        book.setEdition(e.getEdition());
        book.setTitle(e.getTitle());
        book.setAuthor(authorEntity);
        book.setIsbn(e.getIsbn());
        book.setPublisher(e.getPublisher());
        book.setPublicationYear(e.getPublicationYear());
        book.setGenre(genre);
        book.setShelf(shelf);
        book.setDescription(e.getDescription());
        book.setCheckoutCount(e.getCheckoutCount());
        book.setStatus(e.getBookStatus() != null ? BookStatus.valueOf(e.getBookStatus()) : null);
        book.setCreatedAt(e.getCreatedAt());
        book.setUpdatedAt(e.getUpdatedAt());
        return book;
    }

    static BookEntity toEntity(Book book){
        if (book == null){
            return null;
        }
        BookEntity bookEntity = new BookEntity();
        bookEntity.setBookId(book.getId());
        bookEntity.setTitle(book.getTitle());
        bookEntity.setIsbn(book.getIsbn());
        bookEntity.setPublisher(book.getPublisher());
        bookEntity.setPublicationYear(book.getPublicationYear());
        bookEntity.setGenre(book.getGenre() !=null ? book.getGenre().getGenreName() : null);
        bookEntity.setEdition(book.getEdition());
        bookEntity.setDescription(book.getDescription());
        bookEntity.setShelfId(book.getShelf() != null ? book.getShelf().getId() : null);
        bookEntity.setCheckoutCount(book.getCheckoutCount());
        bookEntity.setBookStatus(book.getStatus() != null ? book.getStatus().name() : null);
        bookEntity.setCreatedAt(book.getCreatedAt());
        bookEntity.setUpdatedAt(book.getUpdatedAt());
        return bookEntity;
    }
}
```

**Same pattern for `AuthorMapper` - make it package-private.**

---

## Good Examples in Your Codebase

### ✅ LoadingBar.java - Correctly Public

```java
package com.penrose.bibby.cli;

public class LoadingBar {  // ✅ Public - utility class used across CLI package

    public static void showProgressBar(String taskName, int totalSteps, int delayMs) throws InterruptedException {
        System.out.println(taskName);
        for (int i = 0; i <= totalSteps; i++) {
            int percent = (i * 100) / totalSteps;
            String bar = "\uD83D\uDFE9".repeat(i) + " ".repeat(totalSteps - i);
            System.out.print("\r[" + bar + "] " + percent + "%");
            Thread.sleep(delayMs);
        }
        System.out.println("\n✅ Done!");
    }
}
```

**Why public is correct here:**
- It's a utility that might be used by multiple command classes
- It has no dependencies on internal implementation details
- It's part of the CLI package's public API

### ✅ BookStatus.java - Correctly Public

```java
package com.penrose.bibby.library.book;

public enum BookStatus {  // ✅ Public - domain concept used across layers
    AVAILABLE,
    CHECKED_OUT,
    RESERVED,
    LOST,
    ARCHIVED
}
```

**Why public is correct here:**
- It's a domain concept that needs to be used in DTOs and domain models
- Enums representing business concepts should usually be public
- It's stable (unlikely to change frequently)

### ✅ BookSummary.java - Correctly Public

```java
package com.penrose.bibby.library.book;

public record BookSummary(Long bookId, String title) {  // ✅ Public - DTO for cross-layer transfer
}
```

**Why public is correct here:**
- DTOs need to cross package boundaries
- Records are immutable and safe to expose
- Part of the public API returned by services

### ✅ BookDetailView.java - Correctly Public

```java
package com.penrose.bibby.library.book;

public record BookDetailView(
    Long bookId,
    String title,
    String authors,
    String bookcaseLabel,
    String shelfLabel,
    String bookStatus
) {}  // ✅ Public - DTO for rich query results
```

**Why public is correct here:**
- Projection DTO used by repository queries (line 39 in BookRepository)
- Needs to be accessible from CLI and controller layers
- Immutable and safe

---

## When to Use Public vs Package-Private

### Use `public` for:

| Type                    | Example                        | Reason                                              |
|-------------------------|--------------------------------|-----------------------------------------------------|
| **DTOs**                | `BookSummary`, `BookcaseDTO`   | Cross package boundaries                            |
| **Domain Models**       | `Book`, `Author`, `Shelf`      | Used by services and potentially other packages     |
| **Services**            | `BookService`, `ShelfService`  | Public API of a package                             |
| **Controllers**         | `BookController`               | REST API endpoints (must be public for Spring)      |
| **Enums**               | `BookStatus`                   | Domain concepts used across layers                  |
| **Public Utilities**    | `LoadingBar`                   | Helper classes with no coupling to internals        |
| **Command Classes**     | `BookCommands`                 | Spring Shell needs to discover them (must be public)|

### Use package-private (no modifier) for:

| Type                    | Example                        | Reason                                              |
|-------------------------|--------------------------------|-----------------------------------------------------|
| **Entities**            | `BookEntity`, `AuthorEntity`   | JPA implementation details                          |
| **Repositories**        | `BookRepository`               | Data access implementation details                  |
| **Mappers**             | `BookMapper`, `AuthorMapper`   | Internal conversion logic                           |
| **Internal Helpers**    | Utility methods only used within one package | Encapsulation                       |

---

## Advanced: Using `package-info.java`

Java allows you to document and configure packages using a special file called `package-info.java`.

**Currently, Bibby has NO `package-info.java` files.**

### What is `package-info.java`?

It's a file that sits in the package directory and documents:
1. The **purpose** of the package
2. The **public API** contract
3. **Usage examples**
4. **Design decisions**

### Example: package-info.java for book package

Create this file: `/src/main/java/com/penrose/bibby/library/book/package-info.java`

```java
/**
 * Book Aggregate - Core domain logic for managing books in the library.
 *
 * <h2>Public API (what other packages can use):</h2>
 * <ul>
 *   <li>{@link com.penrose.bibby.library.book.BookService} - Book business operations</li>
 *   <li>{@link com.penrose.bibby.library.book.BookController} - REST API for books</li>
 *   <li>{@link com.penrose.bibby.library.book.Book} - Domain model</li>
 *   <li>{@link com.penrose.bibby.library.book.BookSummary} - Lightweight DTO</li>
 *   <li>{@link com.penrose.bibby.library.book.BookDetailView} - Rich query DTO</li>
 *   <li>{@link com.penrose.bibby.library.book.BookRequestDTO} - Input DTO</li>
 *   <li>{@link com.penrose.bibby.library.book.BookStatus} - Status enum</li>
 * </ul>
 *
 * <h2>Internal Implementation (package-private, do NOT use from other packages):</h2>
 * <ul>
 *   <li>{@code BookEntity} - JPA entity (persistence layer)</li>
 *   <li>{@code BookRepository} - Data access (persistence layer)</li>
 *   <li>{@code BookMapper} - Entity/Domain conversion</li>
 * </ul>
 *
 * <h2>Dependencies:</h2>
 * <ul>
 *   <li>{@link com.penrose.bibby.library.author} - Author information</li>
 *   <li>{@link com.penrose.bibby.library.genre} - Book categorization</li>
 *   <li>{@link com.penrose.bibby.library.shelf} - Physical location</li>
 * </ul>
 *
 * <h2>Design Notes:</h2>
 * <p>
 * This package follows the <strong>Repository Pattern</strong> and <strong>Aggregate Root</strong> design.
 * Books are the aggregate root, managing their own lifecycle and relationships with Authors, Genres, and Shelves.
 * </p>
 *
 * <h2>Usage Example (from CLI layer):</h2>
 * <pre>{@code
 * @Component
 * public class BookCommands {
 *     private final BookService bookService;  // ✅ Inject service
 *
 *     public void displayBooks() {
 *         List<BookSummary> books = bookService.getAllBooks();  // ✅ Use DTOs
 *         books.forEach(book -> System.out.println(book.title()));
 *     }
 * }
 * }</pre>
 *
 * @since 1.0
 * @author Penrose Development Team
 */
package com.penrose.bibby.library.book;
```

**Benefits:**

1. **Self-Documenting Architecture** - New developers can read the package contract
2. **IDE Support** - Shows up in JavaDoc tooltips
3. **Clear Boundaries** - Explicitly states what's public vs private
4. **Design Rationale** - Explains WHY certain decisions were made

---

## Summary: Package Visibility Rules

### The "Least Privilege" Principle

**Start with the most restrictive visibility, then relax only if needed.**

```
private > package-private > public
  ↑           ↑              ↑
  Best     Preferred      Use only when necessary
```

### Decision Tree: Class Visibility

```
Does this class need to be used from other packages?
│
├─ YES → Is it part of the public API?
│   │
│   ├─ YES → public (e.g., Service, DTO, Domain Model)
│   │
│   └─ NO → Consider refactoring (might be a design smell)
│
└─ NO → package-private (e.g., Entity, Repository, Mapper)
```

### Decision Tree: Field/Method Visibility

```
Does this field/method need to be accessed from outside this class?
│
├─ NO → private
│
└─ YES → From where?
    │
    ├─ Only subclasses → protected
    │
    ├─ Only this package → package-private
    │
    └─ From any package → public
```

---

## Key Concepts Recap

| Concept                        | What It Means                                                      | Example in Bibby                     |
|--------------------------------|--------------------------------------------------------------------|--------------------------------------|
| **Package**                    | Namespace + access control boundary                                | `com.penrose.bibby.library.book`     |
| **Vertical Slice Architecture**| One package per domain aggregate                                   | Separate packages for book, author   |
| **Layering**                   | Presentation → Service → Data Access                               | CLI → BookService → BookRepository   |
| **DTO (Data Transfer Object)** | Simple data carrier, no business logic                             | `BookSummary`, `BookcaseDTO`         |
| **Entity**                     | JPA-mapped class representing a database table                     | `BookEntity`, `AuthorEntity`         |
| **Domain Model**               | Rich business logic, not tied to persistence                       | `Book`, `Author` (currently anemic)  |
| **Public Class**               | Accessible from any package                                        | `BookService`, `BookSummary`         |
| **Package-Private Class**      | Accessible only within the same package                            | Should be: `BookEntity`, `BookMapper`|
| **Encapsulation**              | Hiding implementation details                                      | Making repositories package-private  |
| **Layering Violation**         | Higher layer bypassing a layer to access a lower layer             | CLI accessing entities directly      |
| **Leaky Abstraction**          | Implementation details exposed through API                         | Services returning entities          |

---

## Action Items for Your Codebase

### Priority 1: Critical Fixes (Do First)

- [ ] **Fix Genre field visibility** (Genre.java:6-7)
  ```java
  // Change:
  String genreName;
  String genreDescription;

  // To:
  private String genreName;
  private String genreDescription;
  ```

- [ ] **Make all entities package-private**
  - [ ] BookEntity.java
  - [ ] AuthorEntity.java
  - [ ] ShelfEntity.java
  - [ ] BookcaseEntity.java
  - [ ] CatalogEntity.java

- [ ] **Make all repositories package-private**
  - [ ] BookRepository.java
  - [ ] AuthorRepository.java
  - [ ] ShelfRepository.java
  - [ ] BookcaseRepository.java
  - [ ] CatalogRepository.java

### Priority 2: Architectural Improvements (Do Next)

- [ ] **Refactor services to return DTOs instead of entities**
  - [ ] `BookService.findBooksByShelf()` → return `List<BookSummary>`
  - [ ] `ShelfService.findByBookcaseId()` → return `List<ShelfSummary>`
  - [ ] `BookcaseService.getAllBookcases()` → return `List<BookcaseDTO>`

- [ ] **Update CLI layer to use DTOs**
  - [ ] Remove `BookEntity` imports from `BookCommands.java`
  - [ ] Remove `AuthorEntity` imports from `BookCommands.java`
  - [ ] Remove `ShelfEntity` imports from `BookcaseCommands.java`
  - [ ] Remove `BookcaseEntity` imports from `BookcaseCommands.java`

- [ ] **Fix controller dependencies**
  - [ ] Remove `AuthorRepository` injection from `BookController`
  - [ ] Inject `AuthorService` instead if author data is needed

### Priority 3: Code Organization (Do Later)

- [ ] **Make mappers package-private**
  - [ ] BookMapper.java
  - [ ] AuthorMapper.java

- [ ] **Create package-info.java files**
  - [ ] `com.penrose.bibby.library.book`
  - [ ] `com.penrose.bibby.library.author`
  - [ ] `com.penrose.bibby.library.shelf`
  - [ ] `com.penrose.bibby.library.bookcase`
  - [ ] `com.penrose.bibby.cli`

### Priority 4: Testing (Verify Your Changes)

- [ ] **Verify architectural boundaries are enforced**
  ```bash
  # After making entities package-private, try to compile
  # This should now FAIL (which is good!):
  # BookCommands trying to import BookEntity
  ```

- [ ] **Run all tests**
  ```bash
  ./mvnw test
  ```

- [ ] **Test CLI commands manually**
  - [ ] `bookcase add`
  - [ ] `bookcase browse`
  - [ ] `book add`
  - [ ] `book search`

---

## Testing Your Understanding

### Question 1: Visibility Identification

For each class below, determine if it should be `public` or package-private (no modifier):

1. `BookEntity` → **Answer:** Package-private (JPA implementation detail)
2. `BookService` → **Answer:** Public (public API of book package)
3. `BookSummary` → **Answer:** Public (DTO used across layers)
4. `BookMapper` → **Answer:** Package-private (internal helper)
5. `BookRepository` → **Answer:** Package-private (data access detail)
6. `BookStatus` → **Answer:** Public (domain concept used across layers)
7. `LoadingBar` → **Answer:** Public (utility used across CLI package)

### Question 2: Spot the Violation

Which of these imports in `BookCommands` are architectural violations?

```java
import com.penrose.bibby.library.book.BookService;       // ✅ OK
import com.penrose.bibby.library.book.BookEntity;        // ❌ VIOLATION
import com.penrose.bibby.library.book.BookSummary;       // ✅ OK
import com.penrose.bibby.library.author.AuthorService;   // ✅ OK
import com.penrose.bibby.library.author.AuthorEntity;    // ❌ VIOLATION
import com.penrose.bibby.library.shelf.ShelfRepository;  // ❌ VIOLATION
```

**Rule:** CLI layer should only import:
- Services (✅)
- DTOs (✅)
- Domain models (✅)

**Never import:**
- Entities (❌)
- Repositories (❌)
- Mappers (❌)

### Question 3: Refactoring Challenge

Given this code:

```java
// BookCommands.java
List<BookEntity> books = bookService.findBooksByShelf(shelfId);
for (BookEntity book : books) {
    System.out.println(book.getTitle());
}
```

Refactor it to use DTOs:

**Answer:**

```java
// BookCommands.java
List<BookSummary> books = bookService.findBooksByShelf(shelfId);
for (BookSummary book : books) {
    System.out.println(book.title());  // Record accessor method
}
```

**Changes needed:**
1. Update `BookService.findBooksByShelf()` signature to return `List<BookSummary>`
2. Make `BookEntity` package-private (enforces the boundary)
3. Use `book.title()` instead of `book.getTitle()` (records use accessor methods, not getters)

---

## Deep Dive: Why Package-Private Matters

### Real-World Analogy

Think of your codebase as an **apartment building**:

```
🏢 Bibby Application
├─ 🚪 Book Package (Apartment 1)
│  ├─ 🛋️ BookService (Living Room) → public
│  ├─ 🍳 BookRepository (Kitchen) → package-private
│  ├─ 🛏️ BookEntity (Bedroom) → package-private
│  └─ 📦 BookMapper (Storage Closet) → package-private
│
├─ 🚪 Author Package (Apartment 2)
│  ├─ 🛋️ AuthorService (Living Room) → public
│  ├─ 🍳 AuthorRepository (Kitchen) → package-private
│  └─ 🛏️ AuthorEntity (Bedroom) → package-private
│
└─ 🚪 CLI Package (Apartment 3)
   └─ 🛋️ BookCommands (Living Room) → public
```

**Package-private classes are like private rooms:**
- You wouldn't let guests from Apartment 3 wander into the bedroom of Apartment 1
- Guests can only access the living room (public API)
- Package-private = "family only" access

**Why this matters:**
1. **Privacy** - Implementation details stay internal
2. **Freedom to Change** - You can redecorate your bedroom without affecting guests
3. **Clear Contracts** - Guests know what they can access (living room only)

### What Happens Without Package-Private?

**Scenario: You want to rename `BookEntity` to `BookRecord`**

#### If BookEntity is public (CURRENT STATE):

```
Step 1: Rename BookEntity → BookRecord
Step 2: Compile...
Step 3: ERRORS!
  - BookCommands.java:7 → Cannot find symbol: BookEntity
  - BookcaseCommands.java:4 → Cannot find symbol: BookEntity
  - (10 more files...)
Step 4: Fix ALL 12 files that imported BookEntity
Step 5: Test ENTIRE application (CLI, REST API, etc.)
Step 6: 😫 Regret making this change
```

**Refactoring difficulty: HIGH** (changes ripple across the entire codebase)

#### If BookEntity is package-private (AFTER FIX):

```
Step 1: Rename BookEntity → BookRecord
Step 2: Compile...
Step 3: Fix BookService.java (only file in the package that uses it)
Step 4: Run tests for book package
Step 5: ✅ Done! No other files affected.
```

**Refactoring difficulty: LOW** (changes contained within one package)

### Measuring Coupling

**Coupling** = How many other classes depend on this class

```java
// BookEntity is public (HIGH COUPLING):
public class BookEntity { /* ... */ }

// Used by:
// - BookService.java (book package) ✓ OK
// - BookRepository.java (book package) ✓ OK
// - BookMapper.java (book package) ✓ OK
// - BookCommands.java (cli package) ❌ VIOLATION
// - BookcaseCommands.java (cli package) ❌ VIOLATION
// - BookController.java (book package) ✓ OK
// → 6 files depend on it (3 violate boundaries)
```

```java
// BookEntity is package-private (LOW COUPLING):
class BookEntity { /* ... */ }

// Used by:
// - BookService.java (book package) ✓ OK
// - BookRepository.java (book package) ✓ OK
// - BookMapper.java (book package) ✓ OK
// → Only 3 files can depend on it (all in same package)
```

**Lower coupling = Easier maintenance**

---

## Common Mistakes and How to Avoid Them

### Mistake #1: "Everything public by default"

❌ **Bad habit:**
```java
public class BookEntity { /* ... */ }  // "I might need this somewhere, so make it public"
public class BookMapper { /* ... */ }  // "Just in case..."
public interface BookRepository { /* ... */ }  // "Repositories need to be public, right?"
```

✅ **Good habit:**
```java
class BookEntity { /* ... */ }       // Start package-private
class BookMapper { /* ... */ }       // Only make public if you MUST
interface BookRepository { /* ... */ }  // Keep internal to service layer
```

**Rule of thumb:** Start restrictive, relax only when the compiler forces you to.

### Mistake #2: "Returning entities from services"

❌ **Bad:**
```java
public List<BookEntity> getAllBooks() {  // Leaky abstraction!
    return bookRepository.findAll();
}
```

✅ **Good:**
```java
public List<BookSummary> getAllBooks() {  // DTOs hide persistence details
    return bookRepository.findAll()
        .stream()
        .map(entity -> new BookSummary(entity.getBookId(), entity.getTitle()))
        .toList();
}
```

### Mistake #3: "Controllers injecting repositories"

❌ **Bad:**
```java
@RestController
public class BookController {
    private final BookRepository bookRepository;  // Bypasses service layer!

    @GetMapping("/books")
    public List<BookEntity> getBooks() {
        return bookRepository.findAll();  // No business logic validation!
    }
}
```

✅ **Good:**
```java
@RestController
public class BookController {
    private final BookService bookService;  // Proper layering

    @GetMapping("/books")
    public List<BookSummary> getBooks() {
        return bookService.getAllBooks();  // Service handles business logic
    }
}
```

### Mistake #4: "Forgetting to mark fields private"

❌ **Bad:**
```java
public class Genre {
    private Long id;
    String genreName;        // ❌ Package-private field!
    String genreDescription; // ❌ Package-private field!
}
```

✅ **Good:**
```java
public class Genre {
    private Long id;
    private String genreName;        // ✅ Explicit private
    private String genreDescription; // ✅ Explicit private
}
```

**Linter tip:** Configure your IDE to warn about package-private fields.

---

## Advanced Topic: Module System (Java 9+)

**Note:** This is beyond the scope of your current Bibby project, but good to know for future.

Java 9 introduced the **module system** (`module-info.java`) which adds even stronger encapsulation than packages.

**Example module-info.java:**

```java
module com.penrose.bibby.library {
    // Exports only specific packages (not internal packages)
    exports com.penrose.bibby.library.book;     // Public API
    exports com.penrose.bibby.library.author;   // Public API

    // Does NOT export internal packages:
    // - book.repository (hidden)
    // - book.entity (hidden)

    requires spring.boot;
    requires spring.data.jpa;
}
```

**Benefits:**
- Even stronger encapsulation than package-private
- Explicitly declare which packages are part of the public API
- Prevents reflection-based access to internal classes

**When to use:**
- Large codebases with multiple modules
- Libraries distributed to third parties
- Microservices with clear boundaries

---

## Further Reading

### Books
1. **"Clean Architecture"** by Robert C. Martin
   → Explains layering and dependency rules

2. **"Domain-Driven Design"** by Eric Evans
   → Aggregate boundaries and package organization

3. **"Effective Java"** by Joshua Bloch
   → Item 15: "Minimize the accessibility of classes and members"

### Articles
1. [Package by Feature vs Package by Layer](https://phauer.com/2020/package-by-feature/)
2. [Spring Boot Layered Architecture Best Practices](https://www.baeldung.com/spring-boot-clean-architecture)

### Official Documentation
1. [Java Language Specification - Access Control](https://docs.oracle.com/javase/specs/jls/se17/html/jls-6.html#jls-6.6)

---

## What's Next?

In **Section 9: Collections & Generics**, we'll examine:
- `List<BookEntity>` vs `Set<AuthorEntity>` (when to use which collection)
- Generic type safety in repositories (`JpaRepository<BookEntity, Long>`)
- Type erasure and its implications
- Creating custom generic utilities for Bibby

**Your Progress:**
- ✅ Sections 1-8 Complete (24% of mentorship guide)
- ⏳ 25 sections remaining

---

## Section 8 Summary

### What You Learned

1. ✅ **Package organization** using vertical slice architecture
2. ✅ **Class-level access modifiers** (public vs package-private)
3. ✅ **Architectural layering** violations in Bibby (CLI accessing entities)
4. ✅ **When to use DTOs** vs entities vs domain models
5. ✅ **Package-private repositories** and mappers as implementation details
6. ✅ **The "least privilege" principle** for visibility
7. ✅ **package-info.java** for documenting package contracts

### Issues Found in Bibby

| Issue | Location | Severity | Fix |
|-------|----------|----------|-----|
| Package-private fields | Genre.java:6-7 | High | Add `private` modifier |
| Public entities | BookEntity, AuthorEntity, etc. | High | Make package-private |
| CLI accessing entities | BookCommands, BookcaseCommands | Critical | Use DTOs instead |
| Controller injecting repository | BookController:11 | Medium | Inject service instead |
| Public repositories | All *Repository.java | Medium | Make package-private |
| Public mappers | BookMapper, AuthorMapper | Low | Make package-private |
| Missing package docs | All packages | Low | Add package-info.java |

### Key Takeaways

> **"Make everything as private as possible, then relax visibility only when required."**

> **"Entities and repositories are implementation details. DTOs are the public API."**

> **"Higher layers depend on lower layers, but never the reverse."**

---

**Section 8 Complete! 🎉**

**Time to refactor:** Start with Priority 1 fixes (Genre fields, make entities package-private), then move to architectural improvements.

**Questions?** Review the "Testing Your Understanding" section above.

**Ready for Section 9?** Reply **"yes"** or **"continue"**.
