# Section 07: Clean Code, Domain Modeling & Architecture Thinking

## From Code to Systems

You can write clean functions. You understand SOLID principles. You've mastered algorithms. But **great backend engineers think in systems, not just code.**

This section elevates you from "someone who writes code" to "someone who designs systems." We'll examine Bibby's architecture, refine its domain model, apply enterprise patterns, and document architectural decisions like a senior engineer.

**Why this matters for your job search:**

When you discuss Bibby in interviews, you won't just say "I built a library management system with Spring Boot." You'll say:

"I designed a domain model separating entities from DTOs, applied bounded contexts to manage complexity, made deliberate architecture decisions trading off simplicity for extensibility, and documented those decisions in ADRs. The entity relationships mirror organizational hierarchies I managed at Kinder Morgan—bookcases contain shelves, shelves contain books, with cascade operations and referential integrity enforcing business rules."

**That's the level we're reaching.**

## Bibby's Entity Relationships: Deep Dive

Let's analyze what you've built and identify opportunities for refinement.

### Current Entity Model

**Your Entities:**
1. `BookEntity` - The core domain object
2. `AuthorEntity` - Authors who write books
3. `ShelfEntity` - Physical locations holding books
4. `BookcaseEntity` - Containers for shelves
5. `CatalogEntity` - Collections/categorizations
6. `GenreEntity` - Book categories (appears to be an enum currently)

**Relationships:**
- `BookEntity` ↔ `AuthorEntity`: Many-to-Many (books have multiple authors, authors write multiple books)
- `BookEntity` → `ShelfEntity`: Many-to-One (many books on one shelf)
- `ShelfEntity` → `BookcaseEntity`: Many-to-One (many shelves in one bookcase)

### Entity Relationship Diagram

```
BookcaseEntity (1) ─────────────< (Many) ShelfEntity
                                           │
                                           │ (Many)
                                           ↓
                                           │ (One)
                                        BookEntity
                                           │
                                           │ (Many-to-Many)
                                           ↓
                                      AuthorEntity

Notes:
- BookEntity.shelfId creates the relationship (currently using Long, should be @ManyToOne)
- book_authors join table connects books and authors
- Could add User entity for checkout tracking
- Could add CheckoutHistory for audit trail
```

### Current Issues and Improvements

**Issue #1: Denormalized Relationship**

**Current Code:**
```java
@Entity
public class BookEntity {
    private Long shelfId;  // Storing foreign key directly
}
```

**Problem:** This is database thinking, not object-oriented thinking. You're storing a foreign key instead of a relationship.

**Better Approach:**
```java
@Entity
public class BookEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private ShelfEntity shelf;  // Relationship to actual object
}
```

**Why This Matters:**
- **ORM Intent:** JPA maps objects, not foreign keys
- **Navigation:** Can do `book.getShelf().getBookcase()` instead of manual joins
- **Type Safety:** Compiler ensures you're working with `ShelfEntity`, not just `Long`
- **Cascade Operations:** Can define what happens when shelf is deleted

**Industrial Connection:**
"Like equipment tracking—you don't just store a facility ID number, you maintain the actual relationship to the facility object with all its properties and behaviors. The relationship carries meaning beyond the ID."

**Issue #2: Bi-directional Relationships Not Fully Utilized**

**Current:** `BookEntity` knows about `ShelfEntity` (via shelfId), but `ShelfEntity` doesn't know about its books.

**Better (Bi-directional):**
```java
@Entity
public class ShelfEntity {
    @OneToMany(mappedBy = "shelf", cascade = CascadeType.ALL)
    private List<BookEntity> books = new ArrayList<>();

    public void addBook(BookEntity book) {
        books.add(book);
        book.setShelf(this);  // Keep both sides in sync
    }

    public void removeBook(BookEntity book) {
        books.remove(book);
        book.setShelf(null);
    }
}

@Entity
public class BookEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private ShelfEntity shelf;
}
```

**Benefits:**
- Can query: `shelf.getBooks()` to get all books on a shelf
- Cascade operations: delete shelf → handle books appropriately
- Consistency: helper methods keep both sides synchronized

**Issue #3: Missing Audit Fields as Base Entity**

