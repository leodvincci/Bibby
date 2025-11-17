# SECTION 6: DOMAIN EVENTS

## Making the Implicit Explicit

Welcome to Section 6. We're now going to explore one of the most powerful patterns in DDD: **Domain Events**.

Remember in Section 3 when we discussed the rule "one transaction = one aggregate"? Domain events are the solution for when you need to coordinate multiple aggregates without violating that rule.

But domain events are much more than a technical trick - they're a way of making implicit domain concepts explicit in your code.

---

## What is a Domain Event?

A **Domain Event** is a record of something significant that happened in the domain. It represents a fact about the past that domain experts care about.

### The Past Tense Principle

Domain events are always named in the **past tense** because they represent things that have already happened:

✓ Good:
- `BookCheckedOut`
- `AuthorAdded`
- `BookMovedToShelf`
- `CheckoutLimitExceeded`

❌ Bad:
- `CheckOutBook` (command, not event)
- `BookCheckout` (not past tense)
- `MoveBook` (action, not fact)

### The "When X Happens" Pattern

A domain event answers the question: **"When X happens, what else needs to happen?"**

Examples:
- **When a book is checked out**, update the checkout count
- **When a book is added to the collection**, send a notification
- **When a shelf becomes full**, alert the user
- **When a book is moved**, update shelf statistics

These are all domain concepts that deserve to be explicit in your model.

---

## Why Domain Events Matter

### Reason 1: Decoupling Aggregates

**Problem**: You want to update multiple aggregates, but "one transaction = one aggregate" is the rule.

**Without events** (violates DDD rules):
```java
@Transactional
public void moveBook(BookId bookId, ShelfId newShelfId) {
    Book book = bookRepository.findById(bookId);
    Shelf oldShelf = shelfRepository.findById(book.getShelfId());
    Shelf newShelf = shelfRepository.findById(newShelfId);

    // Violating aggregate boundaries!
    oldShelf.removeBook(book);
    newShelf.addBook(book);
    book.setShelf(newShelf);

    // Three aggregates in one transaction - wrong!
    bookRepository.save(book);
    shelfRepository.save(oldShelf);
    shelfRepository.save(newShelf);
}
```

**With events** (proper DDD):
```java
@Transactional
public void moveBook(BookId bookId, ShelfId newShelfId) {
    Book book = bookRepository.findById(bookId);
    ShelfId oldShelfId = book.getCurrentShelfId();

    // Update only ONE aggregate
    book.placeOnShelf(newShelfId);
    bookRepository.save(book);

    // Publish event - other aggregates update eventually
    eventPublisher.publish(new BookMovedToShelfEvent(
        bookId, oldShelfId, newShelfId, LocalDateTime.now()
    ));
}

// Event handler updates shelves asynchronously
@EventListener
public void handleBookMoved(BookMovedToShelfEvent event) {
    updateShelfStatistics(event.getOldShelfId());
    updateShelfStatistics(event.getNewShelfId());
}
```

### Reason 2: Making Domain Concepts Explicit

**Question**: Is "book checked out" significant in your domain?

**Answer**: YES! It's a key concept. But in code without events, it's just a setter call:

```java
book.setStatus(BookStatus.CHECKED_OUT);  // Implicit, easy to miss
```

With events:
```java
book.checkOut();  // Publishes BookCheckedOutEvent
```

Now "book checked out" is an explicit, first-class concept in your domain.

### Reason 3: Audit Trail and History

Domain events create a natural audit log:

```java
@EventListener
public void logDomainEvents(DomainEvent event) {
    auditLog.record(event);
}
```

Now you have a complete history of everything that happened:
- 10:23 AM: BookAddedToCollection
- 10:25 AM: BookPlacedOnShelf
- 2:15 PM: BookCheckedOut
- 8:30 PM: BookReturned

### Reason 4: Enabling Future Features

Today: When a book is checked out, just update the status.

Tomorrow: When a book is checked out, also:
- Send email notification
- Update reading statistics
- Recommend similar books
- Update "popular books" list

With events, you just add new event listeners. Without events, you have to modify existing code.

---

## Domain Event Characteristics

A proper domain event has these properties:

### 1. Immutable

Once created, an event never changes. It's a historical fact.

```java
public final class BookCheckedOutEvent {  // final = immutable
    private final BookId bookId;
    private final LocalDateTime occurredAt;

    public BookCheckedOutEvent(BookId bookId, LocalDateTime occurredAt) {
        this.bookId = bookId;
        this.occurredAt = occurredAt;
    }

    public BookId getBookId() { return bookId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    // No setters!
}
```

