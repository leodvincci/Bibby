# Section 9: Data Ownership & Service Databases

**Part III: Building Microservices**

---

## The Hardest Problem in Microservices

Here's the uncomfortable truth: **splitting code is easy; splitting data is where microservices break**.

Every experienced architect has seen this pattern:
1. Team splits monolith into "microservices"
2. Services still share the same database
3. They claim victory: "We're doing microservices!"
4. Six months later: services are coupled, deploys are synchronized, nothing can scale independently
5. Realization: **they built a distributed monolith**

In this section, I'll show you **why data ownership is the cornerstone of microservices architecture**, and how to get it right using real examples from Bibby.

---

## Bibby's Current Data Architecture

Let's examine what we have today. From `application.properties:3-5`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
spring.datasource.username=amigoscode
spring.datasource.password=password
```

**One database. One schema. All entities.**

Here's the entity relationship diagram:

```
┌─────────────────┐
│   Bookcase      │
│  bookcaseId(PK) │◄─────┐
│  bookcaseLabel  │      │
│  shelfCapacity  │      │ FK: bookcaseId
└─────────────────┘      │
                         │
                    ┌────┴────────┐
                    │   Shelf     │
                    │ shelfId(PK) │◄─────┐
                    │ bookcaseId  │      │
                    │ shelfLabel  │      │ FK: shelfId
                    └─────────────┘      │
                                         │
                                    ┌────┴────────┐
                                    │   Book      │
                      ┌─────────────│  bookId(PK) │
                      │             │  shelfId    │
                      │             │  title      │
                      │             │  isbn       │
  ┌──────────────┐    │             │  bookStatus │
  │  book_authors│◄───┘             └─────────────┘
  │  (join table)│
  │  book_id     │────┐
  │  author_id   │◄───┼─────┐
  └──────────────┘    │     │
                      │     │
                 ┌────┴─────┴────┐
                 │    Author     │
                 │  authorId(PK) │
                 │  firstName    │
                 │  lastName     │
                 │  fullName     │
                 └───────────────┘
```

**Analysis:**
- Foreign key constraints span the entire schema
- Many-to-many relationship requires join table
- Complex queries join across 4 tables (see `BookRepository.getBookDetailView()`)
- All transactions happen in one ACID boundary

**This works beautifully in a monolith.** It's normalized, referentially consistent, and performant.

**But what happens when we try to split this into services?**

---

## Anti-Pattern: The Shared Database

Let's say we split Bibby into three services (from Section 8):
- **Catalog Service** - manages books, authors, genres
- **Library Service** - manages bookcases, shelves, physical organization
- **Circulation Service** - handles checkouts, returns, patron activities

**Naive approach:** Create three services, all pointing to the same database.

```
Catalog Service ──┐
                  ├──► PostgreSQL (amigos database)
Library Service ──┤
                  │
Circulation Svc ──┘
```

**Why this is a distributed monolith:**

### Problem 1: Schema Coupling

From `BookEntity.java:10-11, 22`:
```java
@Entity
@Table(name = "books")
public class BookEntity {
    private Long shelfId;  // ← FK to Library Service's domain!

