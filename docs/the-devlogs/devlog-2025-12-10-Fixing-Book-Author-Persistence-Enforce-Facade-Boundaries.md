# Devlog: Fixing Book-Author Persistence and Enforcing Facade Boundaries

**Date:** December 10, 2025  
**Diff Context:** Book registration workflow, hexagonal architecture enforcement, JPA relationship persistence fix

---

## 1. High-Level Summary

- **Fixed a critical JPA persistence ordering issue** where book-author relationships weren't being saved correctly because authors weren't persisted before association with books
- **Enforced hexagonal architecture boundaries** by routing BookController through BookFacade instead of calling BookService directly, and removing BookFacade implementation from BookService
- **Expanded repository interfaces** with new domain operations (updateTheBooksShelf, getBookById, findBookByIsbn, saveAuthor) to support the refactored workflow
- **Removed unused dependencies** (PromptFacade from BookCommands) and cleaned up constructor injection
- **Added comprehensive logging infrastructure** to trace the persistence flow during debugging
- **Restructured the book registration workflow** to save authors immediately during prompting rather than batch-saving at the end

---

## 2. The Underlying Problem or Friction

This diff reveals a classic JPA/Hibernate relationship persistence issue that manifests in modular monoliths with complex entity associations.

**The Core Bug:** When creating a new book with authors, the system was attempting to associate AuthorEntity references with a BookEntity before those authors were actually persisted. JPA requires that related entities either (a) already exist in the database with valid IDs, or (b) be cascade-persisted in the same transaction. The existing code was doing neither correctly.

**Evidence in the diff:**

```java
// BEFORE: Authors collected but not persisted
for (int i = 0; i < authorCount; i++) {
    authors.add(cliPrompt.promptForAuthor());
}
// Later tried to associate unpersisted authors with book

// AFTER: Authors saved immediately, returning persisted DTOs with IDs
for (int i = 0; i < authorCount; i++) {
    authors.add(authorFacade.saveAuthor(cliPrompt.promptForAuthor()));
    log.info("Author added: {}", authors.get(i));
}
```

**Additional friction points addressed:**

- BookService was simultaneously acting as a facade implementation AND an application service—violating single responsibility
- The controller had direct access to the service layer, bypassing the contract/facade boundary
- Method naming was unclear (`setShelfForBook` vs the more intention-revealing `updateTheBooksShelf`)
- The `registerBook` method was setting authors BEFORE the initial save, causing the transient entity association problem

---

## 3. The Behavior Change

From the **user/system perspective**, these changes mean:

**Before:**
- Book registration would fail silently or throw exceptions when author associations couldn't be resolved
- Authors might not appear associated with books after creation
- The persistence ordering was unpredictable

**After:**
- Authors are persisted immediately upon entry, guaranteeing valid IDs
- The book entity is saved first (getting its own ID), then author associations are added in a second save
- The workflow is explicit: prompt → save author → collect persisted DTO → create book → associate authors

**The two-phase save pattern in BookDomainRepositoryImpl:**

```java
@Override
public void registerBook(Book book) {
    log.info("Mapping book domain to entity for book: " + book.getTitle().title());
    BookEntity bookEntity = new BookEntity();
    bookEntity.setIsbn(book.getIsbn().isbn);
    bookEntity.setTitle(book.getTitle().title());
    // ... other fields ...
    bookEntity.setCreatedAt(LocalDate.now());
    bookEntity.setUpdatedAt(LocalDate.now());
    
    // PHASE 1: Save the book first to get a valid ID
    log.info("Registering new book: " + book.getTitle().title());
    bookJpaRepository.save(bookEntity);
    log.info("Book registered with id: " + bookEntity.getBookId());

    // PHASE 2: Now associate authors (they already have IDs from earlier save)
    Set<AuthorEntity> authorEntities = bookMapper.toEntitySetFromAuthorRefs(book.getAuthors());
    log.info(authorEntities.toString());
    bookEntity.setAuthors(authorEntities);
    bookJpaRepository.save(bookEntity);
}
```

**Contract changes:**
- `setShelfForBook(Long id, Long shelfId)` → `updateTheBooksShelf(BookDTO bookDTO, Long newShelfId)` (richer domain object instead of primitive ID)
- BookRequestDTO parameter order changed: `(title, authors, isbn)` → `(title, isbn, authors)` (ISBN now comes before authors)
- CLI command renamed: `register` → `new` (more intuitive UX)

---

## 4. The Architectural Meaning

This diff represents a significant step toward **proper hexagonal architecture alignment**:

**1. Facade Boundary Enforcement**

```java
// BEFORE: Controller called service directly
public BookController(BookService bookService, ...) {
    this.bookService = bookService;
}

@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookService.createNewBook(requestDTO);  // Direct service call
}

// AFTER: Controller routes through facade
public BookController(BookService bookService, BookFacade bookFacade, ...) {
    this.bookFacade = bookFacade;
}

@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookFacade.createNewBook(requestDTO);  // Through the adapter
}
```