**Current:** `createdAt` and `updatedAt` duplicated in every entity

**Better (Inheritance):**
```java
@MappedSuperclass
public abstract class BaseEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters - no setters for createdAt
}

@Entity
public class BookEntity extends BaseEntity {
    // Inherits createdAt, updatedAt automatically
    // onCreate/onUpdate hooks fire automatically
}
```

**Benefits:**
- DRY: Define once, inherit everywhere
- Automatic: Timestamps managed by JPA lifecycle hooks
- Consistent: All entities have audit fields
- No manual management: Can't forget to set updatedAt

**Industrial Connection:**
"Like standardized equipment documentation—every piece of equipment has installation date, last maintenance date. Don't duplicate this across every equipment type; define it once in base specification."

### Refined Entity Model

**BookEntity (Complete):**
```java
@Entity
@Table(name = "books")
public class BookEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true)
    private String isbn;

    private String publisher;
    private Integer publicationYear;
    private Integer edition;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus status = BookStatus.AVAILABLE;

    private Integer checkoutCount = 0;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private ShelfEntity shelf;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<AuthorEntity> authors = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id")
    private CatalogEntity catalog;

    // Business methods
    public void checkout() {
        if (!status.isCheckoutAllowed()) {
            throw new IllegalStateException(
                String.format("Cannot checkout book in %s status", status)
            );
        }
        status = BookStatus.CHECKED_OUT;
        checkoutCount++;
    }

    public void checkin() {
        if (status != BookStatus.CHECKED_OUT) {
            throw new IllegalStateException("Book is not checked out");
        }
        status = BookStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == BookStatus.AVAILABLE;
    }

    // Getters and setters
}
```

**Key Improvements:**
1. ✅ Extends `BaseEntity` for audit fields
2. ✅ Uses `@ManyToOne` for shelf relationship
3. ✅ Uses `@Enumerated(EnumType.STRING)` for status (stores "AVAILABLE", not ordinal)
4. ✅ Business logic in entity (`checkout()`, `checkin()`)
5. ✅ Validation in business methods
6. ✅ Descriptive column annotations

## JPA Best Practices

### Practice #1: Choose the Right Fetch Strategy

**The Problem: N+1 Queries**

```java
// This triggers N+1 queries!
List<BookEntity> books = bookRepository.findAll();
for (BookEntity book : books) {
    System.out.println(book.getShelf().getShelfLabel());  // Each one queries DB!
}
```

**Why:**
- `findAll()` retrieves all books (1 query)
- Each `getShelf()` triggers a separate query (N queries for N books)
- Total: 1 + N queries

**Solution #1: Fetch Joins**

```java
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    @Query("SELECT b FROM BookEntity b LEFT JOIN FETCH b.shelf WHERE b.id = :id")
    Optional<BookEntity> findByIdWithShelf(@Param("id") Long id);

    @Query("SELECT DISTINCT b FROM BookEntity b " +
           "LEFT JOIN FETCH b.shelf " +
           "LEFT JOIN FETCH b.authors")
    List<BookEntity> findAllWithRelationships();
}
```

**Solution #2: Entity Graphs**

```java
@Entity
@NamedEntityGraph(
    name = "Book.withShelfAndAuthors",
    attributeNodes = {
        @NamedAttributeNode("shelf"),
        @NamedAttributeNode("authors")
    }
)
public class BookEntity extends BaseEntity {
    // ...
}

// In repository:
@EntityGraph("Book.withShelfAndAuthors")
List<BookEntity> findAll();
```

**When to Use Which Fetch Type:**

| Relationship | Default | Recommendation | Reason |
|--------------|---------|----------------|---------|
| `@ManyToOne` | EAGER | Use LAZY | Avoid loading related entities unnecessarily |
| `@OneToMany` | LAZY | Keep LAZY | Loading collections can be expensive |
| `@ManyToMany` | LAZY | Keep LAZY | Collections are expensive, load on demand |
| `@OneToOne` | EAGER | Depends | If truly always needed, EAGER is fine |

**Rule of Thumb:** Default to LAZY, use fetch joins or entity graphs when you need eager loading.

