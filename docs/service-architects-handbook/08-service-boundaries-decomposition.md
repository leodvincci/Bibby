# Chapter 8: Service Boundaries & Decomposition

## Introduction: The Million Dollar Question

You're staring at Bibby's monolithic codebase. Your manager says "we need to go microservices." You ask: "Where do we draw the lines?"

This is the hardest question in microservices architecture. Draw the lines wrong, and you get:
- Services that can't be deployed independently (distributed monolith)
- Database calls turned into network calls (10x slower)
- Coordination overhead that crushes productivity
- A mess that's worse than the monolith you started with

Draw the lines right, and you get:
- Teams that can deploy independently
- Services that scale independently
- Clear ownership and accountability
- Systems that are easier to reason about

In this chapter, I'll teach you how to find the RIGHT service boundaries in Bibby using Domain-Driven Design, coupling analysis, and real-world judgment. By the end, you'll know exactly how to split (or not split) any system.

## Finding Natural Service Boundaries

Let's analyze Bibby's current structure:

```
Bibby Monolith
├── Book (entity, service, repository, controller)
│   └── Authors (many-to-many)
├── Author (entity, service, repository, controller)
├── Shelf (entity, service, repository, controller)
├── Bookcase (entity, service, repository, controller)
└── Catalog (service - aggregates data)
```

**The naive approach**: "One entity = one service"

```
❌ BAD SPLIT:
┌────────────────┐
│ Book Service   │ (manages books)
└────────────────┘

┌────────────────┐
│ Author Service │ (manages authors)
└────────────────┘

┌────────────────┐
│ Shelf Service  │ (manages shelves)
└────────────────┘

┌────────────────┐
│Bookcase Service│ (manages bookcases)
└────────────────┘
```

**Why this is wrong**:

Looking at Bibby's `searchByTitle()` operation:

```java
// Current monolith (one method call)
BookEntity book = bookService.findBookByTitle(title);
Optional<ShelfEntity> shelf = shelfService.findShelfById(book.getShelfId());
Optional<BookcaseEntity> bookcase = bookcaseService.findBookCaseById(shelf.get().getBookcaseId());
```

**After splitting into separate services**:

```
Client calls Book Service:
  GET /books/42 → {title, shelfId: 7}

Client calls Shelf Service:
  GET /shelves/7 → {shelfLabel: "A1", bookcaseId: 3}

Client calls Bookcase Service:
  GET /bookcases/3 → {bookcaseLabel: "Main"}

Result: 3 network round trips (was 1 method call)
```

Even worse, look at the `BookDetailView` query:

```java
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

**This single query would become**:
```
1. GET /books/42
2. GET /authors?bookId=42 (might return multiple)
3. GET /shelves/7
4. GET /bookcases/3