    @ManyToMany
    private Set<AuthorEntity> authors;  // ← Join table shared with Catalog
}
```

**If Library Service changes the `shelves` table schema, Catalog Service breaks.** You can't deploy independently.

### Problem 2: Transaction Boundaries

From `BookService.java:22-41`:
```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    // Find or create author (Catalog domain)
    AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(
        firstName, lastName
    );
    if (authorEntity == null) {
        authorEntity = new AuthorEntity(firstName, lastName);
        authorRepository.save(authorEntity);  // Write to authors table
    }

    // Create book and link
    bookEntity.setAuthors(authorEntity);
    bookRepository.save(bookEntity);  // Write to books + book_authors
}
```

**This transaction spans two domains: Author (Catalog) and Book (Catalog).** In a monolith with one DB, `@Transactional` guarantees ACID properties.

**But if we split services:** Should this be in Catalog Service? Both authors and books are catalog concepts, so yes. But imagine if books were in Library Service and authors in Catalog Service—**you can't use @Transactional across service boundaries.**

### Problem 3: Cross-Domain Queries

From `BookRepository.java:27-39`:
```java
@Query(value = """
    SELECT b.book_id, b.title,
           STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
           bc.bookcase_label, s.shelf_label, b.book_status
    FROM books b
    JOIN book_authors ba ON b.book_id = ba.book_id
    JOIN authors a ON ba.author_id = a.author_id
    JOIN shelves s ON s.shelf_id = b.shelf_id           -- Library domain
    JOIN bookcases bc ON bc.bookcase_id = s.bookcase_id -- Library domain
    WHERE b.book_id = :bookId
    GROUP BY b.book_id, b.title, bc.bookcase_label, s.shelf_label, b.book_status
""", nativeQuery = true)
BookDetailView getBookDetailView(Long bookId);
```

**This query joins tables across Catalog and Library services.** If they have separate databases, this query is **impossible**.

### Problem 4: No Isolation

When multiple services share a database:
- **Locking conflicts:** Library Service query locks rows that Catalog Service needs
- **Connection pool exhaustion:** One service's traffic spike starves others
- **Schema migrations:** Require coordinated deploys across all services
- **Performance degradation:** One service's slow query impacts all

**Verdict: Shared database = Not microservices. Period.**

---

## The Database Per Service Pattern

**Core Principle:** Each service owns its data exclusively. No other service can access its database directly.

```
┌───────────────────┐
│ Catalog Service   │
│  ┌──────────────┐ │     ┌───────────────────┐
│  │ PostgreSQL   │ │     │ Library Service   │
│  │ - books      │ │     │  ┌──────────────┐ │
│  │ - authors    │ │     │  │ PostgreSQL   │ │
│  │ - genres     │ │     │  │ - bookcases  │ │
│  └──────────────┘ │     │  │ - shelves    │ │
└───────────────────┘     │  └──────────────┘ │
         ▲                └───────────────────┘
         │                         ▲
         │ REST/Events             │ REST/Events
         │                         │
┌────────┴─────────────────────────┴────┐
│       Circulation Service             │
│  ┌──────────────┐                     │
│  │ PostgreSQL   │                     │
│  │ - checkouts  │                     │
│  │ - patrons    │                     │
│  │ - holds      │                     │
│  └──────────────┘                     │
└───────────────────────────────────────┘
```

**Benefits:**
1. **Independent scaling:** Catalog DB can use read replicas; Library DB stays small
2. **Technology choice:** Use PostgreSQL for Catalog, MongoDB for Circulation, Redis for sessions
3. **Isolated failures:** Library DB crash doesn't affect Catalog queries
4. **Schema evolution:** Catalog can migrate schemas without coordinating with Library

**Challenges:**
1. **No ACID across services**
2. **No foreign key constraints across services**
3. **No JOIN queries across services**
4. **Data duplication** (when is it okay?)

Let's tackle each challenge.

---

## Challenge 1: Data Ownership Rules

**Who owns what data in Bibby?**

### Catalog Service Owns:
- Books table (title, ISBN, publisher, description)
- Authors table
- Genres table
- book_authors join table

**Reason:** These are **catalog concepts**—metadata about what books exist.

### Library Service Owns:
- Bookcases table
- Shelves table
- shelfId reference (where a book physically lives)

**Reason:** These are **physical organization** concepts.

### Circulation Service Owns:
- Checkouts table (who has what book, due date)
- Patrons table (library card holders)
- Holds/Reservations table

**Reason:** These are **lending workflow** concepts.

**The Key Question: Where does `bookStatus` live?**

Currently it's in `BookEntity.java:13`:
```java
private String bookStatus;  // AVAILABLE, CHECKED_OUT, LOST, etc.
```

**Two approaches:**

**Approach A: Catalog owns it** (status is metadata)
- **Pro:** Catalog can answer "is this book available?" queries
- **Con:** Circulation Service must call Catalog to update status after checkout
- **Con:** Creates circular dependency if Catalog needs checkout history

**Approach B: Circulation owns it** (status is operational state)
- **Pro:** Status changes are transactional with checkout records
- **Con:** Searching for available books requires calling Circulation API
- **Con:** Catalog can't answer "available books" queries alone

**Real-world decision:** Most library systems use **Approach B** and solve search with **CQRS** (read models). We'll cover this shortly.

---

## Challenge 2: When to Duplicate Data

**Anti-pattern:** Never duplicate data across services.

**Reality:** Strategic duplication is essential in microservices.

### Rule 1: Duplicate Reference Data

Reference data changes rarely and is read frequently.

**Example:** Catalog Service needs to display books on a shelf. It needs `shelfLabel` and `bookcaseLabel` for display.

**Bad approach:** Call Library Service API for every book display
```java
// In Catalog Service - DON'T DO THIS
for (Book book : books) {
    Shelf shelf = libraryClient.getShelf(book.getShelfId());  // N+1 query over HTTP!
    Bookcase bookcase = libraryClient.getBookcase(shelf.getBookcaseId());
    book.setDisplayLocation(bookcase.getLabel() + " - " + shelf.getLabel());
}
```

**Good approach:** Duplicate shelf/bookcase labels in Catalog's database
```java
// In Catalog Service
@Entity
public class Book {
    private Long bookId;
    private String title;