### 2. Contains All Necessary Information

Event consumers shouldn't need to query for additional data.

❌ Bad (missing information):
```java
public class BookCheckedOutEvent {
    private BookId bookId;
    // Event handler needs to load the book to get more info
}
```

✓ Good (complete information):
```java
public class BookCheckedOutEvent {
    private final BookId bookId;
    private final ISBN isbn;
    private final Title title;
    private final MemberId checkedOutBy;
    private final LocalDateTime occurredAt;
    private final LocalDateTime dueDate;
    // Everything a handler might need
}
```

### 3. Named in Ubiquitous Language

Event names should come from domain conversations:

✓ Domain language:
- `BookCheckedOut`
- `AuthorAddedToBook`
- `BookReserved`

❌ Technical language:
- `BookStatusChangedEvent`
- `DatabaseUpdateOccurred`

### 4. Past Tense

Events are facts about what already happened:

✓ Past tense:
- `BookCheckedOut` (it happened)
- `ShelfCreated`
- `BookcaseRelocated`

❌ Present/future:
- `CheckOutBook` (sounds like command)
- `BookWillBeCheckedOut`

---

## Implementing Domain Events in Spring Boot

Spring provides excellent support for domain events.

### Pattern 1: Simple Event with ApplicationEventPublisher

```java
// domain/event/BookCheckedOutEvent.java
public class BookCheckedOutEvent {
    private final BookId bookId;
    private final LocalDateTime occurredAt;
    private final int newCheckoutCount;

    public BookCheckedOutEvent(BookId bookId, LocalDateTime occurredAt, int newCheckoutCount) {
        this.bookId = bookId;
        this.occurredAt = occurredAt;
        this.newCheckoutCount = newCheckoutCount;
    }

    // Getters...
}

// domain/model/Book.java
public class Book {
    // Entity fields...

    public void checkOut() {
        if (!status.canCheckOut()) {
            throw new BookNotAvailableException();
        }

        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;
        this.updatedAt = LocalDateTime.now();

        // Publish event
        registerEvent(new BookCheckedOutEvent(
            this.id,
            LocalDateTime.now(),
            this.checkoutCount
        ));
    }

    // Event registration (to be published after save)
    private final List<Object> domainEvents = new ArrayList<>();

    void registerEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> getDomainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}

// application/service/CheckOutBookService.java
@Service
public class CheckOutBookService {
    private final BookRepository bookRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void checkOutBook(BookId bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.checkOut();  // Registers event internally

        bookRepository.save(book);

        // Publish all events after save
        book.getDomainEvents().forEach(eventPublisher::publishEvent);
        book.clearDomainEvents();
    }
}

// Event handlers
@Component
public class BookEventHandlers {

    @EventListener
    public void handleBookCheckedOut(BookCheckedOutEvent event) {
        System.out.println("Book checked out: " + event.getBookId());
        // Update statistics, send notifications, etc.
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookCheckedOutAfterCommit(BookCheckedOutEvent event) {
        // This runs only if transaction commits successfully
        emailService.sendCheckoutConfirmation(event.getBookId());
    }
}
```

### Pattern 2: Spring Data's @DomainEvents

Spring Data provides automatic event publishing:

```java
// domain/model/Book.java
public class Book {
    @Transient  // JPA - don't persist this field
    private final List<Object> domainEvents = new ArrayList<>();

    public void checkOut() {
        // ... checkout logic ...

        // Register event
        registerEvent(new BookCheckedOutEvent(this.id, LocalDateTime.now()));
    }

    @DomainEvents  // Spring Data will publish these automatically
    public Collection<Object> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @AfterDomainEventPublication  // Called after publishing
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    private void registerEvent(Object event) {
        this.domainEvents.add(event);
    }
}

// infrastructure/persistence/jpa/JpaBookRepository.java
// Spring Data automatically publishes events when save() is called!
```

---

## Domain Events in Bibby

Let's identify domain events in your library system.

### Identified Events

**Book Lifecycle Events**:
- `BookAddedToCollection` - new book acquired
- `BookRemovedFromCollection` - book disposed of
- `BookDetailsUpdated` - title, description, etc. changed