**2. Service Decoupling**

```java
// BEFORE: BookService implemented BookFacade
public class BookService implements BookFacade { ... }

// AFTER: BookService is pure application logic
public class BookService { ... }  // No longer implements facade
```

This separation means:
- **BookFacade** defines the contract (the port)
- **BookFacadeImpl** (not shown but implied) handles coordination and DTO mapping
- **BookService** focuses on business logic without facade concerns

**3. Repository Interface Expansion**

The `BookDomainRepository` gained several operations that push JPA concerns further down:

```java
// New methods added
BookEntity getBookById(Long id);
void updateTheBooksShelf(Book book, Long bookId, Long newShelfId);
void updateAvailabilityStatus(String bookTitle);
void registerBookFromMetaData(BookMetaDataResponse response, String isbn, Long shelfId);
BookEntity findBookByIsbn(String isbn);
BookDetailView getBookDetailView(Long bookId);
```

This expansion allows the application layer to work purely with domain objects while infrastructure handles the JPA translation.

**4. Improved Testability**

By routing through facades and expanding repository interfaces:
- BookService can be unit tested without JPA dependencies
- The CLI can be tested by mocking the facade layer
- Repository implementations can be swapped (e.g., in-memory for tests)

---

## 5. The Developer's Thought Process (Reconstructed)

This diff tells a story of debugging and iterative improvement:

**Phase 1: "Why aren't my authors being saved?"**

The extensive logging additions (`log.info` throughout AuthorRepositoryImpl, BookDomainRepositoryImpl, BookMapper) reveal the developer was tracing a persistence failure. The pattern suggests:

> "I need to understand exactly when and how entities are being saved. Let me add logging at every persistence boundary."

**Phase 2: "The authors don't have IDs when I try to associate them"**

The critical change from batch collection to immediate save:

```java
// The "aha" moment
authors.add(authorFacade.saveAuthor(cliPrompt.promptForAuthor()));
```

> "Authors need to exist in the database BEFORE I can associate them with a book. I need to save each author immediately after the user enters them, not at the end."

**Phase 3: "The service shouldn't be the facade"**

The removal of `implements BookFacade` from BookService shows architectural clarity emerging:

> "BookService is doing too much. It's trying to be both the facade and the business logic. The controller should call a separate facade implementation that coordinates between multiple services."

**Phase 4: "The registerBook method needs two saves"**

The two-phase save in BookDomainRepositoryImpl:

> "JPA can't associate transient entities. I need to save the book first to get an ID, then add the author associations and save again."

**Phase 5: "Clean up the debris"**

Removal of PromptFacade, commented-out code, and method renames show the developer tidying after the fix:

> "Now that it works, let me remove the unused dependency and give methods clearer names."

---

## 6. Narrative of the Changes

The developer encountered a subtle but frustrating bug in Bibby's book registration workflow. When users entered a new book with authors through the CLI, the associations weren't being persisted correctly. Sometimes authors would be saved without being linked to books; sometimes the entire operation would fail.

The root cause was a classic JPA relationship timing issue. The original code collected author information from the user, built a list of AuthorDTO objects, then tried to create a Book domain object with AuthorRef associations. But those AuthorRef objects didn't have valid database IDs—the authors hadn't been persisted yet. When the system tried to save the BookEntity with these "phantom" author associations, JPA couldn't resolve the references.

The fix required restructuring the entire registration flow. Instead of batch-collecting authors and saving them at the end, the new approach saves each author immediately after the user enters their information. The `authorFacade.saveAuthor()` call returns a persisted DTO complete with the database-generated ID. These IDs are then used when constructing the Book's author references, ensuring JPA can properly establish the many-to-many relationship.

The persistence layer itself also needed adjustment. The `registerBook` method now performs a two-phase save: first saving the BookEntity to establish its ID, then adding the author associations and saving again. This pattern is necessary because JPA cannot persist a many-to-many relationship when both sides are transient.

Beyond the bug fix, this diff reveals an ongoing architectural cleanup. The developer recognized that `BookService` was violating single responsibility by implementing `BookFacade`. The service should focus on business logic while a separate facade implementation handles coordination and DTO mapping. The `BookController` was updated to inject `BookFacade` directly, properly routing through the hexagonal architecture's inbound port rather than bypassing it.

Several smaller improvements accompany the main fix: removing the unused `PromptFacade` dependency, renaming methods to be more intention-revealing (`updateTheBooksShelf` instead of `setShelfForBook`), expanding repository interfaces with domain-appropriate operations, and adding comprehensive logging to support future debugging.

---

## 7. Key Technical Highlights