**Industrial Connection:**
"Like requesting equipment data—don't fetch full maintenance history, parts catalog, and vendor info if you just need the equipment serial number. Fetch only what you need (LAZY), specify when you need more (JOIN FETCH)."

### Practice #2: Separate Entity from DTO

**The Problem: Exposing Entities Directly**

```java
// DON'T DO THIS
@RestController
public class BookController {
    @GetMapping("/books/{id}")
    public BookEntity getBook(@PathVariable Long id) {
        return bookService.findById(id);  // Exposing entity directly
    }
}
```

**Why This Is Bad:**
- **Circular References:** Authors have books, books have authors → JSON serialization fails
- **Over-Fetching:** Client gets all entity data, even internal fields
- **Coupling:** API structure tied to database structure
- **Security:** Might expose sensitive fields
- **Lazy Loading:** Can trigger LazyInitializationException

**Better: DTOs (Data Transfer Objects)**

```java
// DTO for API responses
public class BookResponseDTO {
    private Long id;
    private String title;
    private String isbn;
    private Integer publicationYear;
    private BookStatus status;
    private List<AuthorSummaryDTO> authors;
    private ShelfLocationDTO shelfLocation;

    // Constructor, getters, setters
}

public class AuthorSummaryDTO {
    private Long id;
    private String fullName;
}

public class ShelfLocationDTO {
    private String shelfLabel;
    private String bookcaseLabel;
}

// Mapper to convert Entity ↔ DTO
@Component
public class BookMapper {
    public BookResponseDTO toResponseDTO(BookEntity entity) {
        BookResponseDTO dto = new BookResponseDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setIsbn(entity.getIsbn());
        dto.setPublicationYear(entity.getPublicationYear());
        dto.setStatus(entity.getStatus());

        // Map authors
        dto.setAuthors(entity.getAuthors().stream()
            .map(this::toAuthorSummary)
            .collect(Collectors.toList()));

        // Map shelf location
        if (entity.getShelf() != null) {
            dto.setShelfLocation(toShelfLocation(entity.getShelf()));
        }

        return dto;
    }

    private AuthorSummaryDTO toAuthorSummary(AuthorEntity author) {
        return new AuthorSummaryDTO(
            author.getId(),
            author.getFirstName() + " " + author.getLastName()
        );
    }

    private ShelfLocationDTO toShelfLocation(ShelfEntity shelf) {
        return new ShelfLocationDTO(
            shelf.getShelfLabel(),
            shelf.getBookcase().getBookcaseLabel()
        );
    }
}

// Controller now uses DTOs
@RestController
public class BookController {
    private final BookService bookService;
    private final BookMapper bookMapper;

    @GetMapping("/books/{id}")
    public BookResponseDTO getBook(@PathVariable Long id) {
        BookEntity book = bookService.findById(id);
        return bookMapper.toResponseDTO(book);
    }
}
```

**Benefits:**
- ✅ API contract decoupled from database schema
- ✅ No circular reference issues
- ✅ Control exactly what data is exposed
- ✅ Can version DTOs independently
- ✅ No lazy loading issues (DTO is fully populated)

**Industrial Connection:**
"Like equipment reports for different audiences—operators get operational data (DTO for operators), maintenance gets technical specs (DTO for maintenance), management gets cost summary (DTO for management). Same entity, different views."

### Practice #3: Use Optimistic Locking for Concurrent Updates

**The Problem: Lost Updates**

User A and User B both fetch book at version 1.
User A updates title, saves.
User B updates status, saves → **overwrites User A's title change!**

**Solution: Optimistic Locking**

```java
@Entity
public class BookEntity extends BaseEntity {

    @Version
    private Long version;  // JPA manages this automatically

    // When update is attempted with stale version, OptimisticLockException is thrown
}
```

**How It Works:**
1. User A fetches book (version = 1)
2. User B fetches book (version = 1)
3. User A saves changes → version increments to 2
4. User B tries to save with version = 1 → **Exception thrown**
5. User B must refetch and try again

**Service Layer Handling:**