    // Cached reference data from Library Service
    private Long shelfId;           // Source of truth reference
    private String shelfLabel;      // Denormalized cache
    private String bookcaseLabel;   // Denormalized cache
}
```

**How to keep it in sync:** Listen to Library Service events (see Section 10):
```java
@EventListener
public void onShelfRenamed(ShelfRenamedEvent event) {
    bookRepository.updateShelfLabel(event.getShelfId(), event.getNewLabel());
}
```

**Trade-off:** Eventual consistency. After a shelf rename, Catalog displays old label for seconds/minutes until event processes.

### Rule 2: Don't Duplicate Transactional Data

**Example:** Circulation Service checkout flow needs book title for confirmation email.

**Bad approach:** Duplicate entire Book entity in Circulation DB
```java
// In Circulation Service - DON'T DO THIS
@Entity
public class Checkout {
    private String bookTitle;       // ❌ Will become stale
    private String bookIsbn;        // ❌ What if Catalog corrects a typo?
    private String authorNames;     // ❌ Circular dependency
}
```

**Good approach:** Store only the reference, fetch on-demand
```java
// In Circulation Service
@Entity
public class Checkout {
    private Long catalogBookId;  // ✅ Source of truth reference only

    // Fetch fresh data when needed
    public CheckoutConfirmation toConfirmation(CatalogClient client) {
        BookInfo book = client.getBook(catalogBookId);
        return new CheckoutConfirmation(book.getTitle(), dueDate, patronEmail);
    }
}
```

**When to cache:** For **display-only fields that change rarely** (book title, author name). Use TTL and accept stale reads.

---

## Challenge 3: Distributed Transactions (and Why They Fail)

Let's adapt Bibby's `createNewBook()` flow for a distributed setup. Imagine we split Authors into a separate Author Service (extreme example for teaching).

**Desired behavior:** Create author, then create book. Both must succeed or both must fail (atomicity).

### The 2PC (Two-Phase Commit) Trap

**Theory:** Coordinator asks each service to prepare, then commit.

```
Catalog Service          Author Service
     │                        │
     │──── PREPARE ──────────►│
     │                        │ Lock resources
     │◄──── READY ────────────│
     │                        │
     │──── COMMIT ────────────►│
     │                        │ Write & release
     │◄──── DONE ─────────────│
```

**Why it fails in practice:**

1. **Coordinator failure:** If Catalog crashes between PREPARE and COMMIT, Author Service is stuck with locks
2. **Timeout ambiguity:** Did Author Service commit before timing out, or not? Network partition makes this unknowable
3. **Locks under load:** Holding locks during network round-trips kills throughput
4. **Not truly supported:** Most modern databases/message queues don't implement XA transactions reliably

**Netflix learned this the hard way:** Their 2PC-based orchestration layer caused cascading failures during a network blip (2012 outage).

### The Saga Pattern (Choreography)

**Instead of atomicity, use eventual consistency with compensating transactions.**

**Scenario:** User creates a book with a new author.

**Saga steps:**
1. Catalog Service creates Book (status: PENDING_AUTHOR)
2. Catalog publishes `AuthorCreationRequested` event
3. Author Service creates Author
4. Author Service publishes `AuthorCreated` event
5. Catalog Service updates Book (status: ACTIVE, links authorId)

**Failure scenario:** Author Service can't create author (duplicate name, validation error).

**Compensation:**
1. Author Service publishes `AuthorCreationFailed` event
2. Catalog Service deletes the pending Book
3. User sees error: "Author creation failed; book was not created"

**Implementation in Bibby:**

```java
// Catalog Service
@Transactional
public void createBookWithNewAuthor(BookRequestDTO request) {
    // Step 1: Create book locally (PENDING state)
    Book book = new Book(request.getTitle(), BookStatus.PENDING_AUTHOR);
    bookRepository.save(book);

    // Step 2: Request author creation (async)
    eventPublisher.publish(new AuthorCreationRequested(
        book.getId(),
        request.getFirstName(),
        request.getLastName()
    ));

    // Don't wait for response - return immediately
    return new BookCreationResponse(book.getId(), "PENDING");
}