4 network calls, manual data aggregation in the client
```

**The lesson**: Don't split by entities. Split by **business capabilities**.

## Applying DDD Bounded Contexts

Remember from Chapter 2: **Bounded contexts are natural service boundaries.**

Let's identify Bibby's bounded contexts:

### Context 1: Library Management

**Purpose**: Manage the physical inventory of books

**Entities**:
- Book (physical copy with status, checkout count)
- Shelf (physical location)
- Bookcase (furniture)

**Business Capabilities**:
- Track book inventory
- Assign books to physical locations
- Manage shelf capacity

**Ubiquitous Language**:
- "Book" = physical item you can touch
- "Shelf" = physical storage location with label
- "Location" = bookcase + shelf combination

**Data that changes together**:
- When you move a book, you update shelf assignment
- When you add a shelf, you update bookcase capacity
- Book and shelf are tightly coupled

**Team ownership**: Library Operations Team

### Context 2: Catalog & Discovery

**Purpose**: Help users find books

**Entities**:
- BookSummary (lightweight, read-optimized)
- SearchResult
- ShelfSummary (for browse flow)

**Business Capabilities**:
- Search books by title, author, genre
- Browse by location (bookcase → shelf → books)
- Get book recommendations

**Ubiquitous Language**:
- "Book" = searchable item with metadata
- "Search result" = matching books
- "Browse" = navigate hierarchy

**Data that changes together**:
- Search index updates when books are added
- Book metadata for discovery purposes
- Read-optimized denormalized views

**Team ownership**: Search & Discovery Team

### Context 3: Circulation

**Purpose**: Manage checkouts and returns

**Entities**:
- Checkout (transaction record)
- User
- DueDate

**Business Capabilities**:
- Check out books
- Check in books
- Manage overdue books
- Track checkout history

**Ubiquitous Language**:
- "Checkout" = borrowing transaction
- "Due date" = when book must be returned
- "Overdue" = past due date

**Data that changes together**:
- Checkout status and book availability
- Due dates and user records
- Checkout history

**Team ownership**: Circulation Team

### Context 4: Authorship

**Purpose**: Manage author information

**Entities**:
- Author
- Biography
- Publications

**Business Capabilities**:
- Maintain author records
- Link authors to books
- Manage author metadata

**Ubiquitous Language**:
- "Author" = person who wrote books
- "Bibliography" = all works by an author

**Data that changes together**:
- Author name and biography
- Author-book relationships

**Team ownership**: Metadata Team

## The Right Split for Bibby

Based on bounded contexts and business capabilities:

```
┌─────────────────────────────────┐
│     Library Service             │
│  ┌──────────────────────────┐  │
│  │ Book (inventory)         │  │
│  │ Shelf (physical loc)     │  │
│  │ Bookcase (furniture)     │  │
│  └──────────────────────────┘  │
│                                 │
│  Responsibilities:              │
│  - Track physical inventory     │
│  - Manage locations             │
│  - Book availability status     │
│                                 │
│  Database: library_db           │
│  Tables: books, shelves,        │
│          bookcases              │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│     Catalog Service             │
│  ┌──────────────────────────┐  │
│  │ BookSummary (read model) │  │
│  │ SearchIndex              │  │
│  │ ShelfSummary             │  │
│  └──────────────────────────┘  │
│                                 │
│  Responsibilities:              │
│  - Search and discovery         │
│  - Denormalized read views      │
│  - Browse flows                 │
│                                 │
│  Database: catalog_db           │
│  (Elasticsearch or read DB)     │
│                                 │
│  Syncs from Library events      │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│   Circulation Service           │
│  ┌──────────────────────────┐  │
│  │ Checkout                 │  │
│  │ User                     │  │
│  │ CheckoutHistory          │  │
│  └──────────────────────────┘  │
│                                 │
│  Responsibilities:              │
│  - Checkout/checkin flow        │
│  - Due date management          │
│  - Overdue tracking             │
│                                 │
│  Database: circulation_db       │
│  Tables: checkouts, users       │
│                                 │
│  Calls Library Service to       │
│  update book status             │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│     Author Service (optional)   │
│  ┌──────────────────────────┐  │
│  │ Author                   │  │
│  │ Biography                │  │
│  └──────────────────────────┘  │
│                                 │
│  Database: author_db            │
│  Tables: authors                │
└─────────────────────────────────┘
```

**Why this split?**

1. **Library and Catalog are separate because**:
   - Different data models (BookEntity vs BookSummary)
   - Different update patterns (Library changes rarely, Catalog queries constantly)
   - Different scaling needs (Catalog needs read replicas, Library doesn't)
   - Different teams (Operations vs Search)

2. **Circulation is separate because**:
   - Different lifecycle (checkouts are temporal, books are persistent)
   - Different business rules (due dates, fines)
   - Can deploy independently (checkout logic changes often)

3. **Authors might stay with Library because**:
   - Tightly coupled (Book-Author many-to-many)
   - Change together (adding a book often adds an author)
   - Small entity (doesn't justify separate service overhead)

## Service Granularity: How "Micro" Should Microservices Be?

**The wrong question**: "How small should services be?"
**The right question**: "How independently can we deploy and scale this?"

### The Single Responsibility Principle at Service Scale

From Bibby's code, look at `BookService`:

```java
public class BookService {
    public void createNewBook(BookRequestDTO bookRequestDTO) { ... }
    public BookEntity findBookByTitle(String title) { ... }
    public void checkOutBook(BookEntity bookEntity) { ... }
    public void checkInBook(String bookTitle) { ... }
    public List<BookEntity> findBooksByShelf(Long id) { ... }
    public List<BookSummary> getBooksForShelf(Long shelfId) { ... }
    public BookDetailView getBookDetails(Long bookId) { ... }
}
```

**Analysis**: This service has multiple responsibilities:
- Inventory management (create, find)
- Circulation (checkout, checkin)
- Search/browse (getBooksForShelf, getBookDetails)

**Should we split this into three services?**

**Option A: Split aggressively** (3 services)

```
BookInventoryService: create, find
BookCirculationService: checkout, checkin
BookBrowseService: getBooksForShelf, getBookDetails
```

**Problems**:
- `checkout` needs to update book status → calls BookInventoryService
- `getBooksForShelf` needs book data → calls BookInventoryService
- Distributed transactions everywhere
- 3x operational overhead

**Option B: Keep together, separate concerns internally**

```java
// Library Service (single deployment)

