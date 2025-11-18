# Chapter 2: Domain-Driven Design (DDD) Basics

## Introduction: Why Microservices Without DDD Usually Fail

Let me tell you about the biggest mistake I see teams make when building microservices.

A company decided to "go microservices" and split their monolith into services. They made the split based on their database tables. One team looked at the schema and said, "We have a Users table, a Books table, an Orders table, and a Payments table. Let's make four services!"

Six months later, they had:
- A User Service that couldn't function without calling the Books Service
- An Orders Service that needed to join data from Users, Books, AND Payments
- A Payments Service that crashed when Orders went down
- Network calls everywhere, database calls nowhere useful

**What went wrong?** They split on technical boundaries instead of domain boundaries. They never asked, "What does our business actually *do*?" They never identified the natural seams in their problem space.

This is why you need Domain-Driven Design (DDD) before you build microservices. **DDD is how you find the right service boundaries.** It's the difference between a well-structured distributed system and a distributed monolith that makes everyone miserable.

In this chapter, I'm going to teach you DDD using a real system: **Bibby**, a library management application. We'll dissect its domain model, identify bounded contexts, understand aggregates, and learn how to think in domain terms. By the end, you'll know how to find natural service boundaries in any system.

## What Is Domain-Driven Design?

Domain-Driven Design, introduced by Eric Evans in 2003, is an approach to software design that focuses on:

1. **Modeling the business domain accurately** — Your code should reflect how the business actually works
2. **Speaking a common language** — Developers and domain experts use the same vocabulary
3. **Finding natural boundaries** — Identifying where concepts belong and where they should be separated
4. **Protecting business rules** — Ensuring invariants are always maintained

Here's the key insight that changed how I think about architecture:

> **Your service boundaries should follow your domain boundaries, not your technical architecture.**

If you can't explain WHY two pieces of functionality should be in separate services using **business language** (not technical language), they probably shouldn't be separate.

Let's see what this means in practice using Bibby's domain.

## Understanding Bibby's Domain

Bibby is a library management system for tracking physical books. Let's start with the business problem it solves:

**Business Goal**: Manage a personal library of physical books, track their locations on shelves in bookcases, and record checkout/check-in activity.

**Core Business Concepts**:
- **Books** — Physical items with titles, authors, editions
- **Authors** — People who write books (books can have multiple authors)
- **Shelves** — Physical storage locations in a bookcase
- **Bookcases** — Furniture holding multiple shelves
- **Checkout** — Borrowing a book (changes status, increments counter)
- **Catalog** — Organizing and searching the collection

Now, here's where DDD starts to matter. Let's look at Bibby's actual domain model and dissect it using DDD concepts.

## Domains, Subdomains, and Bounded Contexts

### The Domain

The **domain** is the problem space your software addresses. For Bibby, the domain is **library management**.

Within that domain, there are multiple **subdomains** — distinct areas of business functionality:

1. **Inventory Management** (Core subdomain)
   - What books do I own?
   - Where are they physically located?
   - What condition are they in?

2. **Circulation Management** (Core subdomain)
   - Check out and check in books
   - Track who has what
   - Manage due dates (potential future feature)

3. **Catalog & Discovery** (Supporting subdomain)
   - Search for books by title, author, genre
   - Browse by location
   - Recommendations (AI-assisted shelf organization)

4. **Metadata Management** (Generic subdomain)
   - Book titles, ISBNs, publication info
   - Author information
   - Genre classification

**Core vs Supporting vs Generic:**
- **Core subdomains** = competitive advantage, unique business value (how YOU manage circulation)
- **Supporting subdomains** = necessary but not differentiating (search functionality)
- **Generic subdomains** = solved problems you could buy (ISBN lookup services)

### Bounded Contexts

A **bounded context** is a clear boundary within which a particular model applies. Inside this boundary, terms have specific, consistent meanings.

Let's identify bounded contexts in Bibby:

#### Bounded Context 1: Library Management

**Purpose**: Track physical inventory and locations

**Ubiquitous Language**:
- Book (a physical item on a shelf)
- Shelf (a location with a label and position)
- Bookcase (furniture containing shelves)
- Location (bookcase + shelf)