```java
@Service
public class BookService {

    @Transactional
    public void updateBook(Long id, BookUpdateRequest request) {
        try {
            BookEntity book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

            // Apply updates
            book.setTitle(request.getTitle());
            // ... other fields

            bookRepository.save(book);  // Version check happens here

        } catch (OptimisticLockException e) {
            throw new ConcurrentUpdateException(
                "Book was modified by another user. Please refresh and try again."
            );
        }
    }
}
```

**Industrial Connection:**
"Like equipment maintenance lockout/tagout procedures—before working on equipment, you check if someone else has it locked out. If the 'version' (lockout state) has changed since you checked, you can't proceed. Safety mechanism for concurrent operations."

### Practice #4: Understand Cascade Types

**Cascade Types Explained:**

| Cascade Type | What It Does | When to Use |
|--------------|--------------|-------------|
| `PERSIST` | Save child when parent is saved | Parent owns child lifecycle |
| `MERGE` | Update child when parent is updated | Usually combined with PERSIST |
| `REMOVE` | Delete child when parent is deleted | Child can't exist without parent |
| `REFRESH` | Reload child when parent is reloaded | Rarely needed |
| `DETACH` | Detach child from persistence context | Rarely needed |
| `ALL` | All of the above | Use carefully, powerful |

**Example:**

```java
@Entity
public class ShelfEntity extends BaseEntity {

    @OneToMany(
        mappedBy = "shelf",
        cascade = {CascadeType.PERSIST, CascadeType.MERGE},
        orphanRemoval = true
    )
    private List<BookEntity> books = new ArrayList<>();
}
```

**What This Means:**
- `CascadeType.PERSIST`: When shelf is saved, unsaved books are also saved
- `CascadeType.MERGE`: When shelf is updated, books are also updated
- `orphanRemoval = true`: If book is removed from shelf.books list, it's deleted from DB

**DON'T use `CascadeType.REMOVE` here!**
Deleting a shelf shouldn't delete all books—they should just become orphaned or moved.

**When to Use `CascadeType.REMOVE`:**
- True parent-child relationships where child can't exist without parent
- Example: `Order` → `OrderLineItem` (line items have no meaning without order)

**Better for Books:**
```java
@OneToMany(mappedBy = "shelf", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private List<BookEntity> books = new ArrayList<>();

// When shelf is deleted, set books.shelf = null instead of deleting books
```

## Domain-Driven Design (DDD) Basics

Domain-Driven Design is about modeling software to match the business domain. Key concepts:

### Concept #1: Entities vs. Value Objects

**Entities:**
- Have unique identity
- Lifecycle matters (created, updated, deleted)
- Two entities with same data but different IDs are different

**Examples in Bibby:**
- `BookEntity`: Each book has unique ID, even if title/ISBN are same
- `AuthorEntity`: Each author is unique

**Value Objects:**
- No unique identity
- Defined entirely by their attributes
- Immutable
- Two value objects with same attributes are identical

**Example: Create Value Object for Address**

```java
// Value Object (immutable)
@Embeddable
public class Address {
    @Column(name = "street")
    private final String street;

    @Column(name = "city")
    private final String city;

    @Column(name = "state")
    private final String state;

    @Column(name = "postal_code")
    private final String postalCode;

    protected Address() {
        // JPA requires no-arg constructor
        this(null, null, null, null);
    }

    public Address(String street, String city, String state, String postalCode) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
    }

    // Only getters, no setters (immutable)
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }

    @Override
    public boolean equals(Object o) {
        // Compare by attributes, not identity
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, postalCode);
    }
}

// Used in entity
@Entity
public class LibraryBranch extends BaseEntity {
    @Embedded
    private Address address;
}
```

**Why Value Objects:**
- ✅ Type safety: Can't confuse address with other strings
- ✅ Validation: Address constructor can validate format
- ✅ Reusability: Same Address class for User, LibraryBranch, etc.
- ✅ Immutability: Can't accidentally modify address fields

### Concept #2: Aggregates and Aggregate Roots

**Aggregate:** Cluster of entities and value objects treated as a single unit.

**Aggregate Root:** The main entity in an aggregate that external objects reference.

**Example in Bibby:**

```
Aggregate: Bookcase
├── Aggregate Root: BookcaseEntity
└── Entities: ShelfEntity (1..n)
    └── Books are referenced, not owned
```