// Module 1: Inventory
@Service
public class BookInventoryService {
    public void createNewBook(...) { ... }
    public BookEntity findBookByTitle(...) { ... }
}

// Module 2: Circulation (delegates to inventory)
@Service
public class BookCirculationService {
    @Autowired
    private BookInventoryService inventoryService;

    public void checkOutBook(Long bookId) {
        BookEntity book = inventoryService.findById(bookId);
        book.setStatus(CHECKED_OUT);
        inventoryService.save(book);
    }
}

// Module 3: Browse
@Service
public class BookBrowseService {
    public List<BookSummary> getBooksForShelf(...) { ... }
}
```

**Benefits**:
- Clear separation of concerns
- No network overhead (in-process calls)
- Can extract later if needed
- Single deployment (simpler ops)

**The principle**: **Start with modules, extract services only when you have a clear reason.**

### The Two-Pizza Team Rule Applied to Bibby

From Chapter 1, remember: **One team should own one service.**

**Bibby's services mapped to teams** (hypothetical scale-up):

```
Library Service (6 engineers)
├── Inventory management
├── Shelf/bookcase management
└── Physical book tracking

Catalog Service (5 engineers)
├── Search infrastructure
├── Elasticsearch indexing
└── Browse UI/API

Circulation Service (4 engineers)
├── Checkout/checkin flows
├── User management
└── Due date system

Author Service (2 engineers) ← Too small!
├── Author CRUD
└── Biography management
```

**Problem**: Author Service only needs 2 engineers. That's operational overhead (CI/CD, monitoring, on-call) for a tiny service.

**Solution**: Merge Author into Library Service.

**Rule of thumb**: If a service can't keep 4-6 engineers busy, it's probably too small.

## Avoiding the Distributed Monolith

The **distributed monolith** is the worst of both worlds: complexity of microservices, coupling of a monolith.

### Symptoms of a Distributed Monolith

**1. Services that can't deploy independently**

```
// Circulation Service v1.2
public void checkoutBook(Long bookId, Long userId) {
    CheckoutRequest request = new CheckoutRequest(bookId, userId);
    // Calls Library Service v1.2 (requires specific version!)
    libraryService.reserveBook(request);
}

// Library Service v1.2
public void reserveBook(CheckoutRequest request) {
    // Breaking change: added required field "dueDate"
    if (request.dueDate == null) {
        throw new IllegalArgumentException("dueDate required");
    }
}
```

**Problem**: Can't deploy Library v1.2 without deploying Circulation v1.2. They're coupled.

**Solution**: Backward compatibility. Library v1.2 should accept requests from Circulation v1.1:

```java
public void reserveBook(CheckoutRequest request) {
    LocalDate dueDate = request.dueDate != null
        ? request.dueDate
        : LocalDate.now().plusDays(14);  // Default for old clients
}
```

**2. Shared database**

```
❌ WRONG:
┌────────────────┐      ┌────────────────┐
│    Library     │      │   Circulation  │
│    Service     │      │    Service     │
└───────┬────────┘      └────────┬───────┘
        │                        │
        └───────────┬────────────┘
                    ↓
            ┌───────────────┐
            │ Shared DB     │
            │ books table   │
            └───────────────┘