**Models**:

Looking at Bibby's code (`src/main/java/com/penrose/bibby/library/`):

```java
// BookEntity.java — The core entity
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
    private Long shelfId;  // ← Physical location
    private Integer checkoutCount;
    private String bookStatus;

    @ManyToMany
    @JoinTable(name = "book_authors",
               joinColumns = @JoinColumn(name = "book_id"),
               inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<AuthorEntity> authors = new HashSet<>();
}
```

**Key insight**: In this context, a "Book" means a **physical copy** you can touch, shelve, and check out. That's different from a "Book" in an online bookstore catalog context, which might represent the abstract concept of a work (title + author).

#### Bounded Context 2: Catalog & Discovery

**Purpose**: Find and organize books for browsing

**Ubiquitous Language**:
- Book (a searchable item with metadata)
- Summary (lightweight representation for lists)
- Search result
- Collection

**Models**:

```java
// BookSummary.java — Read-optimized projection
public record BookSummary(Long bookId, String title) {}

// ShelfSummary.java — For browsing flow
public record ShelfSummary(Long shelfId, String label, long bookCount) {}
```

**Key insight**: In the Catalog context, you don't care about `checkoutCount` or `shelfId`. You care about discoverability. Notice how Bibby uses different models (summaries vs full entities) for different contexts.

Looking at the repository query that supports this:

```java
// BookRepository.java
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
```

This is a **read model** optimized for the Catalog context — it denormalizes data across multiple tables because the *catalog perspective* cares about "show me everything about this book in one view."

### Why Bounded Contexts Matter for Microservices

**This is the critical connection**: When you split a monolith into microservices, **bounded contexts become service boundaries**.

If Bibby were to become microservices, you might have:
- **Library Service** (manages physical inventory, shelving, locations)
- **Catalog Service** (search, browse, read-optimized views)
- **Circulation Service** (checkout, checkin, holds, due dates)

Each service would have **its own model of "Book"** appropriate to its context. That's not duplication — that's proper modeling.

## Entities: Things with Identity and Lifecycle

An **entity** is an object defined primarily by its identity, not its attributes. Two entities are the same if they have the same ID, even if all their other properties differ.

### Entities in Bibby

Let's look at `BookEntity`:

```java
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;  // ← The identity

    private String title;
    private Integer checkoutCount;
    private String bookStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // Book has a LIFECYCLE
    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }
}
```

**Why is Book an entity?**

1. **It has identity**: `bookId` uniquely identifies this book
2. **It has a lifecycle**: Created → Available → CheckedOut → Available → Archived
3. **Attributes change over time**: `checkoutCount` increments, `bookStatus` changes, but it's still the *same book*
4. **It's tracked individually**: You care about *this specific copy*, not just "a book with this title"

Let's look at the checkout flow from `BookService.java`:

```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**This is entity behavior**: The book's *state* changes over time, but it remains the same entity. The `bookId` never changes — that's its identity.

### Entity Equality

Entities are equal if their IDs match. Look at `Book.java` (the domain model):

```java
@Override
public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Book book = (Book) o;
    return Objects.equals(id, book.id);  // ← Equality based on ID only
}