- **Fixed author persistence ordering** by calling `authorFacade.saveAuthor()` immediately during the prompting loop, ensuring authors have valid IDs before book association
- **Implemented two-phase book registration** in `BookDomainRepositoryImpl.registerBook()`: save book first to get ID, then associate authors and save again
- **Decoupled BookService from BookFacade** by removing the `implements BookFacade` clause, separating application logic from port contract
- **Routed BookController through BookFacade** instead of calling BookService directly, enforcing hexagonal boundaries
- **Expanded BookDomainRepository interface** with `updateTheBooksShelf()`, `getBookById()`, `findBookByIsbn()`, and `getBookDetailView()` to push JPA operations into infrastructure
- **Added AuthorRepository.saveAuthor()** and `saveAll()` methods to support the new immediate-persistence workflow
- **Added new mapper methods** (`toDomainFromBookRequestDTO`, `toDomainFromDTO`) to support richer DTO-to-domain conversions
- **Removed unused PromptFacade dependency** from BookCommands, cleaning up constructor injection
- **Changed BookRequestDTO parameter order** to `(title, isbn, authors)` for consistency with domain model
- **Renamed CLI command** from `register` to `new` for improved user experience
- **Enabled INFO-level logging** across Spring, Hibernate, and application layers to support debugging

---

## 8. Talking Points (Interview, Portfolio, Devlog)

- **Diagnosed and resolved a JPA many-to-many persistence issue** by restructuring the book registration workflow to persist authors before association, implementing a two-phase save pattern that respects JPA's transient entity constraints

- **Strengthened hexagonal architecture boundaries** by decoupling the application service from its facade contract, ensuring controllers route through inbound ports rather than bypassing architectural layers

- **Improved domain model integrity** by expanding repository interfaces with domain-appropriate operations, pushing JPA concerns into infrastructure adapters while keeping application logic persistence-agnostic

- **Enhanced system debuggability** by implementing comprehensive logging at persistence boundaries, enabling rapid diagnosis of entity lifecycle issues in complex domain operations

- **Reduced coupling between modules** by removing unused cross-module dependencies and clarifying method contracts with intention-revealing names that encode domain semantics

- **Applied incremental refactoring discipline** to a critical code path, fixing the immediate bug while simultaneously improving architectural alignment and code clarity

---

## 9. Potential Interview Questions (Based on This Diff)

### Design Rationale & Tradeoffs

1. **Why did you choose to save authors immediately during the prompting loop rather than batch-saving at the end?**
   - *Expected discussion: JPA transient entity associations, ID generation timing, user experience implications of early failures*

2. **What tradeoffs come with the two-phase save approach in `registerBook()`? Are there alternatives?**
   - *Expected discussion: cascade options, `@PrePersist` hooks, orphan removal, transaction boundaries*

3. **Why remove `implements BookFacade` from BookService instead of keeping it for convenience?**
   - *Expected discussion: single responsibility, testability, separation of concerns, facade vs service roles*

### Domain-Driven Design Understanding

4. **The `AuthorRef` value object stores an ID alongside the AuthorName. Why include the ID in a value object?**
   - *Expected discussion: identity vs equality, reference semantics, the "foreign key in domain" pattern*

5. **Why does `updateTheBooksShelf()` take a full BookDTO instead of just the book ID?**
   - *Expected discussion: rich domain objects vs primitive obsession, domain logic requiring full context*

6. **How would you handle the scenario where a user enters duplicate author names? Where should that validation live?**
   - *Expected discussion: domain invariants, value object equality, repository vs service responsibility*

### Architecture Awareness

7. **The controller now injects both BookService and BookFacade. Is this a code smell? What's the path forward?**
   - *Expected discussion: transitional state, removing direct service dependency, facade as sole entry point*

8. **How does routing through the facade improve testability compared to direct service calls?**
   - *Expected discussion: mocking at boundaries, contract stability, isolation of integration concerns*

9. **The BookMapper now has a dependency on AuthorFacade. Does this violate any architectural principles?**
   - *Expected discussion: mapper responsibilities, infrastructure layer dependencies, alternative approaches*

### Persistence & Transactional Behavior

10. **Why doesn't the `registerBook()` method have `@Transactional` annotation? What are the implications?**
    - *Expected discussion: transaction boundaries, rollback behavior, consistency guarantees*

11. **If the second save in `registerBook()` fails after the first succeeds, what state is the system in?**
    - *Expected discussion: partial failures, eventual consistency, compensating transactions*

12. **The mapper's `toEntitySetFromAuthorRefs()` only sets the author ID, not firstName/lastName. How does JPA resolve the full entity?**
    - *Expected discussion: JPA proxies, lazy loading, session context, `getReference()` vs `find()`*

### Code Quality & Maintainability

13. **There's a lot of commented-out code in this diff. What's your approach to handling code during a refactor?**
    - *Expected discussion: incremental changes, feature flags, git history vs code comments*

