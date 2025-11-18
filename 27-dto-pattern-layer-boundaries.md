# Section 27: DTO Pattern & Layer Boundaries

**Learning objective:** Understand the Data Transfer Object (DTO) pattern, learn why entities should never be exposed outside the service layer, master DTO mapping strategies, and fix Bibby's layer boundary violations.

**Why this matters:** Your `BookService` returns `BookEntity` directly to controllers and CLI. This couples your presentation layer to your database schema, makes refactoring dangerous, and exposes JPA implementation details. DTOs solve this by creating a clear boundary between layers.

**Duration:** 65 min read

---

## The Critical Question

**Your current service method:**

```java
// BookService.java:43
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    // ...
    return bookEntity.get();  // ❌ Returning entity directly!
}
```

**Question:** What happens when you change `BookEntity` (add field, change relationship, rename field)?

**Answer:** You break **every controller, CLI command, and test** that uses this method. This is called **tight coupling** - your presentation layer is directly dependent on your database schema.

This section reveals how DTOs create a protective boundary between layers.

---

## What You'll Learn From Your Code

You have **massive confusion** in Bibby:
- **2 DTOs:** `BookRequestDTO`, `BookcaseDTO` ✅
- **5 Entities:** `BookEntity`, `AuthorEntity`, `ShelfEntity`, `BookcaseEntity`, `CatalogEntity`
- **3 DUPLICATE plain classes:** `Book`, `Shelf`, `Bookcase` ❌ (WHAT ARE THESE?)
- **Services returning entities:** All services expose entities directly ❌

**Big discovery:** You have `Book.java` (181 lines, plain class) AND `BookEntity.java` (136 lines, JPA entity) in the **same package**. This is DTO/Entity confusion at its worst.

We'll untangle this mess and build proper layer boundaries.

---

## Part 1: What is a DTO?

### Definition

**DTO (Data Transfer Object):** A simple object that carries data between layers with NO business logic.

**Purpose:** Decouple presentation layer (controllers, CLI) from persistence layer (entities, database).

---

### Entity vs DTO

| Aspect | Entity | DTO |
|--------|--------|-----|
| **Purpose** | Represent database table | Transfer data between layers |
| **Annotations** | `@Entity`, `@Id`, `@ManyToOne` | None (or validation like `@NotNull`) |
| **Relationships** | `@ManyToMany`, `@OneToMany` | Plain fields (no JPA relationships) |
| **Mutability** | Mutable (setters for JPA) | Immutable (records are perfect) |
| **Location** | Service layer and below | Controllers, CLI, REST APIs |
| **Lifecycle** | Managed by Hibernate | Created/destroyed per request |
| **equals/hashCode** | Business key | All fields |

---

### Example: BookEntity vs BookDTO