```

**Why it's wrong**:
- Can't deploy independently (schema changes affect both)
- Can't scale database independently
- Tight coupling at data layer

**Right approach**: Each service owns its data

```
✅ RIGHT:
┌────────────────┐      ┌────────────────┐
│    Library     │      │   Circulation  │
│    Service     │      │    Service     │
└───────┬────────┘      └────────┬───────┘
        ↓                        ↓
┌───────────────┐      ┌────────────────┐
│ Library DB    │      │ Circulation DB │
│ books table   │      │ checkouts table│
│               │      │ book_id (ref)  │
└───────────────┘      └────────────────┘
```

Circulation stores `book_id` as a reference, not a foreign key.

**3. Synchronous call chains**

```
❌ WRONG:
Client → API Gateway → Circulation → Library → Author → ???

If Author Service is down, checkout fails!
```

**Right approach**: Asynchronous, event-driven

```
✅ RIGHT:
Client → API Gateway → Circulation
                         ↓
                    Publishes event
                         ↓
                  ┌──────┴──────┐
                  ↓             ↓
             Library        Analytics
            (updates)      (records)
```

Circulation doesn't wait for Library to respond.

**4. God service (anti-pattern)**

```
❌ Bibby "Orchestrator Service":
┌─────────────────────────────────┐
│  Bibby Orchestrator Service     │
│  - checkoutBook()                │
│    → calls Library               │
│    → calls Circulation           │
│    → calls Notification          │
│    → calls Analytics             │
│  - searchBooks()                 │
│    → calls Library               │
│    → calls Author                │
│    → calls Catalog               │
└─────────────────────────────────┘
```

This is just a monolith with extra steps (and network latency).

**Right approach**: Let services call each other directly, or use events.

### How to Detect Coupling

**Exercise for Bibby**: Draw a dependency graph

```
Library ←──── Circulation (reads book status)
  ↓
  └────────→ Catalog (publishes book events)

Author ←──── Library (references authors)

Shelf ←──── Library (shelf assignment)
```

**Questions to ask**:

1. **Can Library deploy without Circulation?** Yes (Circulation depends on Library, not vice versa)

2. **Can Circulation deploy without Library?** No (needs Library to verify book exists)
   - **Fix**: Cache book data in Circulation, use eventual consistency

3. **Can Catalog deploy without Library?** No (consumes Library events)
   - **Acceptable**: Event consumers can be unavailable temporarily

4. **What happens if Library is down?**
   - Checkout fails (acceptable — can't checkout without checking availability)
   - Search still works (Catalog has its own data)
   - Browse still works (Catalog has denormalized data)

**The goal**: Minimize synchronous dependencies, maximize autonomy.

## Functional vs Domain Decomposition

**Functional decomposition** (by technical layer):
```
❌ WRONG for Bibby:
┌──────────────┐
│  UI Service  │ (serves web UI)
└──────────────┘
┌──────────────┐
│ API Service  │ (exposes APIs)
└──────────────┘
┌──────────────┐
│ Data Service │ (database access)
└──────────────┘
```

**Why it's wrong**: Every feature touches all three services. No independent deployment.

**Domain decomposition** (by business capability):
```
✅ RIGHT for Bibby:
┌──────────────────────────────┐
│   Library Service            │
│   UI + API + Database        │
│   (complete vertical slice)  │
└──────────────────────────────┘
┌──────────────────────────────┐
│   Catalog Service            │
│   UI + API + Database        │
└──────────────────────────────┘
```

Each service is a complete vertical slice. Adding "search by genre" only touches Catalog Service.

## The Strangler Fig Pattern (Incremental Migration)

**Don't rewrite everything at once.** Extract services incrementally.

### Bibby Migration Strategy

**Phase 1: Extract Catalog Service** (6 months)

Why first?
- Read-only dependency on Library (low risk)
- Clear boundary (search vs inventory)
- High value (can optimize search independently)

**Implementation**:

```
Step 1: Build Catalog Service (3 months)
- Create new service with search API
- Set up event listener for Library updates
- Populate initial search index