**Rules:**
1. External objects can only hold references to the aggregate root
2. Invariants within aggregate are enforced by root
3. Database transactions should not cross aggregate boundaries

**In Practice:**

```java
@Entity
public class BookcaseEntity extends BaseEntity {

    @OneToMany(mappedBy = "bookcase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShelfEntity> shelves = new ArrayList<>();

    private Integer shelfCapacity;

    // Aggregate root enforces invariants
    public void addShelf(ShelfEntity shelf) {
        if (shelves.size() >= shelfCapacity) {
            throw new CapacityExceededException(
                String.format("Bookcase can only hold %d shelves", shelfCapacity)
            );
        }
        shelves.add(shelf);
        shelf.setBookcase(this);  // Maintain relationship
    }

    public void removeShelf(ShelfEntity shelf) {
        shelves.remove(shelf);
        shelf.setBookcase(null);
    }

    // Only bookcase knows about shelf capacity constraint
}

// Service layer works with aggregate root
@Service
public class BookcaseService {

    @Transactional
    public void addShelfToBookcase(Long bookcaseId, ShelfEntity newShelf) {
        BookcaseEntity bookcase = bookcaseRepository.findById(bookcaseId)
            .orElseThrow();

        bookcase.addShelf(newShelf);  // Let aggregate root enforce rules

        bookcaseRepository.save(bookcase);  // Save aggregate
    }
}
```

**Benefits:**
- Business rules enforced in one place (aggregate root)
- Can't violate invariants by direct manipulation
- Clear transaction boundaries

**Industrial Connection:**
"Like facility management—facility manager (aggregate root) controls what equipment can be added to facility, enforces capacity constraints, manages relationships. You don't bypass the facility manager to add equipment directly."

### Concept #3: Bounded Contexts

**Bounded Context:** Explicit boundary within which a domain model applies.

**Example:** "Book" means different things in different contexts:

**Library Management Context (Bibby):**
- Book = physical item with location, status, checkout history
- Attributes: shelfId, status, checkoutCount

**Catalog Context:**
- Book = publication with metadata
- Attributes: title, ISBN, authors, publisher, genre

**Inventory Context:**
- Book = stock item with quantity, supplier, cost
- Attributes: quantity, supplierId, purchasePrice

**In Code:**

```java
// Library Management Context
@Entity
@Table(name = "books", schema = "library_mgmt")
public class BookEntity extends BaseEntity {
    private String isbn;
    private BookStatus status;
    private ShelfEntity shelf;
    // Operational attributes
}

// Catalog Context
@Entity
@Table(name = "publications", schema = "catalog")
public class PublicationEntity extends BaseEntity {
    private String isbn;  // Links to BookEntity
    private String title;
    private String description;
    private Set<AuthorEntity> authors;
    // Metadata attributes
}
```

**Integration Between Contexts:**

```java
@Service
public class BookCatalogIntegrationService {

    // Fetch publication details for a library book
    public PublicationDetails getPublicationDetails(String isbn) {
        // Call catalog context (could be different microservice)
        return catalogService.findByIsbn(isbn);
    }
}
```

**Benefits:**
- ✅ Each context has focused, coherent model
- ✅ Prevents "god objects" trying to represent everything
- ✅ Enables independent evolution of contexts

**Industrial Connection:**
"Like departmental systems—Engineering's 'equipment' data (specs, drawings), Operations' 'equipment' data (status, location), Finance's 'equipment' data (cost, depreciation). Same physical equipment, different perspectives, different systems."

## Architecture Decision Records (ADRs)

**What:** Lightweight documentation of significant architectural decisions.

**Why:**
- Future you (or teammates) will ask: "Why did we design it this way?"
- ADRs capture context, alternatives considered, and reasoning

**Format:**