**Book Availability Events**:
- `BookCheckedOut` - book borrowed
- `BookReturned` - book brought back
- `BookReserved` - book reserved for later
- `BookReportedLost` - book can't be found
- `BookArchived` - book removed from circulation

**Book Organization Events**:
- `BookPlacedOnShelf` - book assigned to shelf
- `BookMovedToShelf` - book relocated
- `BookRemovedFromShelf` - book taken off shelf

**Shelf Events**:
- `ShelfCreated` - new shelf added to bookcase
- `ShelfRemoved` - shelf removed
- `ShelfReorganized` - shelf position changed
- `ShelfBecameFull` - shelf reached capacity

**Author Events**:
- `AuthorCreated` - new author in system
- `AuthorAddedToBook` - author attributed to book
- `AuthorRemovedFromBook` - authorship corrected

### Example: Complete Event Implementation

```java
// domain/event/BookCheckedOutEvent.java
public final class BookCheckedOutEvent {
    private final BookId bookId;
    private final ISBN isbn;
    private final Title title;
    private final LocalDateTime checkedOutAt;
    private final int checkoutCount;

    public BookCheckedOutEvent(
        BookId bookId,
        ISBN isbn,
        Title title,
        LocalDateTime checkedOutAt,
        int checkoutCount
    ) {
        this.bookId = Objects.requireNonNull(bookId);
        this.isbn = Objects.requireNonNull(isbn);
        this.title = Objects.requireNonNull(title);
        this.checkedOutAt = Objects.requireNonNull(checkedOutAt);
        this.checkoutCount = checkoutCount;
    }

    // Getters
    public BookId getBookId() { return bookId; }
    public ISBN getIsbn() { return isbn; }
    public Title getTitle() { return title; }
    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public int getCheckoutCount() { return checkoutCount; }

    @Override
    public String toString() {
        return String.format("BookCheckedOut[book=%s, title=%s, at=%s, count=%d]",
            bookId, title, checkedOutAt, checkoutCount);
    }
}

// domain/model/Book.java
public class Book {
    public void checkOut() {
        if (!status.canCheckOut()) {
            throw new BookNotAvailableException();
        }

        ShelfId previousShelfId = this.currentShelfId;

        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;
        this.updatedAt = LocalDateTime.now();

        // Publish event with full information
        registerEvent(new BookCheckedOutEvent(
            this.id,
            this.isbn,
            this.title,
            LocalDateTime.now(),
            this.checkoutCount
        ));
    }

    public void returnToLibrary() {
        if (status == AvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException("Book already available");
        }

        this.status = AvailabilityStatus.AVAILABLE;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new BookReturnedEvent(this.id, LocalDateTime.now()));
    }

    public void placeOnShelf(ShelfId shelfId) {
        ShelfId oldShelfId = this.currentShelfId;
        this.currentShelfId = Objects.requireNonNull(shelfId);
        this.updatedAt = LocalDateTime.now();

        if (oldShelfId != null) {
            registerEvent(new BookMovedToShelfEvent(
                this.id, oldShelfId, shelfId, LocalDateTime.now()
            ));
        } else {
            registerEvent(new BookPlacedOnShelfEvent(
                this.id, shelfId, LocalDateTime.now()
            ));
        }
    }
}

// Event handlers
@Component
public class LibraryStatisticsUpdater {

    private final ShelfStatisticsService statisticsService;

    @EventListener
    @Async  // Run asynchronously
    public void handleBookMovedToShelf(BookMovedToShelfEvent event) {
        // Update statistics for old shelf
        statisticsService.updateBookCount(event.getOldShelfId());

        // Update statistics for new shelf
        statisticsService.updateBookCount(event.getNewShelfId());
    }

    @EventListener
    public void handleBookCheckedOut(BookCheckedOutEvent event) {
        // Update library statistics
        statisticsService.recordCheckout(event.getBookId());
    }
}

@Component
public class BookEventLogger {

    @EventListener
    public void logAllBookEvents(Object event) {
        if (event.getClass().getPackageName().contains("domain.event")) {
            log.info("Domain event occurred: {}", event);
        }
    }
}
```

---

## Event-Driven Architecture Patterns

### Pattern 1: Synchronous Event Handling

Events handled in the same transaction:

```java
@EventListener
public void handleBookCheckedOut(BookCheckedOutEvent event) {
    // Runs synchronously in same transaction
    updateStatistics(event);
}
```

**Use when**: Event handling must succeed or fail with the transaction.

### Pattern 2: Asynchronous Event Handling