@EventListener
public void onAuthorCreated(AuthorCreatedEvent event) {
    // Step 5: Complete the book creation
    Book book = bookRepository.findById(event.getBookId());
    book.setAuthorId(event.getAuthorId());
    book.setStatus(BookStatus.ACTIVE);
    bookRepository.save(book);
}

@EventListener
public void onAuthorCreationFailed(AuthorCreationFailedEvent event) {
    // Compensate: Delete pending book
    bookRepository.deleteById(event.getBookId());
}
```

**Trade-offs:**
- ✅ No distributed locks
- ✅ Each service uses local transactions
- ✅ System eventually consistent
- ❌ More complex logic (state machines)
- ❌ User sees "pending" status (eventual consistency)
- ❌ Must handle compensation failures (what if delete fails?)

### The Saga Pattern (Orchestration)

**Alternative:** Central orchestrator coordinates the flow.

```java
// Book Creation Saga Orchestrator
public class BookCreationSaga {

    public void execute(BookRequestDTO request) {
        String sagaId = UUID.randomUUID().toString();

        try {
            // Step 1: Create author
            AuthorResponse author = authorService.createAuthor(
                request.getFirstName(),
                request.getLastName(),
                sagaId  // Pass saga ID for idempotency
            );

            // Step 2: Create book with author link
            BookResponse book = catalogService.createBook(
                request.getTitle(),
                author.getId(),
                sagaId
            );

            sagaRepository.recordSuccess(sagaId, book.getId());

        } catch (AuthorServiceException e) {
            // No compensation needed - nothing committed yet
            sagaRepository.recordFailure(sagaId, "Author creation failed");
            throw e;

        } catch (CatalogServiceException e) {
            // Compensate: Delete the author we created
            authorService.deleteAuthor(author.getId(), sagaId);
            sagaRepository.recordFailure(sagaId, "Book creation failed, author deleted");
            throw e;
        }
    }
}
```

**Trade-offs:**
- ✅ Clear failure handling logic
- ✅ Easier to reason about (sequential flow)
- ✅ Orchestrator maintains saga state
- ❌ Orchestrator is a single point of failure
- ❌ Orchestrator becomes a god service (knows about all services)
- ❌ Tight coupling to orchestrator

**When to use each:**
- **Choreography:** Simple flows (2-3 services), loose coupling preferred
- **Orchestration:** Complex flows (5+ steps), need centralized monitoring

---

## Event Sourcing: The Immutable Truth

Let's tackle the `checkOutBook()` race condition from `BookService.java:56-62`:

```java
public void checkOutBook(BookEntity bookEntity) {
    if (!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**Problem:** Two patrons try to check out the same book simultaneously.

```
Time    Thread A                    Thread B
───────────────────────────────────────────────────
t1      Read: status=AVAILABLE
t2                                  Read: status=AVAILABLE
t3      Write: status=CHECKED_OUT
t4                                  Write: status=CHECKED_OUT  ❌ Lost update
```

**Standard fix:** Optimistic locking with `@Version`.

**Event Sourcing approach:** Don't store current state. Store the **events that led to the state**.

### Event-Sourced Book Lifecycle

```java
// Event store (append-only log)
book_events
┌──────┬─────────┬─────────────────────┬──────────────────┐
│ seq  │ book_id │ event_type          │ event_data       │
├──────┼─────────┼─────────────────────┼──────────────────┤
│ 1    │ 42      │ BookAdded           │ {title: "1984"}  │
│ 2    │ 42      │ ShelfAssigned       │ {shelfId: 7}     │
│ 3    │ 42      │ BookCheckedOut      │ {patronId: 88}   │
│ 4    │ 42      │ BookReturned        │ {timestamp: ...} │
│ 5    │ 42      │ BookCheckedOut      │ {patronId: 99}   │
└──────┴─────────┴─────────────────────┴──────────────────┘
```

**Current state is derived by replaying events:**

```java
public Book rebuild(Long bookId) {
    List<BookEvent> events = eventStore.getEvents(bookId);
    Book book = new Book();

    for (BookEvent event : events) {
        book.apply(event);  // Apply each event in order
    }

    return book;  // Current state: CHECKED_OUT by patron 99
}
```

**Concurrency handling:** Events are appended with optimistic concurrency control.

```java
@Transactional
public void checkOutBook(Long bookId, Long patronId, int expectedVersion) {
    // Verify current version (last event sequence)
    int currentVersion = eventStore.getVersion(bookId);

    if (currentVersion != expectedVersion) {
        throw new ConcurrencyException("Book state changed since you read it");
    }

    // Append new event
    eventStore.append(new BookCheckedOut(
        bookId,
        patronId,
        Instant.now(),
        expectedVersion + 1  // New version
    ));
}
```

**Race condition resolved:**
```
Time    Thread A                              Thread B
─────────────────────────────────────────────────────────────────
t1      Read events: version=4, AVAILABLE
t2                                            Read events: version=4, AVAILABLE
t3      Append: BookCheckedOut(v5) ✅
t4                                            Append: BookCheckedOut(v5) ❌ Version conflict
```

Thread B gets `ConcurrencyException` and retries with fresh state.

**Benefits:**
1. **Audit trail:** Every state change is recorded (who checked out when)
2. **Time travel:** Rebuild state at any point in history
3. **Event replay:** Fix bugs by replaying events through corrected logic
4. **Analytics:** Query event log directly ("how many checkouts per book?")

**Costs:**
1. **Storage:** Events accumulate (use snapshots for old aggregates)
2. **Complexity:** Rebuilding state on every read is slow (solution: projections)
3. **Event schema:** Events are immutable; can't change old event formats

---

## CQRS: Separating Reads from Writes

**Problem:** Event sourcing makes **writes** robust, but **reads** slow.

Rebuilding `BookDetailView` from events (joining shelves + bookcases) would require:
1. Replay BookAdded, ShelfAssigned events
2. Call Library Service for shelf/bookcase labels
3. Aggregate author names from Author Service

**Too slow for user-facing queries.**

**CQRS (Command Query Responsibility Segregation):** Use different models for writes and reads.

```
┌─────────────────────────────────────────┐
│          WRITE SIDE                     │
│  ┌──────────────┐                       │
│  │ Command:     │                       │
│  │ CheckOutBook │──────►Event Store     │
│  └──────────────┘       (append-only)   │
│                              │           │
│                              │ Events    │
└──────────────────────────────┼───────────┘
                               │
                               ▼
                         ┌──────────┐
                         │Event     │
                         │Projector │
                         └────┬─────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
    ┌─────────┐        ┌───────────┐      ┌─────────────┐
    │ Read DB │        │ Search    │      │ Analytics   │
    │(Postgres)│        │(Elastic)  │      │(ClickHouse) │
    └─────────┘        └───────────┘      └─────────────┘
         │                    │                    │
┌────────┴────────────────────┴────────────────────┴────┐
│               READ SIDE                               │
│  Query: GetAvailableBooks ────► Search Index          │
│  Query: BookDetailView ────────► Read DB              │
│  Query: CheckoutReport ────────► Analytics DB         │
└───────────────────────────────────────────────────────┘
```

### Bibby CQRS Example

**Write side (Circulation Service):**
```java
// Command handler
public void handle(CheckOutBookCommand cmd) {
    // Validate business rules
    Book book = bookRepository.findById(cmd.getBookId());
    if (book.getStatus() == BookStatus.CHECKED_OUT) {
        throw new BookNotAvailableException();
    }

    // Append event
    eventStore.append(new BookCheckedOut(
        cmd.getBookId(),
        cmd.getPatronId(),
        Instant.now()
    ));
}
```

**Read side (Catalog Service):**
```java
// Event projector (background process)
@EventListener
public void onBookCheckedOut(BookCheckedOut event) {
    // Update denormalized read model
    jdbcTemplate.update(
        "UPDATE book_detail_view SET status = 'CHECKED_OUT', last_checkout = ? WHERE book_id = ?",
        event.getTimestamp(),
        event.getBookId()
    );
}

// Query handler
public BookDetailView getBookDetails(Long bookId) {
    // Fast read from projection (pre-joined data)
    return jdbcTemplate.queryForObject(
        "SELECT * FROM book_detail_view WHERE book_id = ?",
        bookDetailViewMapper,
        bookId
    );
}
```

**The `book_detail_view` table is a projection:**
```sql
CREATE TABLE book_detail_view (
    book_id         BIGINT PRIMARY KEY,
    title           VARCHAR(500),
    authors         TEXT,              -- Pre-joined: "David Thomas, Andrew Hunt"
    bookcase_label  VARCHAR(100),      -- Denormalized from Library Service
    shelf_label     VARCHAR(100),      -- Denormalized from Library Service
    status          VARCHAR(50),       -- Derived from Circulation events
    last_checkout   TIMESTAMPTZ,
    INDEX idx_status (status)
);
```

**This is exactly what Bibby's native query does today**, but now updated via events instead of real-time JOINs.

**Trade-offs:**
- ✅ Read queries are blazing fast (no JOINs, no service calls)
- ✅ Read models optimized per use case (search, reporting, UI)
- ✅ Write side unburdened by read performance concerns
- ❌ Eventual consistency (read model lags behind writes by seconds)
- ❌ Data duplication (same data in event store + N read models)
- ❌ Complexity (projector must handle event replay, failures)

---

## Migration Strategy: From Shared DB to Service DBs

**The problem:** Bibby has one database. We can't rewrite everything overnight.

**Strangler Fig pattern** (from Section 8) applies to data too.

### Step 1: Introduce Logical Schemas

**Before:**
```
amigos database
├── books
├── authors
├── shelves
├── bookcases
└── checkouts  (not yet added)
```

**After (same physical DB):**
```
amigos database
├── catalog schema
│   ├── books
│   └── authors
└── library schema
    ├── shelves
    └── bookcases
```

**Code change:** Update `@Table` annotations
```java
@Entity
@Table(name = "books", schema = "catalog")
public class BookEntity { ... }
```

**Benefit:** Services can't accidentally query wrong schema. Sets boundaries.

### Step 2: Remove Cross-Schema Foreign Keys

**Before:**
```sql
ALTER TABLE catalog.books
    ADD CONSTRAINT fk_shelf
    FOREIGN KEY (shelf_id) REFERENCES library.shelves(shelf_id);
```

**After:**
```sql
-- No foreign key constraint
-- Enforce referential integrity in application code
```

**BookService validation:**
```java
public void assignShelf(Long bookId, Long shelfId) {
    // Call Library Service to validate shelf exists
    ShelfResponse shelf = libraryClient.getShelf(shelfId);
    if (shelf == null) {
        throw new ShelfNotFoundException(shelfId);
    }

    // Now safe to store reference
    bookRepository.updateShelfId(bookId, shelfId);
}
```

### Step 3: Eliminate Cross-Schema Queries

**Before:** `getBookDetailView()` JOINs catalog + library schemas.

**After:** Build a projection.

```java
// Background job (runs every 5 minutes or on events)
public void rebuildBookDetailProjections() {
    List<Book> books = catalogRepo.findAll();

    for (Book book : books) {
        // Fetch library data
        ShelfResponse shelf = libraryClient.getShelf(book.getShelfId());
        BookcaseResponse bookcase = libraryClient.getBookcase(shelf.getBookcaseId());

        // Update projection table
        bookDetailRepo.upsert(new BookDetail(
            book.getId(),
            book.getTitle(),
            String.join(", ", book.getAuthorNames()),
            bookcase.getLabel(),
            shelf.getLabel(),
            book.getStatus()
        ));
    }
}
```

**Now `getBookDetailView()` reads from `book_details` projection table** (no JOINs).

### Step 4: Separate Physical Databases

**Finally, move schemas to separate databases.**

```
catalog_db (PostgreSQL)              library_db (PostgreSQL)
├── books                            ├── shelves
└── authors                          └── bookcases
```

**Update application.properties:**
```properties
# Catalog Service
spring.datasource.url=jdbc:postgresql://localhost:5432/catalog_db

# Library Service
spring.datasource.url=jdbc:postgresql://localhost:5433/library_db
```

**Critical:** Do this during a maintenance window. Use database migration tools (Liquibase, Flyway) to export/import data.

---

## Data Ownership Principles (Summary)

1. **Each service owns its data exclusively** - No cross-service database access
2. **Duplicate reference data strategically** - Cache labels, names; sync via events
3. **Don't duplicate transactional data** - Store references; fetch on-demand
4. **Use sagas for distributed workflows** - Choreography for simplicity, orchestration for complex flows
5. **Event sourcing for audit/compliance** - Immutable event log; derive state
6. **CQRS for read performance** - Separate read models; eventual consistency
7. **Migrate incrementally** - Logical schemas → Remove FKs → Projections → Separate DBs

---

## Real-World War Story: Uber's Schemaless

**Problem:** Uber grew from 1 database to 1000+ microservices. Foreign keys didn't scale.

**Solution (2014):** Built "Schemaless" - a data abstraction layer over MySQL/Cassandra that:
- Stores data as JSON blobs (no relational constraints)
- Uses append-only mutations (event sourcing lite)
- Rebuilds entity state from mutation log

**Outcome:**
- ✅ Services scaled independently
- ✅ Schema evolution without migrations
- ❌ Lost relational query power (no ad-hoc JOINs)
- ❌ Eventual consistency required application changes

**Lesson:** Data ownership forces trade-offs. Choose consistency model based on business requirements, not developer comfort.

---

## Anti-Patterns to Avoid

### 1. The Shared ORM

**Symptom:** Multiple services share a JPA entity library.

```java
// shared-models library (used by Catalog + Library)
@Entity
public class BookEntity {
    // ❌ Both services depend on same entity definition
}
```

**Why it's bad:**
- Schema change requires redeploying all services
- Entities encode business logic (which service's logic wins?)
- Tight coupling through shared code

**Fix:** Each service defines its own entities, even if similar.

### 2. The API Gateway Database

**Symptom:** API Gateway reads from service databases to stitch responses.

```java
// In API Gateway
public BookDetailDTO getBookDetail(Long id) {
    Book book = catalogDB.query("SELECT * FROM books WHERE id = ?", id);
    Shelf shelf = libraryDB.query("SELECT * FROM shelves WHERE id = ?", book.shelfId);
    // ❌ Gateway bypasses services, couples to schemas
}
```

**Why it's bad:** Gateway now owns data logic; services become dumb CRUD layers.

**Fix:** Gateway calls service APIs; services own aggregation logic.

### 3. The Event Soup

**Symptom:** Services publish 50+ event types; no one knows who consumes what.

**Why it's bad:**
- Impossible to deprecate events (might break unknown consumers)
- Event schema changes break downstream in production
- Debugging requires tracing events across 10 services

**Fix:**
- Document events in schema registry (Confluent Schema Registry, AsyncAPI)
- Version events explicitly (`BookCheckedOutV2`)
- Monitoring: track event consumers per topic

---

## Action Items

**For Bibby:**

1. **Add logical schemas** (30 minutes)
   - Create `catalog` and `library` schemas
   - Update `@Table` annotations
   - Verify separation with `SHOW TABLES FROM catalog;`

2. **Build a projection** (2 hours)
   - Create `book_details_view` table
   - Write background job to populate from JOINs
   - Replace `getBookDetailView()` query with projection read
   - Verify performance improvement

3. **Remove one foreign key** (1 hour)
   - Drop FK from `books.shelf_id` → `shelves.shelf_id`
   - Add application-level validation in `assignShelf()`
   - Test that invalid shelf IDs are rejected

4. **Implement a saga** (4 hours)
   - Choose: "Check out book + send email confirmation"
   - Write saga orchestrator
   - Add compensation logic (undo checkout if email fails)
   - Test rollback scenarios

**For your project:**

1. **Map data ownership** - Which service owns which tables?
2. **Identify cross-domain queries** - Grep for JOINs spanning services
3. **Start with projections** - Don't split DBs until queries are decoupled
4. **Choose consistency model** - Where can you accept eventual consistency?

---

## Further Reading

- **"Designing Data-Intensive Applications"** by Martin Kleppmann (Chapter 9: Consistency & Consensus)
- **Pat Helland: "Life Beyond Distributed Transactions"** (2007 paper - foundational)
- **Saga pattern:** Chris Richardson's microservices.io (concrete examples)
- **Event Sourcing:** Greg Young's talks (search "Event Sourcing Greg Young")
- **CQRS:** Martin Fowler's bliki (https://martinfowler.com/bliki/CQRS.html)

---

## Next Section Preview

**Section 10: Communication Patterns** will teach you:
- Synchronous vs asynchronous communication
- REST vs messaging (when to use each)
- Message brokers (Kafka, RabbitMQ)
- Event-driven architecture patterns
- The outbox pattern (reliable event publishing)
- Circuit breakers and retries

We'll analyze Bibby's `searchByTitle()` flow and show how message queues would change the design.

Ready? Let's talk about how services **communicate** once their data is separated.

---

**Word count:** ~3,700 words