```markdown
# ADR-001: Separate Entity and DTO Models

## Status
Accepted

## Context
We need to expose book data via REST API. We considered two approaches:
1. Return entities directly from controllers
2. Create separate DTO layer

Returning entities directly is simpler initially but causes problems:
- Circular references in JSON serialization (books ↔ authors)
- Lazy loading exceptions when relationships not fetched
- API coupled to database schema
- Can't version API independently
- Security risk of exposing internal fields

## Decision
We will use separate DTO classes for API requests/responses.
Entities remain internal to service layer.
Mapper classes handle Entity ↔ DTO conversion.

## Consequences

### Positive
- API contract decoupled from database schema
- Can evolve schema without breaking API
- Control exactly what data is exposed
- No lazy loading issues (DTOs fully populated)
- Easier to version API

### Negative
- More classes to maintain (entity + DTO + mapper)
- Boilerplate mapping code
- Need to keep DTOs in sync with entities

### Mitigation
- Use MapStruct for automated mapping (reduces boilerplate)
- Create DTO base classes for common patterns
- Keep DTOs in api package, entities in domain package
```

**ADR for Bibby: Shelf-Book Relationship**

```markdown
# ADR-002: BookEntity References ShelfEntity (Not Foreign Key)

## Status
Accepted

## Context
Need to model relationship between books and shelves. Two approaches:

**Option 1:** Store foreign key directly
```java
private Long shelfId;
```

**Option 2:** Use JPA @ManyToOne relationship
```java
@ManyToOne
private ShelfEntity shelf;
```

## Decision
Use @ManyToOne relationship to ShelfEntity object.

## Rationale
- **Object-oriented:** Work with objects, not IDs
- **Navigation:** Can traverse object graph: `book.getShelf().getBookcase()`
- **Type safety:** Compiler enforces correct types
- **Cascade operations:** JPA can manage cascades properly
- **Aligns with ORM philosophy:** JPA is about object mapping

This matches how we think about equipment relationships in operations:
equipment has a facility (the object), not just facilityId.

## Consequences

### Positive
- Type-safe navigation through object graph
- Better IDE support (autocomplete relationships)
- Can leverage JPA cascade operations
- More maintainable (relationship semantics clear)

### Negative
- Slightly more complex than storing Long
- Must understand fetch strategies (LAZY vs EAGER)
- Can cause N+1 queries if not careful

### Mitigation
- Default to FetchType.LAZY to avoid unnecessary loading
- Use @Query with JOIN FETCH when loading relationships needed
- Document fetch strategies in repository methods
```

**Where to Store ADRs:**

```
bibby/
├── docs/
│   ├── adr/
│   │   ├── 001-entity-dto-separation.md
│   │   ├── 002-shelf-book-relationship.md
│   │   ├── 003-optimistic-locking.md
│   │   └── template.md
```

**In Interviews:**

"When designing Bibby, I made deliberate architecture decisions and documented them in ADRs. For example, I chose to separate entities from DTOs to decouple the API from the database schema—I documented the alternatives, trade-offs, and rationale. This practice came from operations where we'd document procedure changes and the reasoning behind them."

## Exercises for This Section

### Exercise 1: Refactor Entity Relationships (6-8 hours)

**Tasks:**
1. Create `BaseEntity` with audit fields and JPA lifecycle hooks
2. Refactor `BookEntity.shelfId` to `@ManyToOne ShelfEntity shelf`
3. Add bi-directional relationship to `ShelfEntity`
4. Update all repository queries to use new relationships
5. Write tests ensuring relationships work correctly
6. Create migration script (if using Flyway/Liquibase)

**Deliverable:** Refactored entities, passing tests, migration script

### Exercise 2: Implement DTO Layer (4-6 hours)

**Tasks:**
1. Create `BookResponseDTO`, `BookRequestDTO`
2. Create `AuthorSummaryDTO`, `ShelfLocationDTO`
3. Create `BookMapper` with mapping methods
4. Update `BookController` to use DTOs
5. Write tests for mapper
6. Document DTO structure in API docs

**Deliverable:** Complete DTO layer, controllers using DTOs, tests passing

### Exercise 3: Add Optimistic Locking (2 hours)

**Tasks:**
1. Add `@Version` field to all entities
2. Create `ConcurrentUpdateException`
3. Update service methods to handle `OptimisticLockException`
4. Write test simulating concurrent updates
5. Document locking strategy

**Deliverable:** Optimistic locking implemented, tested