Step 2: Dual writes (1 month)
- Library writes to both DB and events
- Catalog Service consumes events
- Verify consistency

Step 3: Migrate clients (1 month)
- Point mobile app to Catalog Service for search
- Keep admin UI pointing to Library
- Monitor metrics

Step 4: Sunset old search (1 month)
- Deprecate Library search endpoint
- All clients on Catalog Service
- Remove old search code from Library
```

**Phase 2: Extract Circulation Service** (9 months)

Why second?
- More complex (writes to Library)
- Needs distributed transaction handling
- Requires saga pattern

**Implementation** (simplified):

```
Step 1: Build Circulation Service
- Checkout/checkin API
- Calls Library Service to update book status

Step 2: Move checkout history
- Migrate historical data
- Dual writes during transition

Step 3: Migrate clients
- One client at a time
- Roll back if issues

Step 4: Decommission old code
```

**Never extract**: Shelf and Bookcase
- Too tightly coupled to Book
- No scaling benefit
- Operational overhead > value

## Conway's Law Revisited

From Chapter 1: **Your architecture will mirror your organization.**

**Scenario 1**: Bibby has one team (5 engineers)

**Recommended architecture**: Modular monolith

```
┌─────────────────────────────────┐
│      Bibby Monolith              │
│  ┌────────────────────────────┐ │
│  │ Library Module             │ │
│  └────────────────────────────┘ │
│  ┌────────────────────────────┐ │
│  │ Catalog Module             │ │
│  └────────────────────────────┘ │
│  ┌────────────────────────────┐ │
│  │ Circulation Module         │ │
│  └────────────────────────────┘ │
│                                  │
│  One deployment, clear modules   │
└─────────────────────────────────┘

Team: 5 engineers own everything
```

**Scenario 2**: Bibby has three teams (15 engineers)

**Recommended architecture**: Microservices

```
┌────────────┐  ┌────────────┐  ┌────────────┐
│  Library   │  │  Catalog   │  │Circulation │
│  Service   │  │  Service   │  │  Service   │
└────────────┘  └────────────┘  └────────────┘
     ↓               ↓               ↓
  Team A          Team B          Team C