@Override
public int hashCode() {
    return Objects.hashCode(id);
}
```

**Why this matters**: Two books with identical titles, authors, and ISBNs are still *different books* if they're different physical copies. That's entity thinking.

### Other Entities in Bibby

**AuthorEntity**:

```java
@Entity
@Table(name = "authors")
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long authorId;  // ← Identity

    private String firstName;
    private String lastName;
    private String fullName;  // Derived from first + last

    @ManyToMany(mappedBy = "authors")
    private Set<BookEntity> books = new HashSet<>();
}
```

**Why is Author an entity?** Because we track *individual authors* across multiple books. If "David Thomas" changes his name, we update one entity, and all his books reflect the change.

**ShelfEntity**:

```java
@Entity
@Table(name = "shelves")
public class ShelfEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;

    private String shelfLabel;  // "A1", "B2", etc.
    private Long bookcaseId;
    private int shelfPosition;
}
```

**Why is Shelf an entity?** Because you track *specific physical shelves* over time. Shelf A1 might get relabeled to A2, but it's still the same shelf (same `shelfId`).

## Value Objects: Things Defined by Attributes

A **value object** is defined entirely by its attributes, not by identity. Two value objects are equal if all their attributes match. Value objects are immutable.

### Value Objects in Bibby

#### BookSummary

```java
public record BookSummary(Long bookId, String title) {}
```

**Why is this a value object?**
- **No identity**: There's no "BookSummaryId" — the combination of `bookId` and `title` IS the value
- **Immutable**: Java records are immutable by default
- **Equality by attributes**: Two `BookSummary(1L, "Sapiens")` instances are identical
- **No lifecycle**: It's created, used, and discarded

This is used in browsing flows:

```java
// ShelfService.java
List<BookSummary> getBooksForShelf(Long shelfId) {
    return bookRepository.findBookSummariesByShelfIdOrderByTitleAsc(shelfId);
}
```

You don't care about the *identity* of this summary — you care about displaying (bookId, title) to the user. That's value object thinking.

#### ShelfSummary

```java
public record ShelfSummary(Long shelfId, String label, long bookCount) {}
```

Same pattern: immutable, no lifecycle, defined by its attributes. Used for read-heavy operations.

#### BookRequestDTO

```java
public record BookRequestDTO(String title, String firstName, String lastName) {}
```

This is a **value object for input**. It carries data from the CLI into the service layer:

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    String firstName = bookRequestDTO.firstName();
    String lastName = bookRequestDTO.lastName();
    String title = bookRequestDTO.title();
    // ... create entities from the DTO
}
```

**Why a DTO here?** Because the command "add a book with these attributes" is a value — it doesn't have identity or lifecycle. It's data, not an entity.

#### BookStatus Enum

```java
public enum BookStatus {
    AVAILABLE,
    CHECKED_OUT,
    RESERVED,
    LOST,
    ARCHIVED
}
```

**Classic value object**. The status "CHECKED_OUT" is the same everywhere, always. No identity, no lifecycle, immutable.

### The Critical Difference: Entities vs Value Objects

| **Entities** | **Value Objects** |
|--------------|-------------------|
| Defined by **identity** (ID) | Defined by **attributes** (values) |
| Mutable (state changes over time) | Immutable (never change) |
| Has a lifecycle (created → modified → deleted) | No lifecycle (created, used, discarded) |
| Equality by ID | Equality by all attributes |
| Examples: Book, Author, Shelf | Examples: BookSummary, BookStatus, Address |

**Rule of thumb**: If you care about tracking something over time, it's an entity. If you only care about what it IS (not which one it is), it's a value object.

## Aggregates and Aggregate Roots

An **aggregate** is a cluster of entities and value objects that form a consistency boundary. You interact with an aggregate through its **aggregate root** — the main entity that controls access to everything inside.

**Aggregates enforce business invariants.**

### The Book Aggregate in Bibby

Let's look at the Book aggregate:

```
┌─────────────────────────────────────┐
│     Book Aggregate                  │
│  ┌────────────────────────────┐    │
│  │ BookEntity (Aggregate Root)│    │
│  │ - bookId                   │    │
│  │ - title                    │    │
│  │ - checkoutCount            │    │
│  │ - bookStatus               │    │
│  └────────────────────────────┘    │
│              │                       │
│              │ owns                  │
│              ↓                       │
│  ┌────────────────────────────┐    │
│  │  Set<AuthorEntity>         │    │
│  │  (authors)                 │    │
│  └────────────────────────────┘    │
└─────────────────────────────────────┘
```

**Why is Book the aggregate root?**

1. **It enforces invariants**: You can't have a book without checking its status. Look at the checkout logic:

```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");  // ← Invariant: no double-checkout
        bookRepository.save(bookEntity);
    }
}
```

2. **It controls access to Authors**: You don't directly modify authors on a book—you go through the Book entity:

```java
// BookEntity.java
public void setAuthors(AuthorEntity authors) {
    this.authors.add(authors);  // ← Book controls the relationship
}
```

3. **The aggregate is saved as a unit**: When you save a `BookEntity`, the many-to-many relationship with Authors is managed transactionally:

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    // Find or create author
    AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(...);
    if (authorEntity == null) {
        authorEntity = new AuthorEntity(firstName, lastName);
        authorRepository.save(authorEntity);
    }

    // Create book and link to author
    BookEntity bookEntity = new BookEntity();
    bookEntity.setTitle(title);
    bookEntity.setAuthors(authorEntity);  // ← Book owns this relationship
    bookRepository.save(bookEntity);  // ← Aggregate saved as a unit
}
```

### Aggregate Boundaries and Consistency

**Key principle**: **Strong consistency within an aggregate, eventual consistency across aggregates.**

Within the Book aggregate:
- ✅ `bookStatus` and `checkoutCount` are always consistent
- ✅ The Book-Author relationship is transactionally managed
- ✅ If you save a book, its authors are linked atomically

Across aggregates:
- ❌ You can't guarantee that a `ShelfEntity` and its `BookEntity` children are always in sync (they're separate aggregates)
- ❌ A book references a shelf by ID (`shelfId`), not by object — this is intentional

Look at how `BookEntity` references Shelf:

```java
private Long shelfId;  // ← Reference by ID, not object reference!
```

**Why not a direct reference?** Because **Shelf is a separate aggregate**. If Book held a `ShelfEntity` object, you'd create a giant aggregate (Book + Shelf + Bookcase), and you'd need distributed transactions every time you moved a book.

By using `shelfId`, you accept eventual consistency: "This book *was* on this shelf the last time I checked."

### Aggregate Design Rules

1. **Keep aggregates small**: The Book aggregate is just Book + Authors. It doesn't include Shelf or Bookcase.
2. **Reference by ID across aggregates**: `shelfId`, `bookcaseId` — not object references.
3. **One repository per aggregate**: `BookRepository` manages the Book aggregate. `ShelfRepository` manages shelves.
4. **Modify one aggregate per transaction**: When you check out a book, you touch BookEntity, not ShelfEntity.

## Ubiquitous Language: Speaking the Domain

**Ubiquitous language** means developers and domain experts use the same vocabulary. The code should read like a conversation with a librarian.

### Ubiquitous Language in Bibby

Let's look at the CLI command:

```java
@Command(command = "check-out", description = "Check-Out a book from the library")
public void checkOutBook(){
    // ...
    BookEntity book = bookService.findBookByTitle(bookTitle);

    if (book.getBookStatus().equals("CHECKED_OUT")){
        System.out.println("This one's already off the shelf. No double-dipping on checkouts.");
    } else {
        bookService.checkOutBook(book);
        System.out.println("All set — " + bookTitle + " is checked out and ready to go.");
    }
}
```

**Notice the language**:
- `check-out` (not "updateBookStatus")
- `bookTitle` (not "itemName")
- `checkOutBook()` (not "setStatus('CHECKED_OUT')")
- "already off the shelf" (domain expert language!)

This isn't just naming — it's **encoding business rules in terms the business uses**.

### Bad Example: Technical Language Leaking

Imagine if Bibby's code looked like this:

```java
@Command(command = "update-record", description = "Modify database entry")
public void updateRecord(String id, String field, String value) {
    repository.update(id, field, value);
}
```

**What's wrong?**
- "update-record" — meaningless to a librarian
- "field", "value" — database terms, not domain terms
- No business rules — just technical operations

**The ubiquitous language test**: Could you show this code to a librarian and have them understand what it does? If not, you're not using domain language.

### Building Ubiquitous Language

1. **Listen to domain experts**: How do librarians actually talk? They say "check out a book," not "change the availability status of an item."

2. **Use domain terms in code**:
   ```java
   bookService.checkOutBook(book);  // ✅ Domain language
   bookService.updateStatus(book, Status.UNAVAILABLE);  // ❌ Technical language
   ```

3. **Model domain concepts explicitly**:
   ```java
   public enum BookStatus {
       AVAILABLE,
       CHECKED_OUT,    // ← These are real library terms
       RESERVED,
       LOST,
       ARCHIVED
   }
   ```

4. **Keep a glossary**: Bibby's README acts as a glossary:
   - **Bookcase**: furniture holding multiple shelves
   - **Shelf**: physical storage location
   - **Check-out**: borrowing a book (changes status, increments counter)

## Anti-Corruption Layers

When you integrate with external systems that use different models, you need an **anti-corruption layer** (ACL) to translate between contexts.

### Example: ISBN Lookup Service

Imagine Bibby integrates with an external ISBN service:

```java
// External API response (NOT our domain model!)
public class ISBNApiResponse {
    private String bookTitle;         // ← They call it "bookTitle"
    private String authorName;        // ← Single string
    private String publicationDate;   // ← String, not structured date
    private String category;          // ← They call it "category", we call it "genre"
}
```

**Without an anti-corruption layer**, you'd corrupt your domain model:

```java
// ❌ BAD: External model leaking into domain
public void importBook(ISBNApiResponse apiResponse) {
    book.setTitle(apiResponse.bookTitle);
    book.setAuthor(apiResponse.authorName);  // Wait, we have first/last name!
    book.setGenre(apiResponse.category);  // Wait, we use Genre enum!
}
```

**With an anti-corruption layer**:

```java
// ✅ GOOD: Translate at the boundary
public class ISBNAdapter {
    public BookEntity fromISBNApi(ISBNApiResponse apiResponse) {
        // Parse their format into our domain model
        String[] nameParts = apiResponse.authorName.split(" ");
        String firstName = nameParts[0];
        String lastName = nameParts[nameParts.length - 1];

        AuthorEntity author = new AuthorEntity(firstName, lastName);

        BookEntity book = new BookEntity();
        book.setTitle(apiResponse.bookTitle);
        book.setAuthors(author);
        book.setGenre(Genre.fromCategoryString(apiResponse.category));  // Map to our enum

        return book;
    }
}
```

**The ACL protects your domain model** from external changes. If the ISBN API changes, you only update the adapter.

## Practical DDD for Microservices

Now let's connect DDD to microservices design.

### Finding Service Boundaries with DDD

If you were to extract microservices from Bibby, here's how DDD guides you:

**Step 1: Identify Bounded Contexts**

- Library Management Context (inventory, shelving)
- Catalog & Search Context (discovery, browsing)
- Circulation Context (checkout, checkin, holds)

**Step 2: Map Contexts to Services**

```
┌──────────────────────┐
│  Library Service     │  Manages physical books, shelves, locations
│  - BookEntity        │  Owns: books, shelves, bookcases
│  - ShelfEntity       │
│  - BookcaseEntity    │
└──────────────────────┘