Events handled in separate thread:

```java
@EventListener
@Async
public void handleBookCheckedOut(BookCheckedOutEvent event) {
    // Runs asynchronously in different thread
    sendEmailNotification(event);
}
```

**Use when**: Event handling is not critical to transaction success.

### Pattern 3: Transactional Event Handling

Events handled after transaction commits:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleBookCheckedOut(BookCheckedOutEvent event) {
    // Only runs if transaction commits successfully
    notifyExternalSystem(event);
}
```

**Use when**: Event handling should only occur if the transaction succeeds.

### Pattern 4: Event Sourcing (Advanced)

Store events as the source of truth:

```java
// Every state change is an event
book.checkOut();  // → BookCheckedOutEvent stored
book.returnToLibrary();  // → BookReturnedEvent stored

// Reconstruct state by replaying events
Book book = new Book();
events.forEach(event -> book.apply(event));
```

**Note**: Event sourcing is advanced. Focus on simple event publishing first.

---

## Action Items

### 1. Identify Domain Events (2-3 hours)

Create `domain-events.md`:
- List 10+ domain events in Bibby
- For each, explain: When does it happen? Who cares?
- Categorize: Book events, Shelf events, Author events

### 2. Implement Your First Event (3-4 hours)

Choose one event and implement fully:
- Create event class (immutable, past tense)
- Modify entity to publish event
- Create event handler
- Test the flow

Recommendation: Start with `BookCheckedOutEvent`

### 3. Add Event-Based Statistics (2-3 hours)

Instead of updating shelf counts directly:
- Publish `BookMovedToShelfEvent`
- Handle event to update counts
- Verify eventual consistency works

### 4. Create Event Logger (1 hour)

Build a generic event logger:
- Logs all domain events to console/file
- Creates audit trail
- Helps debug event flow

### 5. Refactor Multi-Aggregate Operations (3-4 hours)

Find places where you modify multiple aggregates:
- Refactor to modify one aggregate + publish event
- Let event handlers update other aggregates
- Ensure proper transaction boundaries

---

## Key Takeaways

### 1. Events Represent Facts About the Past
- Named in past tense
- Immutable
- Contain complete information

### 2. Events Enable Decoupling
- Modify one aggregate per transaction
- Other aggregates updated via events
- Loose coupling, high cohesion

### 3. Events Make Domain Concepts Explicit
- "Book checked out" is now a first-class concept
- Easy to find where book checkouts are handled
- Self-documenting code

### 4. Events Enable Future Extension
- Add new behavior by adding event listeners
- Don't modify existing code
- Open/Closed Principle

### 5. Spring Provides Excellent Event Support
- `ApplicationEventPublisher`
- `@EventListener`
- `@TransactionalEventListener`
- `@DomainEvents` with Spring Data

---

## Further Study

**Books**:
- "Domain-Driven Design" by Eric Evans - Chapter 8 (Supple Design includes events)
- "Implementing Domain-Driven Design" by Vaughn Vernon - Chapter 8 (Domain Events)
- "Versioning in an Event Sourced System" by Greg Young

**Articles**:
- Martin Fowler: "Event Sourcing"
- Udi Dahan: "Domain Events - Salvation"
- Microsoft: "Domain Events: Design and Implementation"

**Patterns**:
- Event Sourcing
- CQRS (Command Query Responsibility Segregation)
- Saga pattern for distributed transactions

---

## Mentor's Note

Domain events might seem like overkill at first. "Why not just call the method directly?" you might think.

Here's what I've learned over 15 years: domain events are one of those patterns that seem like extra work initially, but pay massive dividends later.

Every project I've worked on that embraced domain events became easier to maintain and extend. Every project that avoided them ended up with tangled, brittle code.

Start simple:
1. Publish one event (`BookCheckedOutEvent`)
2. Handle it in one place (logging)
3. Gradually add more handlers
4. Watch your system become more flexible

The beauty of events is that they grow with your system. Today, you just log checkouts. Tomorrow, you send emails. Next week, you update statistics. Next month, you integrate with a recommendation engine.

All without modifying existing code.

That's the power of events.

In Section 7, we'll learn about Layered Architecture and Hexagonal Architecture - how to structure your entire application to support these DDD patterns.

You've made it through the core tactical patterns! Section 7 brings it all together architecturally.

Excellent progress!

---

**Section 6 Complete** | Next: Section 7 - Layered Architecture & Hexagonal Architecture