### Exercise 4: Create Address Value Object (2 hours)

**Tasks:**
1. Create `Address` as `@Embeddable` value object
2. Make it immutable (final fields, no setters)
3. Add to `LibraryBranch` entity (create if doesn't exist)
4. Implement proper `equals()` and `hashCode()`
5. Write tests

**Deliverable:** Address value object, used in entity

### Exercise 5: Write ADRs (3 hours)

**Create ADRs for:**
1. Entity-DTO separation (why, alternatives, trade-offs)
2. Shelf-book relationship design (@ManyToOne vs Long)
3. Optimistic locking vs pessimistic locking
4. Fetch strategy choices (LAZY default with JOIN FETCH)
5. Aggregate boundaries in Bibby

**Template:**
- Status (Proposed/Accepted/Deprecated)
- Context (what decision needed)
- Decision (what you chose)
- Rationale (why)
- Consequences (positive, negative, mitigation)

**Deliverable:** 5 ADRs in `docs/adr/` folder

### Exercise 6: Domain Model Diagram (2 hours)

**Tasks:**
1. Create visual diagram of Bibby's entity relationships
2. Show aggregates and their boundaries
3. Mark aggregate roots
4. Show cascade directions
5. Include cardinality (1:1, 1:N, N:M)
6. Tools: draw.io, PlantUML, or hand-drawn

**Deliverable:** Domain model diagram, saved in `docs/`

## Action Items for Week 8

### Critical (Must Complete)
1. ✅ Complete Exercise 1: Refactor entity relationships
2. ✅ Complete Exercise 2: Implement DTO layer
3. ✅ Complete Exercise 5: Write 5 ADRs
4. ✅ All tests passing after refactoring

### Important (Should Complete)
5. ⬜ Complete Exercise 3: Add optimistic locking
6. ⬜ Complete Exercise 6: Domain model diagram
7. ⬜ Update Bibby README with architecture section
8. ⬜ Code review all changes using production checklist from Section 04

### Bonus (If Time Permits)
9. ⬜ Complete Exercise 4: Create value objects
10. ⬜ Add JSR-303 validation annotations to entities
11. ⬜ Create database migration scripts (Flyway)
12. ⬜ Write technical blog post about your architecture decisions

## Key Takeaways

1. **Entities are objects, not tables.** Use `@ManyToOne` relationships, not foreign keys.

2. **DTOs decouple API from database.** Essential for maintainable, evolvable systems.

3. **Optimistic locking prevents lost updates.** Critical for concurrent environments.

4. **DDD brings structure.** Entities, value objects, aggregates—these patterns prevent chaos.

5. **ADRs preserve context.** Future you will thank present you for documenting decisions.

6. **Industrial thinking applies.** Aggregates = facility management. Bounded contexts = departmental systems.

7. **Architecture is about trade-offs.** Document alternatives, not just decisions.

## What's Next

Section 08: **Portfolio Project Architecture (Phase 1)**

We'll begin extending Bibby into an enterprise-grade system:
- Add REST API layer (Spring Web)
- Implement request validation
- Exception handling strategy
- API documentation (SpringDoc/OpenAPI)
- Preparing for cloud deployment

Your architecture foundation from this section will support everything we build next.

---

**Word Count:** ~7,100 words

**Time Investment Week 8:** 18-25 hours
- Entity refactoring: 6-8 hours
- DTO implementation: 4-6 hours
- ADR writing: 3 hours
- Testing and validation: 3-4 hours
- Documentation: 2-3 hours

**Expected Outcome:**
- Bibby architecture significantly improved
- Entity relationships proper JPA style
- DTO layer decoupling API from domain
- ADRs documenting key decisions
- Understanding of DDD concepts
- Can discuss architecture decisions in interviews

**Success Metrics:**
- All entity relationships using JPA annotations
- DTO layer fully implemented and tested
- 5 ADRs written and reviewed
- Domain model diagram created
- Can explain architecture to non-technical person
- Code review passes production checklist

---

*Architecture isn't about building cathedrals. It's about making deliberate decisions, understanding trade-offs, and documenting the reasoning. You're not just coding—you're designing systems that model real-world domains. That's what senior engineers do.*