**BookEntity (DON'T expose this!):**
```java
// BookEntity.java (database representation)
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    private String title;
    private String isbn;
    private String publisher;
    private int publicationYear;
    private String genre;
    private int edition;
    private String description;
    private Integer checkoutCount;
    private String bookStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private ShelfEntity shelf;  // JPA relationship

    @ManyToMany
    @JoinTable(/* ... */)
    private Set<AuthorEntity> authors;  // JPA relationship
}
```

**BookDTO (DO expose this!):**
```java
// BookDTO.java (presentation representation)
public record BookDTO(
    Long id,
    String title,
    String isbn,
    String publisher,
    int publicationYear,
    String genre,
    int edition,
    String description,
    String bookStatus,
    List<String> authorNames  // Simplified (no entity reference!)
) {}
```

**Key differences:**
- ✅ DTO has NO `@Entity`, `@Id`, `@ManyToMany`
- ✅ DTO has simple `List<String> authorNames` instead of `Set<AuthorEntity> authors`
- ✅ DTO is a **record** (immutable, concise)
- ✅ DTO has NO JPA annotations
- ✅ DTO can have different field names (`id` vs `bookId`)

---

## Part 2: Your DTO/Entity Confusion

### The Shocking Discovery

**You have DUPLICATE classes:**

```
src/main/java/com/penrose/bibby/library/book/
├── Book.java          (181 lines, plain class)  ❌ WHAT IS THIS?
└── BookEntity.java    (136 lines, JPA entity)   ✅ Correct

src/main/java/com/penrose/bibby/library/shelf/
├── Shelf.java         (37 lines, plain class)   ❌ WHAT IS THIS?
└── ShelfEntity.java   (44 lines, JPA entity)    ✅ Correct

src/main/java/com/penrose/bibby/library/bookcase/
├── Bookcase.java      (38 lines, plain class)   ❌ WHAT IS THIS?
└── BookcaseEntity.java (54 lines, JPA entity)   ✅ Correct
```

**Question:** Why do you have `Book.java` AND `BookEntity.java`?

Let's investigate `Book.java`:

```java
// Book.java:10-24 (plain class, NO @Entity)
public class Book {
    private Long id;
    private int edition;
    private String title;
    private AuthorEntity authorEntity;  // ❌ References entity!
    private String isbn;
    private String publisher;
    private int publicationYear;
    private Genre genre;
    private Shelf shelf;  // ❌ References non-entity Shelf class!
    private String description;
    private BookStatus status;
    private Integer checkoutCount;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
```

**Problems:**
1. ❌ **No `@Entity` annotation** - Not a JPA entity
2. ❌ **References `AuthorEntity`** - Mixing DTO with entity!
3. ❌ **References `Shelf`** - Non-entity plain class
4. ❌ **Has same fields as `BookEntity`** - Duplicate structure
5. ❌ **Has equals/hashCode using ID** - Wrong for DTO
6. ❌ **Unused** - Only imported in 4 files, never actually used

**Verdict:** `Book.java` is a failed attempt at a DTO that references entities. Delete it.

---

### The Same Problem with Shelf and Bookcase

**Shelf.java:**
```java
// Shelf.java:6-10
public class Shelf {
    private Long id;
    private Bookcase bookCase;  // ❌ References non-entity Bookcase!
    private String shelfLabel;
    private int shelfPosition;
}
```

**Bookcase.java:**
```java
// Bookcase.java:3-6
public class Bookcase {
    private Long bookcaseId;
    private String bookcaseLabel;
    private int shelfCapacity;
}
```

**Same problems:**
- No `@Entity` annotations
- Duplicate fields from entities
- Confused references (Shelf → Bookcase, Book → Shelf)
- Unused or barely used

**Verdict:** Delete `Book.java`, `Shelf.java`, and `Bookcase.java`. Use entities internally, DTOs externally.

---

### What About Genre?

**Genre.java:**
```java
// Genre.java:3-13
public class Genre {
    private Long id;
    String genreName;  // ❌ Package-private (no access modifier)
    String genreDescription;

    public Genre(String genreName, String genreDescription) {
        this.genreName = genreName;
        this.genreDescription = genreDescription;
    }
}
```

**Problems:**
1. ❌ **No `@Entity` annotation** - Not persisted to database
2. ❌ **Package-private fields** - Poor encapsulation
3. ❌ **Unused** - Only referenced in `Book.java` (which should be deleted)

**Better design:** Genre should be either:
- **Option 1:** An enum (if fixed set: FICTION, NON_FICTION, MYSTERY, etc.)
- **Option 2:** A JPA entity (if users can create custom genres)

**Current state:** `BookEntity.genre` is a `String` (line 21), so Genre class is redundant.

**Verdict:** Delete `Genre.java` or convert to enum if needed later.

---

## Part 3: Layer Boundaries - The Three-Layer Architecture

### The Ideal Architecture

```
┌─────────────────────────────────────┐
│   Presentation Layer                │
│   (Controllers, CLI Commands)       │
│   Works with: DTOs                  │  ← USER SEES THIS
└───────────────┬─────────────────────┘
                │ DTOs flow up
                │ DTOs flow down
┌───────────────▼─────────────────────┐
│   Service Layer                     │
│   (Services, Business Logic)        │
│   Works with: Entities + DTOs       │  ← CONVERTS BETWEEN LAYERS
│   - Receives DTOs from controllers  │
│   - Maps DTOs → Entities            │
│   - Calls repositories with entities│
│   - Maps Entities → DTOs            │
│   - Returns DTOs to controllers     │
└───────────────┬─────────────────────┘
                │ Entities flow
                │
┌───────────────▼─────────────────────┐
│   Persistence Layer                 │
│   (Repositories, Database)          │
│   Works with: Entities only         │  ← DATABASE
└─────────────────────────────────────┘
```

**Key rules:**
1. **Presentation layer** NEVER sees entities
2. **Service layer** converts between DTOs and entities
3. **Persistence layer** NEVER sees DTOs

---

### Bibby's Current (Broken) Architecture

```
┌─────────────────────────────────────┐
│   Presentation Layer                │
│   BookController, BookCommands      │
└───────────────┬─────────────────────┘
                │
                │ BookEntity (❌ ENTITY LEAKING!)
                │
┌───────────────▼─────────────────────┐
│   Service Layer                     │
│   BookService                       │
│   - Returns BookEntity              │  ← NO DTO CONVERSION!
│   - Returns List<BookEntity>        │
└───────────────┬─────────────────────┘
                │
                │ BookEntity
                │
┌───────────────▼─────────────────────┐
│   Persistence Layer                 │
│   BookRepository                    │
└─────────────────────────────────────┘
```

**Problems:**
- ❌ Controllers get `BookEntity` directly
- ❌ CLI commands work with `BookEntity`
- ❌ No DTO mapping layer
- ❌ Presentation tightly coupled to database schema

---

## Part 4: Your Current DTOs (The Good Parts)

You have **2 DTOs** that are actually correct!

### DTO 1: BookRequestDTO

**File:** BookRequestDTO.java (4 lines)

```java
// BookRequestDTO.java:3
public record BookRequestDTO(String title, String firstName, String lastName) {}
```

**Usage:**
```java
// BookController.java:19
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookService.createNewBook(requestDTO);
    return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
}
```

**Analysis:**
- ✅ **Record** - Immutable, concise
- ✅ **Simple fields** - No entity references
- ✅ **Used in controller** - Correct layer
- ✅ **Passed to service** - Service handles entity creation

**Grade:** A (perfect input DTO)

**Improvement:** Rename to `CreateBookRequest` (more descriptive than "BookRequestDTO").

---

### DTO 2: BookcaseDTO

**File:** BookcaseDTO.java (4 lines)

```java
// BookcaseDTO.java:3
public record BookcaseDTO(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {}
```

**Usage:**
```java
// BookCaseController.java:20
@PostMapping("/create/bookcase")
public ResponseEntity<String> createBookCase(@RequestBody BookcaseDTO bookcaseDTO){
    String message = bookCaseService.createNewBookCase(
        bookcaseDTO.bookcaseLabel(),
        bookcaseDTO.shelfCapacity()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(message);
}
```

**Analysis:**
- ✅ **Record** - Immutable
- ✅ **Simple fields** - No entity references
- ✅ **Used in controller** - Correct layer
- ⚠️ **Has bookcaseId** - Input DTO shouldn't have ID (generated by database)

**Grade:** B+ (good, but ID shouldn't be in request DTO)

**Improvement:**
```java
// Split into two DTOs:
public record CreateBookcaseRequest(String bookcaseLabel, int shelfCapacity) {}  // Input (no ID)
public record BookcaseResponse(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {}  // Output (has ID)
```

---

## Part 5: What's Missing - Response DTOs

**Current problem:**

```java
// BookService.java:43-54 (WRONG - returns entity)
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    // ...
    return bookEntity.get();  // ❌ Returning JPA entity!
}
```

**Why this is bad:**
1. **Coupling:** Controllers depend on `BookEntity` structure
2. **Lazy loading exceptions:** If controller accesses `book.getAuthors()` after transaction closes → `LazyInitializationException`
3. **Over-fetching:** Returns all 13 fields even if controller only needs 2
4. **Security risk:** Exposes internal fields (createdAt, updatedAt, etc.)
5. **Refactoring nightmare:** Changing `BookEntity` breaks controllers

---

### The Solution: Response DTOs

**Create output DTOs:**

```java
// BookResponse.java (NEW - create this!)
public record BookResponse(
    Long id,
    String title,
    String isbn,
    String publisher,
    int publicationYear,
    String genre,
    int edition,
    String description,
    String status,
    List<String> authorNames  // Simplified!
) {}
```

**Update service:**
```java
// BookService.java - FIXED VERSION
public Optional<BookResponse> findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(
        bookRepository.findByTitleIgnoreCase(title)
    );

    return bookEntity.map(this::toBookResponse);  // Map entity → DTO
}

// Mapping method (in service):
private BookResponse toBookResponse(BookEntity entity) {
    List<String> authorNames = entity.getAuthors().stream()
        .map(author -> author.getFirstName() + " " + author.getLastName())
        .toList();

    return new BookResponse(
        entity.getBookId(),
        entity.getTitle(),
        entity.getIsbn(),
        entity.getPublisher(),
        entity.getPublicationYear(),
        entity.getGenre(),
        entity.getEdition(),
        entity.getDescription(),
        entity.getBookStatus(),
        authorNames
    );
}
```

**Benefits:**
- ✅ Controller gets **DTO** (no JPA coupling)
- ✅ **No lazy loading issues** (authors loaded during mapping)
- ✅ **Simplified structure** (`List<String>` instead of `Set<AuthorEntity>`)
- ✅ **Refactoring safe** (change `BookEntity`, DTO stays same)

---

## Part 6: DTO Mapping Strategies

### Strategy 1: Manual Mapping (What You Should Use)

**Pros:**
- Full control
- No external dependencies
- Type-safe
- Easy to debug
- Works great for simple DTOs

**Cons:**
- Boilerplate code
- Manual updates when fields change

**Example:**
```java
// In BookService.java - ADD THESE METHODS:

// Entity → DTO
private BookResponse toBookResponse(BookEntity entity) {
    return new BookResponse(
        entity.getBookId(),
        entity.getTitle(),
        entity.getIsbn(),
        entity.getPublisher(),
        entity.getPublicationYear(),
        entity.getGenre(),
        entity.getEdition(),
        entity.getDescription(),
        entity.getBookStatus(),
        entity.getAuthors().stream()
            .map(a -> a.getFirstName() + " " + a.getLastName())
            .toList()
    );
}

// DTO → Entity (for updates)
private void updateEntityFromDTO(BookEntity entity, UpdateBookRequest dto) {
    entity.setTitle(dto.title());
    entity.setIsbn(dto.isbn());
    entity.setPublisher(dto.publisher());
    entity.setPublicationYear(dto.publicationYear());
    entity.setGenre(dto.genre());
    entity.setEdition(dto.edition());
    entity.setDescription(dto.description());
}
```

**When to use:** Always (start with manual mapping).

---

### Strategy 2: MapStruct (Advanced)

**Pros:**
- Automatic mapping (less boilerplate)
- Compile-time code generation (type-safe)
- Good performance

**Cons:**
- External dependency
- Learning curve
- Overkill for simple projects

**Example:**
```java
// Add MapStruct dependency to pom.xml:
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

// Create mapper interface:
@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(source = "bookId", target = "id")
    @Mapping(target = "authorNames", expression = "java(mapAuthors(entity.getAuthors()))")
    BookResponse toBookResponse(BookEntity entity);

    default List<String> mapAuthors(Set<AuthorEntity> authors) {
        return authors.stream()
            .map(a -> a.getFirstName() + " " + a.getLastName())
            .toList();
    }
}

// Use in service:
@Service
public class BookService {
    private final BookMapper bookMapper;

    public Optional<BookResponse> findBookByTitle(String title) {
        return bookRepository.findByTitleIgnoreCase(title)
            .map(bookMapper::toBookResponse);  // Automatic mapping!
    }
}
```

**When to use:** Large projects with many DTOs (20+ entities).

---

### Strategy 3: ModelMapper (DON'T USE)

**Pros:**
- Automatic mapping

**Cons:**
- **Runtime reflection** (slow, error-prone)
- **Type-unsafe** (errors at runtime, not compile-time)
- **Hard to debug** (magic mapping rules)

**Example (for comparison only):**
```java
// DON'T DO THIS:
ModelMapper modelMapper = new ModelMapper();
BookResponse dto = modelMapper.map(bookEntity, BookResponse.class);  // ❌ Runtime magic
```

**Verdict:** Avoid. Use manual mapping or MapStruct.

---

**Recommendation for Bibby:** Start with **manual mapping** (Strategy 1). It's simple, explicit, and you only have 5 entities.

---

## Part 7: Fixing Bibby's Services

### Current Service Issues

**BookService.java problems:**

| Method | Returns | Should Return |
|--------|---------|---------------|
| `findBookByTitle()` | `BookEntity` | `Optional<BookResponse>` |
| `findBooksByShelf()` | `List<BookEntity>` | `List<BookResponse>` |
| `checkOutBook()` | void (takes `BookEntity`) | void (takes `Long bookId`) |
| `updateBook()` | void (takes `BookEntity`) | void (takes `UpdateBookRequest`) |
| `findBookById()` | `Optional<BookEntity>` | `Optional<BookResponse>` |

---

### Fixed BookService

**Step 1: Create DTOs**

```java
// CreateBookRequest.java (rename from BookRequestDTO)
public record CreateBookRequest(
    String title,
    String firstName,
    String lastName
) {}

// BookResponse.java (NEW)
public record BookResponse(
    Long id,
    String title,
    String isbn,
    String publisher,
    int publicationYear,
    String genre,
    int edition,
    String description,
    String status,
    List<String> authorNames
) {}

// UpdateBookRequest.java (NEW)
public record UpdateBookRequest(
    String title,
    String isbn,
    String publisher,
    int publicationYear,
    String genre,
    int edition,
    String description
) {}
```

---

**Step 2: Update Service Methods**

```java
// BookService.java - FIXED VERSION
@Service
public class BookService {

    final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    // CREATE (input: DTO, output: DTO)
    @Transactional
    public BookResponse createNewBook(CreateBookRequest request) {
        AuthorEntity author = findOrCreateAuthor(request.firstName(), request.lastName());

        BookEntity entity = new BookEntity();
        entity.setTitle(request.title());
        entity.setAuthors(author);

        BookEntity saved = bookRepository.save(entity);
        return toBookResponse(saved);  // Return DTO, not entity!
    }

    // READ (output: DTO)
    public Optional<BookResponse> findBookByTitle(String title){
        return bookRepository.findByTitleIgnoreCase(title)
            .map(this::toBookResponse);  // Map to DTO
    }

    // READ (output: List of DTOs)
    public List<BookResponse> findBooksByShelf(Long shelfId) {
        return bookRepository.findByShelfId(shelfId).stream()
            .map(this::toBookResponse)
            .toList();
    }

    // UPDATE (input: ID + DTO)
    @Transactional
    public void updateBook(Long bookId, UpdateBookRequest request) {
        BookEntity entity = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        updateEntityFromDTO(entity, request);
        bookRepository.save(entity);  // No return needed for update
    }

    // CHECK OUT (input: ID only, no entity!)
    @Transactional
    public void checkOutBook(Long bookId){
        BookEntity entity = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!entity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
            entity.setBookStatus(BookStatus.CHECKED_OUT.toString());
            bookRepository.save(entity);
        }
    }

    // MAPPING: Entity → DTO
    private BookResponse toBookResponse(BookEntity entity) {
        List<String> authorNames = entity.getAuthors().stream()
            .map(a -> a.getFirstName() + " " + a.getLastName())
            .toList();

        return new BookResponse(
            entity.getBookId(),
            entity.getTitle(),
            entity.getIsbn(),
            entity.getPublisher(),
            entity.getPublicationYear(),
            entity.getGenre(),
            entity.getEdition(),
            entity.getDescription(),
            entity.getBookStatus(),
            authorNames
        );
    }

    // MAPPING: DTO → Entity (for updates)
    private void updateEntityFromDTO(BookEntity entity, UpdateBookRequest dto) {
        entity.setTitle(dto.title());
        entity.setIsbn(dto.isbn());
        entity.setPublisher(dto.publisher());
        entity.setPublicationYear(dto.publicationYear());
        entity.setGenre(dto.genre());
        entity.setEdition(dto.edition());
        entity.setDescription(dto.description());
    }

    // Helper method
    private AuthorEntity findOrCreateAuthor(String firstName, String lastName) {
        AuthorEntity author = authorRepository.findByFirstNameAndLastName(firstName, lastName);
        if (author == null) {
            author = new AuthorEntity(firstName, lastName);
            authorRepository.save(author);
        }
        return author;
    }
}
```

**Key changes:**
1. ✅ All methods return **DTOs** (not entities)
2. ✅ All methods accept **DTOs or IDs** (not entities)
3. ✅ Entities stay **internal to service**
4. ✅ Mapping done **inside service** (controllers never see entities)

---

## Part 8: Controller Anti-Patterns

### Anti-Pattern 1: Returning void with System.out.println

**Your code:**
```java
// BookController.java:24-28
@GetMapping("api/v1/books")
public void findBookByTitle(@RequestBody BookRequestDTO requestDTO){
    System.out.println("Controller Search For " + requestDTO.title());  // ❌ Debug print
    bookService.findBookByTitle(requestDTO.title());  // ❌ Result ignored
}
```

**Problems:**
1. ❌ **Returns void** - No response to client!
2. ❌ **System.out.println** - Should use logger
3. ❌ **Ignores result** - Service returns BookEntity, but it's thrown away
4. ❌ **@GetMapping with @RequestBody** - GET shouldn't have request body

---

**Fixed version:**
```java
// BookController.java - FIXED
@GetMapping("api/v1/books")
public ResponseEntity<BookResponse> findBookByTitle(@RequestParam String title){
    return bookService.findBookByTitle(title)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}
```

**Changes:**
1. ✅ **Returns ResponseEntity<BookResponse>** - Client gets DTO
2. ✅ **@RequestParam instead of @RequestBody** - GET uses query params
3. ✅ **Uses Optional.map()** - Returns 200 OK or 404 Not Found
4. ✅ **No debug prints** - Clean code

---

### Anti-Pattern 2: Controller Accessing Repository Directly

**Your code:**
```java
// BookController.java:10-15
@RestController
public class BookController {
    final BookService bookService;
    final AuthorRepository authorRepository;  // ❌ Controller has repository!

    public BookController(BookService bookService, AuthorRepository authorRepository, BookRepository bookRepository){
        this.bookService = bookService;
        this.authorRepository = authorRepository;  // ❌ WRONG!
    }
}
```

**Problem:** Controllers should NEVER access repositories directly. That's the service layer's job.

**Rule:** Controllers → Services → Repositories (never skip the service layer).

---

**Fixed version:**
```java
// BookController.java - FIXED
@RestController
public class BookController {
    private final BookService bookService;  // ✅ Only service

    public BookController(BookService bookService){
        this.bookService = bookService;
    }
}
```

**If controller needs author data:** Add method to `AuthorService`, call from controller.

---

## Part 9: Complete DTO Strategy for Bibby

### DTOs You Should Create

**Book DTOs:**
```java
// Input DTOs:
public record CreateBookRequest(String title, String firstName, String lastName) {}
public record UpdateBookRequest(String title, String isbn, String publisher, int publicationYear, String genre, int edition, String description) {}

// Output DTOs:
public record BookResponse(Long id, String title, String isbn, String publisher, int publicationYear, String genre, int edition, String description, String status, List<String> authorNames) {}
public record BookSummaryResponse(Long id, String title) {}  // Lightweight
```

**Author DTOs:**
```java
// Input DTOs:
public record CreateAuthorRequest(String firstName, String lastName) {}

// Output DTOs:
public record AuthorResponse(Long id, String firstName, String lastName, List<String> bookTitles) {}
```

**Shelf DTOs:**
```java
// Input DTOs:
public record CreateShelfRequest(String shelfLabel, Long bookcaseId, int shelfPosition) {}

// Output DTOs:
public record ShelfResponse(Long id, String label, int position, String bookcaseLabel, int bookCount) {}
```

**Bookcase DTOs:**
```java
// Input DTOs:
public record CreateBookcaseRequest(String bookcaseLabel, int shelfCapacity) {}

// Output DTOs:
public record BookcaseResponse(Long id, String label, int shelfCapacity, int currentShelfCount) {}
```

---

## Summary: Your DTO Situation - Before & After

### Before (Current State):

| Issue | Grade | Problem |
|-------|-------|---------|
| Duplicate classes (Book, Shelf, Bookcase) | F | Confused DTO/Entity separation |
| Services return entities | D | Tight coupling, lazy load exceptions |
| Controllers access repositories | D | Violates layer boundaries |
| Only 2 DTOs (both input only) | C | No response DTOs |
| Genre.java unused class | F | Dead code |

**Overall Grade: D-**

---

### After (All Fixes Applied):

| Improvement | Grade | Benefit |
|-------------|-------|---------|
| Delete Book/Shelf/Bookcase classes | A | Clear entity/DTO separation |
| Services return DTOs | A | Decoupled layers, no lazy load issues |
| Controllers only call services | A | Proper layer boundaries |
| 8+ DTOs (input + output) | A | Complete DTO coverage |
| Genre deleted or converted to enum | A | No dead code |

**Overall Grade: A**

---

## Action Items

### Priority 1: Delete Confusing Classes (CRITICAL)

```bash
# Delete these files:
rm src/main/java/com/penrose/bibby/library/book/Book.java
rm src/main/java/com/penrose/bibby/library/shelf/Shelf.java
rm src/main/java/com/penrose/bibby/library/bookcase/Bookcase.java
rm src/main/java/com/penrose/bibby/library/genre/Genre.java
```

**Impact:** Eliminates DTO/Entity confusion.

---

### Priority 2: Create Response DTOs

```java
// Create these files:
src/main/java/com/penrose/bibby/library/book/BookResponse.java
src/main/java/com/penrose/bibby/library/book/UpdateBookRequest.java
src/main/java/com/penrose/bibby/library/author/AuthorResponse.java
src/main/java/com/penrose/bibby/library/shelf/ShelfResponse.java
src/main/java/com/penrose/bibby/library/bookcase/BookcaseResponse.java
```

---

### Priority 3: Update Services to Return DTOs

**BookService methods to fix:**
```java
// CHANGE:
public BookEntity findBookByTitle(String title)
// TO:
public Optional<BookResponse> findBookByTitle(String title)

// CHANGE:
public List<BookEntity> findBooksByShelf(Long id)
// TO:
public List<BookResponse> findBooksByShelf(Long id)

// CHANGE:
public void checkOutBook(BookEntity bookEntity)
// TO:
public void checkOutBook(Long bookId)
```

**Add mapping methods to each service:**
```java
private BookResponse toBookResponse(BookEntity entity) { /* ... */ }
private void updateEntityFromDTO(BookEntity entity, UpdateBookRequest dto) { /* ... */ }
```

---

### Priority 4: Fix Controllers

```java
// BookController.java - REMOVE:
final AuthorRepository authorRepository;  // ❌ Delete this

// BookController.java - FIX:
@GetMapping("api/v1/books")
public ResponseEntity<BookResponse> findBookByTitle(@RequestParam String title) {
    return bookService.findBookByTitle(title)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}
```

---

### Priority 5: Update CLI Commands

**CLI commands also use entities - fix them too:**

```java
// BookCommands.java - CHANGE:
BookEntity book = bookService.findBookByTitle(title);  // ❌ Entity
// TO:
Optional<BookResponse> book = bookService.findBookByTitle(title);  // ✅ DTO
```

---

## What You Learned

1. **DTOs vs Entities:**
   - Entities = database representation (JPA annotations, relationships)
   - DTOs = presentation representation (plain records, no JPA)

2. **Layer boundaries:**
   - Presentation → Services (DTOs)
   - Services → Repositories (Entities)
   - Services convert between DTOs and Entities

3. **Bibby's critical issues:**
   - Book/Shelf/Bookcase duplicate classes (delete them!)
   - Services returning entities (return DTOs!)
   - Controllers accessing repositories (use services only!)

4. **DTO mapping:**
   - Manual mapping (best for simple projects like Bibby)
   - MapStruct (advanced, compile-time generation)
   - ModelMapper (avoid - runtime reflection)

5. **DTO naming conventions:**
   - Input: `CreateBookRequest`, `UpdateBookRequest`
   - Output: `BookResponse`, `BookSummaryResponse`
   - Avoid: `BookDTO` (too generic)

---

**Next:** Section 28 - Validation will explore how to validate DTOs with Bean Validation (`@NotNull`, `@Size`, `@Pattern`) and create custom validators.

**Your layers are currently tangled.** These fixes transform Bibby from **Grade D- to Grade A**, creating clean layer boundaries, proper encapsulation, and maintainable code.

You're building excellent architecture. Keep going!

---

*Section 27 complete. 27 of 33 sections finished (82%).*
*Next up: Section 28 - Validation*