(5 engineers)   (5 engineers)   (5 engineers)
```

**The Reverse Conway Maneuver**: Reorganize teams to match desired architecture.

**Example**:

Before:
- Frontend Team (4 engineers)
- Backend Team (6 engineers)
- Database Team (3 engineers)

After (reorganized for microservices):
- Library Team (4 engineers: 2 backend, 1 frontend, 1 infra)
- Catalog Team (5 engineers: 3 backend, 1 frontend, 1 QA)
- Circulation Team (4 engineers: 2 backend, 1 frontend, 1 DevOps)

Each team owns a service end-to-end.

## Practical Decomposition Exercise

Let's decompose Bibby's checkout flow:

**Current monolith**:

```java
// One method, one transaction
@Transactional
public void checkOutBook(Long bookId, Long userId) {
    BookEntity book = bookRepository.findById(bookId).orElseThrow();

    if (book.getStatus() == BookStatus.CHECKED_OUT) {
        throw new AlreadyCheckedOutException();
    }

    book.setStatus(BookStatus.CHECKED_OUT);
    book.incrementCheckoutCount();
    bookRepository.save(book);

    Checkout checkout = new Checkout(bookId, userId, LocalDate.now().plusDays(14));
    checkoutRepository.save(checkout);

    // All or nothing (ACID guarantees)
}
```

**After splitting (Circulation + Library services)**:

```java
// Circulation Service
public CheckoutResponse checkOutBook(Long bookId, Long userId) {
    // Step 1: Reserve book in Library Service
    ReserveBookRequest request = new ReserveBookRequest(bookId);

    try {
        libraryServiceClient.reserveBook(request);
    } catch (BookUnavailableException e) {
        throw new CheckoutFailedException("Book not available");
    }

    // Step 2: Create checkout record
    Checkout checkout = new Checkout(bookId, userId, LocalDate.now().plusDays(14));
    checkoutRepository.save(checkout);

    // Step 3: Confirm reservation
    libraryServiceClient.confirmReservation(bookId, checkout.getId());

    return new CheckoutResponse(checkout.getId());
}
```

**Problem**: What if Step 3 fails? Book is reserved but no checkout record!

**Solution**: Saga pattern (covered in Chapter 17). For now, note that distributed transactions are complex.

## Action Items

Before moving to Chapter 9, analyze Bibby's service boundaries:

### 1. Map Dependencies

Create a dependency graph for Bibby:
- Which services/modules call which?
- Are dependencies unidirectional or circular?
- Can you break circular dependencies?

### 2. Identify Bounded Contexts

For each context you identified:
- What's the core responsibility?
- What data changes together?
- Which team would own it?
- What's the deployment independence?

### 3. Apply the Split Test

For each potential service, ask:
- Can it be deployed independently?
- Does it have clear ownership?
- Is it worth the operational overhead?
- Would 4-6 engineers have enough work?

### 4. Design a Migration Plan

If you were to extract one service from Bibby:
- Which would you extract first? Why?
- What's the 6-month plan?
- How would you handle the cutover?
- What's the rollback strategy?

### 5. Calculate Coupling Score

For each dependency:
- Synchronous or asynchronous?
- Shared database or separate?
- Can services deploy independently?
- What's the blast radius if one fails?

## Key Takeaways

1. **Don't split by entities** — Split by business capabilities and bounded contexts

2. **DDD bounded contexts = service boundaries** — Library Management vs Catalog vs Circulation

3. **Start with modules, extract services later** — Modularity doesn't require microservices

4. **One team per service** — If a service can't keep 4-6 engineers busy, it's too small

5. **Avoid distributed monoliths** — Shared databases, synchronous chains, deployment coupling

6. **Functional decomposition is wrong** — Domain decomposition (vertical slices) is right

7. **Use Strangler Fig pattern** — Incremental extraction, not big-bang rewrite

8. **Conway's Law applies** — Organize teams to match desired architecture

9. **Service granularity is about independence** — Not about size

10. **Coupling is the enemy** — Minimize synchronous dependencies, maximize autonomy

## Further Reading

### Essential Papers
- **"Identifying Service Boundaries"** by Martin Fowler
- **"Conway's Law"** — original paper (1968)
- **"Strangler Fig Application"** by Martin Fowler

### Books
- **"Building Microservices"** by Sam Newman — Chapters on decomposition
- **"Domain-Driven Design Distilled"** by Vaughn Vernon — Bounded contexts
- **"Monolith to Microservices"** by Sam Newman — Migration patterns

### Real-World Case Studies
- **Uber's Microservices Journey** — How they split the monolith
- **Amazon's Two-Pizza Teams** — Team topology and service ownership
- **Shopify's Modular Monolith** — When NOT to split

---

## What's Next?

In **Chapter 9: Data Ownership & Service Databases**, we'll tackle the hardest part of microservices:
- Database per service pattern
- Data duplication vs sharing (when to duplicate)
- Distributed transactions and why they fail
- Sagas pattern (orchestration vs choreography)
- Event sourcing fundamentals
- CQRS (Command Query Responsibility Segregation)

**Remember**: You've learned where to draw service boundaries. Next, you'll learn how to handle data across those boundaries.

Service boundaries are logical. Data ownership is where it gets hard. Let's tackle it next.