┌──────────────────────┐
│  Catalog Service     │  Search and discovery
│  - BookSummary       │  Owns: search index, read models
│  - SearchResults     │  Queries Library Service for book details
└──────────────────────┘

┌──────────────────────┐
│  Circulation Service │  Check-out/check-in
│  - CheckoutRecord    │  Owns: checkout history, due dates
│  - HoldRequest       │  Calls Library Service to update status
└──────────────────────┘
```

**Step 3: Define Service Contracts**

```java
// Library Service API
GET  /books/{id}           → Full book entity
POST /books                → Create book
PUT  /books/{id}/shelf     → Move to new shelf
GET  /shelves/{id}/books   → Books on a shelf

// Catalog Service API
GET  /search?q=sapiens     → BookSummary[]
GET  /books/{id}/summary   → BookSummary (read-optimized)

// Circulation Service API
POST /checkouts            → Check out a book
PUT  /checkouts/{id}/return → Check in a book
GET  /checkouts/overdue    → Late books
```

### Data Ownership and Duplication

**Key principle**: Each service owns its own data for its context.

- **Library Service** owns the authoritative `books` table
- **Catalog Service** has a denormalized, read-optimized copy for search
- **Circulation Service** has a `checkouts` table with `bookId` references

**Is this duplication?** No — it's **context-specific modeling**. The Catalog's "book" is different from the Library's "book."

### Events for Synchronization

When Library Service updates a book, it publishes an event:

```java
// Library Service
public void updateBook(BookEntity book) {
    bookRepository.save(book);
    eventPublisher.publish(new BookUpdatedEvent(book.getBookId(), book.getTitle(), ...));
}
```

Catalog Service subscribes:

```java
// Catalog Service
@EventListener
public void onBookUpdated(BookUpdatedEvent event) {
    searchIndexService.updateBook(event.getBookId(), event.getTitle());
}
```

**Eventual consistency**: The catalog might be slightly out of sync with Library Service, but that's acceptable for search.

## Real-World DDD Case Study: Refactoring Bibby

Let's walk through a realistic scenario: Bibby is growing, and you're considering microservices.

### Current State (Monolith)

```
┌────────────────────────────────────────┐
│           Bibby Monolith               │
│  ┌──────────────────────────────────┐ │
│  │ Book + Author + Shelf + Bookcase │ │
│  │ (all in one DB, one deployment)  │ │
│  └──────────────────────────────────┘ │
└────────────────────────────────────────┘
```

**Pain points**:
- Search queries slow down checkout operations (same DB)
- Can't deploy catalog updates without touching circulation code
- Catalog team wants to use Elasticsearch, but Library team is happy with Postgres

### DDD Analysis

**Identify aggregates**:
- Book Aggregate (Book + Authors)
- Shelf Aggregate (Shelf)
- Bookcase Aggregate (Bookcase)
- Checkout Aggregate (would exist if we built this feature)

**Identify bounded contexts**:
- Library Management (core)
- Catalog & Search (supporting)
- Circulation (core)

**Assess coupling**:
- Catalog reads from Library (query only, no updates)
- Circulation modifies Library (updates book status)

### The Extract Strategy

**Phase 1: Extract Catalog Service** (Low risk, high value)

Why first?
- Read-only dependency on Library
- Clear bounded context
- Different tech needs (wants Elasticsearch)

```
┌──────────────────┐       reads        ┌──────────────┐
│ Library Monolith │ ─────────────────▶ │   Catalog    │
│  (Postgres)      │                     │  Service     │
│                  │                     │(Elasticsearch)│
└──────────────────┘                     └──────────────┘
```

Implementation:
1. Catalog listens to `BookUpdatedEvent` from Library
2. Builds read-optimized search index
3. Provides fast search API
4. If Catalog goes down, Library still works

**Phase 2: Extract Circulation Service** (Higher risk, clear boundary)

```
┌──────────────┐      ┌────────────────┐      ┌──────────┐
│   Catalog    │      │    Library     │      │Circulation│
│   Service    │◀────▶│   Service      │◀────▶│ Service   │
└──────────────┘      └────────────────┘      └──────────┘
      │                       │                      │
    Search               Inventory              Checkouts