14. **The logging statements use string concatenation (`"text: " + value`). What's the issue and how would you fix it?**
    - *Expected discussion: SLF4J parameterized logging, performance, lazy evaluation*

15. **How would you test the book registration workflow now that it's been restructured?**
    - *Expected discussion: unit vs integration tests, mocking facades, test database setup*

---

## 10. Areas Worth Diving Deeper Into

### JPA/Hibernate Relationship Management

**Why this matters:** This diff's core bug stems from misunderstanding how JPA handles transient entity associations in many-to-many relationships. A deeper understanding would prevent similar issues and inform better entity design.

**Resources:**
- *Java Persistence with Hibernate* — Bauer & King (chapters on association mapping and cascade types)
- Vlad Mihalcea's blog series on JPA relationships: [vladmihalcea.com](https://vladmihalcea.com)

### Hexagonal Architecture & Ports/Adapters

**Why this matters:** The separation of BookService from BookFacade represents a move toward cleaner hexagonal boundaries. Understanding the full pattern would help complete this refactor and apply it consistently across modules.

**Resources:**
- "Hexagonal Architecture" — Alistair Cockburn (original article)
- *Get Your Hands Dirty on Clean Architecture* — Tom Hombergs (Java-specific implementation guide)

### Transaction Management in Spring

**Why this matters:** The two-phase save pattern works but lacks explicit transaction demarcation. Understanding Spring's transaction propagation and isolation levels would help ensure data consistency and optimize performance.

**Resources:**
- Spring Framework Documentation: Transaction Management
- *Spring in Action* — Craig Walls (transaction chapter)

### Domain-Driven Design: Aggregates and Consistency Boundaries

**Why this matters:** Book and Author have a many-to-many relationship that spans aggregate boundaries. Understanding DDD aggregate patterns would inform whether these should be separate aggregates or a single consistency boundary.

**Resources:**
- *Implementing Domain-Driven Design* — Vaughn Vernon (aggregates chapter)
- *Domain-Driven Design Distilled* — Vaughn Vernon (for a condensed overview)

### Testing Strategies for Persistence Logic

**Why this matters:** The refactored code is more testable but requires different testing approaches. Understanding when to use repository mocks vs. test containers vs. H2 would help build a robust test suite.

**Resources:**
- *Growing Object-Oriented Software, Guided by Tests* — Freeman & Pryce
- Testcontainers documentation for PostgreSQL integration testing

---

## Code Examples: Before and After

### Author Persistence Flow

**Before (Broken):**
```java
// BookCommands.java
for (int i = 0; i < authorCount; i++) {
    authors.add(cliPrompt.promptForAuthor());  // DTO without database ID
}
BookRequestDTO bookRequestDTO = new BookRequestDTO(title, authors, isbn);
bookFacade.createNewBook(bookRequestDTO);  // Authors not persisted yet!
```

**After (Fixed):**
```java
// BookCommands.java
for (int i = 0; i < authorCount; i++) {
    authors.add(authorFacade.saveAuthor(cliPrompt.promptForAuthor()));  // Persisted!
    log.info("Author added: {}", authors.get(i));  // Has valid ID now
}
BookRequestDTO bookRequestDTO = new BookRequestDTO(title, isbn, authors);
bookFacade.createNewBook(bookRequestDTO);  // Authors already exist in DB
```

### Two-Phase Book Save

```java
// BookDomainRepositoryImpl.java
@Override
public void registerBook(Book book) {
    BookEntity bookEntity = new BookEntity();
    // ... set scalar fields ...
    bookEntity.setCreatedAt(LocalDate.now());
    bookEntity.setUpdatedAt(LocalDate.now());
    
    // Phase 1: Save book to get ID
    bookJpaRepository.save(bookEntity);
    log.info("Book registered with id: " + bookEntity.getBookId());

    // Phase 2: Associate authors (they have IDs now)
    Set<AuthorEntity> authorEntities = bookMapper.toEntitySetFromAuthorRefs(book.getAuthors());
    bookEntity.setAuthors(authorEntities);
    bookJpaRepository.save(bookEntity);  // Update with associations
}
```

### Controller Routing Through Facade

**Before:**
```java
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookService.createNewBook(requestDTO);  // Bypasses adapter
    return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
}
```

**After:**
```java
@PostMapping("api/v1/books")
public ResponseEntity<String> addBook(@RequestBody BookRequestDTO requestDTO) {
    bookFacade.createNewBook(requestDTO);  // Through inbound adapter
    return ResponseEntity.ok("Book Added Successfully: " + requestDTO.title());
}
```

---

*This diff represents work-in-progress refactoring. The commented-out code and extensive logging indicate active debugging and iterative improvement. Future commits should remove the debugging artifacts and complete the facade extraction.*