```

Why second?
- Circulation has write dependencies on Library
- Needs sagas for distributed transactions (checkout = update library + create checkout record)
- Higher complexity, but clear domain boundary

### The Modular Monolith Alternative

**What if we DON'T go microservices?**

Keep Bibby as a monolith, but enforce DDD boundaries:

```
┌────────────────────────────────────────┐
│         Bibby Modular Monolith         │
│  ┌────────────────────────────────┐   │
│  │  Library Module (strict API)   │   │
│  │  - No direct DB access from    │   │
│  │    other modules                │   │
│  └────────────────────────────────┘   │
│  ┌────────────────────────────────┐   │
│  │  Catalog Module (strict API)   │   │
│  │  - Has own tables for search   │   │
│  └────────────────────────────────┘   │
│  ┌────────────────────────────────┐   │
│  │ Circulation Module             │   │
│  └────────────────────────────────┘   │
│                                        │
│    One deployment, strict boundaries   │
└────────────────────────────────────────┘
```

Benefits:
- 80% of microservices benefits (modularity, team ownership)
- 20% of the complexity (no network, no distributed transactions)
- Easy refactoring within the monolith
- Can extract services later when proven necessary

## Action Items

Before moving to Chapter 3, complete these exercises using Bibby:

### 1. Map Bibby's Domain Model

Draw a diagram showing:
- All entities (Book, Author, Shelf, Bookcase)
- All value objects (BookSummary, ShelfSummary, BookStatus, BookRequestDTO)
- Aggregate boundaries (which entities cluster together?)
- Relationships (Book ↔ Author many-to-many, Book → Shelf by ID)

### 2. Identify Bounded Contexts

For each context below, list:
- What entities/value objects belong in it?
- What's the core responsibility?
- What invariants must it protect?

Contexts:
- Library Management
- Catalog & Search
- Circulation (check-out/check-in)

### 3. Refactor for Ubiquitous Language

Find one place in Bibby's code where technical language leaks in. Refactor it to use domain language.

Example:
```java
// Before (technical)
bookRepository.update(bookId, "status", "2");

// After (domain language)
bookService.checkOutBook(book);
```

### 4. Design an Anti-Corruption Layer

If Bibby integrated with an external book metadata API (like Google Books API), design an adapter that translates their model to Bibby's domain model.

```java
public class GoogleBooksAdapter {
    public BookEntity fromGoogleBooksApi(GoogleBookResponse apiResponse) {
        // Your implementation here
    }
}
```

### 5. Decide: Monolith vs Microservices

Given Bibby's current size and complexity, answer:
- Should it remain a monolith?
- Should it become a modular monolith with strict boundaries?
- Should it extract one service (if so, which)?
- What would justify moving to microservices?

## Key Takeaways

1. **DDD is about modeling your business accurately** — Use the language of librarians, not database admins

2. **Bounded contexts define service boundaries** — Each context has its own model, its own rules, its own truth

3. **Entities have identity and lifecycle** — `BookEntity` is tracked over time by `bookId`

4. **Value objects are defined by attributes** — `BookSummary(1, "Sapiens")` is just data, no identity

5. **Aggregates enforce consistency boundaries** — Book + Authors form a transactional unit

6. **Reference across aggregates by ID, not objects** — `shelfId`, not `ShelfEntity`, to avoid giant aggregates

7. **Ubiquitous language keeps code aligned with business** — `checkOutBook()`, not `updateStatus()`

8. **Anti-corruption layers protect your domain** — Don't let external APIs corrupt your model

9. **DDD guides microservices design** — Bounded contexts → Service boundaries

10. **Modular monoliths are underrated** — You can apply DDD without going distributed

## Further Reading

### Essential Books
- **"Domain-Driven Design: Tackling Complexity in the Heart of Software"** by Eric Evans — The original, still the best
- **"Implementing Domain-Driven Design"** by Vaughn Vernon — Practical patterns and examples
- **"Domain-Driven Design Distilled"** by Vaughn Vernon — Shorter, focused introduction

### Articles & Talks
- **"Bounded Contexts"** by Martin Fowler — Clear explanation of context boundaries
- **"Aggregates and Entities in Domain-Driven Design"** by Vaughn Vernon
- **"Strategic Domain-Driven Design"** — Finding bounded contexts

### Real-World Examples
- **"How Uber Applies DDD"** — Bounded contexts at scale
- **"Shopify's Domain Model"** — Modeling e-commerce domains
- **"Domain-Driven Design at Scale"** — Case studies from large systems

---

## What's Next?

In **Chapter 3: Architecture of Distributed Systems**, we'll explore what happens when you split bounded contexts across network boundaries. You'll learn:
- The fundamental challenges of distributed systems
- CAP theorem and its practical implications
- Consistency models (strong, eventual, causal)
- Network failures as the default state
- Latency as a cost you must design around
- The fallacies of distributed computing

**Remember**: You've just learned how to find the RIGHT service boundaries using DDD. Next, you'll learn what it actually means to deploy those boundaries as separate services — and why it's harder than you think.

DDD without distributed systems knowledge = good monoliths.
Distributed systems without DDD knowledge = distributed monoliths.
DDD + distributed systems knowledge = well-architected microservices.

Let's learn the distributed systems part next.
