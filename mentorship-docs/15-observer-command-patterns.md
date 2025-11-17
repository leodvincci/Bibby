# SECTION 15: OBSERVER & COMMAND PATTERNS

## Behavioral Patterns for Events and Requests

Welcome to Section 15! We're continuing our exploration of behavioral patterns. In this section, we'll cover two powerful patterns: **Observer** and **Command**.

**Observer** lets objects subscribe to and receive notifications about events - you've already been using this with Domain Events in Section 6!

**Command** encapsulates requests as objects, enabling features like undo/redo, queuing, and logging. It's the foundation of CQRS (Command Query Responsibility Segregation).

Both patterns are essential for building decoupled, maintainable systems.

---

## The Observer Pattern

### What is the Observer Pattern?

**Definition**: Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically.

**Key Insight**: Instead of objects calling each other directly (tight coupling), use a publish-subscribe mechanism where subjects notify observers without knowing who they are.

### The Problem: Tight Coupling

Suppose when a book is checked out, you need to:
1. Update checkout statistics
2. Send a notification
3. Log the event
4. Update recommendations

**Without Observer** (direct coupling):
```java
public class Book {
    private final StatisticsService statisticsService;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;
    private final RecommendationEngine recommendationEngine;

    public void checkOut() {
        this.status = AvailabilityStatus.CHECKED_OUT;

        // Book knows about all these services!
        statisticsService.recordCheckout(this.id);
        notificationService.send("Book checked out: " + this.title);
        auditLogger.log("CHECKOUT", this.id);
        recommendationEngine.updateAfterCheckout(this.id);
    }
}
```

**Problems**:
1. **Tight coupling**: Book depends on 4 services
2. **Hard to extend**: Adding new behavior requires modifying Book
3. **Hard to test**: Must mock 4 services
4. **Violates Single Responsibility**: Book does too much

### The Solution: Publish Events

With Observer, Book just publishes an event:

```java
public class Book {
    public void checkOut() {
        this.status = AvailabilityStatus.CHECKED_OUT;
        registerEvent(new BookCheckedOutEvent(this.id));  // That's it!
    }
}
```

Observers subscribe to the event:
```java
@Component
public class StatisticsUpdater {
    @EventListener
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        updateStatistics(event.getBookId());
    }
}

@Component
public class CheckoutNotifier {
    @EventListener
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        sendNotification(event.getBookId());
    }
}
```

**Beautiful!** Book is decoupled from observers. Adding new behavior is just adding a new listener.

---

## Observer Pattern Structure

### Components

1. **Subject**: Object that publishes events
2. **Observer**: Object that subscribes to events
3. **Event**: Data passed from subject to observers

### UML Diagram

```
┌──────────────┐                    ┌────────────────┐
│   Subject    │                    │    Observer    │
│   (Book)     │────publishes──────>│  (Listeners)   │
├──────────────┤                    ├────────────────┤
│+ checkOut()  │                    │+ update(event) │
└──────────────┘                    └────────────────┘
       │                                     │
       │ creates                             │
       ▼                                     │
┌──────────────────┐                        │
│ BookCheckedOut   │                        │
│     Event        │                        │
└──────────────────┘                        │
                                             │
                    ┌────────────────────────┼─────────────┐
                    │                        │             │
          ┌─────────▼────┐         ┌────────▼────┐  ┌────▼────────┐
          │ Statistics   │         │Notification │  │ Audit       │
          │ Updater      │         │ Sender      │  │ Logger      │
          └──────────────┘         └─────────────┘  └─────────────┘
```

---

## Observer in Bibby: Domain Events

You already learned about Domain Events in Section 6. **Domain Events ARE the Observer pattern!**

Let me show you how it all connects.

### Step 1: Define the Event

```java
package com.penrose.bibby.library.book.event;

import java.time.LocalDateTime;

public class BookCheckedOutEvent {
    private final BookId bookId;
    private final LocalDateTime checkedOutAt;

    public BookCheckedOutEvent(BookId bookId) {
        this.bookId = bookId;
        this.checkedOutAt = LocalDateTime.now();
    }

    public BookId getBookId() {
        return bookId;
    }

    public LocalDateTime getCheckedOutAt() {
        return checkedOutAt;
    }
}
```

### Step 2: Subject Publishes Event

```java
package com.penrose.bibby.library.book;

public class Book extends AggregateRoot<BookId> {
    private AvailabilityStatus status;
    private int checkoutCount;

    public void checkOut() {
        if (this.status != AvailabilityStatus.AVAILABLE) {
            throw new BookNotAvailableException(this.id);
        }

        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;

        // Publish event - observers will be notified
        registerEvent(new BookCheckedOutEvent(this.id));
    }
}
```

### Step 3: Observers Subscribe

```java
package com.penrose.bibby.statistics;

@Component
public class CheckoutStatisticsUpdater {
    private final StatisticsRepository statisticsRepository;

    @EventListener
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        log.info("Updating checkout statistics for book: {}", event.getBookId());

        Statistics stats = statisticsRepository.findByBookId(event.getBookId())
            .orElse(new Statistics(event.getBookId()));

        stats.incrementCheckoutCount();
        stats.recordLastCheckout(event.getCheckedOutAt());

        statisticsRepository.save(stats);
    }
}
```

```java
package com.penrose.bibby.notification;

@Component
public class CheckoutNotificationSender {
    private final NotificationService notificationService;

    @EventListener
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        log.info("Sending checkout notification for book: {}", event.getBookId());

        Book book = bookRepository.findById(event.getBookId())
            .orElseThrow();

        String message = String.format(
            "Book checked out: %s at %s",
            book.getTitle().getValue(),
            event.getCheckedOutAt()
        );

        notificationService.send(message);
    }
}
```

```java
package com.penrose.bibby.audit;

@Component
public class CheckoutAuditLogger {
    private final AuditLogRepository auditLogRepository;

    @EventListener
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        log.info("Logging checkout event for book: {}", event.getBookId());

        AuditLog entry = new AuditLog(
            "BOOK_CHECKOUT",
            event.getBookId().toString(),
            event.getCheckedOutAt()
        );

        auditLogRepository.save(entry);
    }
}
```

### How Spring Wires It Together

Spring's `ApplicationEventPublisher` implements the Observer pattern:

```java
package com.penrose.bibby.library.book;

@Service
public class BookApplicationService {
    private final BookRepository bookRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void checkOutBook(BookId bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.checkOut();  // Registers event internally
        bookRepository.save(book);

        // Publish all registered events
        book.getDomainEvents().forEach(eventPublisher::publishEvent);
        book.clearDomainEvents();
    }
}
```

**Spring automatically**:
1. Finds all `@EventListener` methods
2. Matches event types
3. Calls matching listeners when event is published

---

## Benefits of Observer Pattern

### 1. Decoupling
Subject doesn't know about observers. Add/remove observers without changing subject.

### 2. Open/Closed Principle
Open for extension (add new observers), closed for modification (subject unchanged).

### 3. Single Responsibility
Each observer handles one concern (statistics, notifications, logging).

### 4. Easy Testing
Test subject without observers. Test each observer independently.

---

## When to Use Observer

**Use Observer when:**
1. One object's state change affects many others
2. You don't know how many observers exist (or which ones)
3. You want loose coupling between subject and observers
4. You need to broadcast notifications

**Real-world examples:**
- Event systems (DOM events, Spring Events)
- Model-View-Controller (model notifies views)
- Reactive programming (RxJava, Project Reactor)
- Domain Events (DDD)

---

## The Command Pattern

### What is the Command Pattern?

**Definition**: Encapsulate a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations.

**Key Insight**: Turn methods into objects. This enables powerful features like undo/redo, macro commands, command queuing, and transaction management.

### The Problem: Requests as Method Calls

Normally, you invoke behavior via method calls:

```java
bookService.addBook(title, isbn, authors);
bookService.checkOutBook(bookId);
bookService.returnBook(bookId);
```

**Limitations**:
1. Can't undo operations
2. Can't queue operations for later
3. Can't log operations easily
4. Can't build composite operations (macros)

### The Solution: Requests as Objects

With Command, each request is an object:

```java
Command addBookCmd = new AddBookCommand(title, isbn, authors);
Command checkoutCmd = new CheckOutBookCommand(bookId);

CommandExecutor executor = new CommandExecutor();
executor.execute(addBookCmd);
executor.execute(checkoutCmd);
```

Now you can:
- Store commands in a history (for undo)
- Queue commands for batch processing
- Log commands for audit trail
- Compose commands into macros

---

## Command Pattern Structure

### Components

1. **Command Interface**: Common interface for all commands
2. **Concrete Commands**: Specific commands that encapsulate requests
3. **Receiver**: Object that performs the actual work
4. **Invoker**: Asks command to execute
5. **Client**: Creates commands and sets receivers

### UML Diagram

```
┌─────────────┐
│   Command   │  (Interface)
├─────────────┤
│+ execute()  │
└──────┬──────┘
       │
   ┌───┴────────────────┬──────────────┐
   │                    │              │
┌──▼───────────┐  ┌────▼────────┐  ┌──▼──────────┐
│AddBookCommand│  │CheckoutCmd  │  │ReturnCmd    │
├──────────────┤  ├─────────────┤  ├─────────────┤
│- receiver    │  │- receiver   │  │- receiver   │
│+ execute()   │  │+ execute()  │  │+ execute()  │
└──────────────┘  └─────────────┘  └─────────────┘
       │                  │                │
       └──────────────────┼────────────────┘
                          │ calls
                          ▼
                   ┌─────────────┐
                   │   Receiver  │
                   │(BookService)│
                   ├─────────────┤
                   │+ addBook()  │
                   │+ checkout() │
                   └─────────────┘
```

---

## Implementing Command in Bibby

### Step 1: Command Interface

```java
package com.penrose.bibby.application.command;

public interface Command<R> {
    /**
     * Execute this command and return result
     */
    R execute();

    /**
     * Get command name for logging
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
```

### Step 2: Concrete Commands

```java
package com.penrose.bibby.application.command.book;

public class AddBookCommand implements Command<BookId> {
    private final Title title;
    private final ISBN isbn;
    private final Set<AuthorId> authorIds;
    private final BookRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public AddBookCommand(
        Title title,
        ISBN isbn,
        Set<AuthorId> authorIds,
        BookRepository repository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.title = title;
        this.isbn = isbn;
        this.authorIds = authorIds;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public BookId execute() {
        // Check if book already exists
        if (repository.existsByIsbn(isbn)) {
            throw new DuplicateIsbnException(isbn);
        }

        // Create book
        Book book = Book.builder()
            .id(BookId.generate())
            .title(title)
            .isbn(isbn)
            .authors(authorIds)
            .status(AvailabilityStatus.AVAILABLE)
            .build();

        // Save
        repository.save(book);

        // Publish event
        eventPublisher.publishEvent(new BookAddedEvent(book.getId()));

        return book.getId();
    }
}
```

```java
public class CheckOutBookCommand implements Command<Void> {
    private final BookId bookId;
    private final BookRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public CheckOutBookCommand(
        BookId bookId,
        BookRepository repository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.bookId = bookId;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Void execute() {
        Book book = repository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.checkOut();
        repository.save(book);

        // Publish events
        book.getDomainEvents().forEach(eventPublisher::publishEvent);
        book.clearDomainEvents();

        return null;
    }
}
```

```java
public class ReturnBookCommand implements Command<Void> {
    private final BookId bookId;
    private final BookRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public ReturnBookCommand(
        BookId bookId,
        BookRepository repository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.bookId = bookId;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Void execute() {
        Book book = repository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.returnToLibrary();
        repository.save(book);

        book.getDomainEvents().forEach(eventPublisher::publishEvent);
        book.clearDomainEvents();

        return null;
    }
}
```

### Step 3: Command Executor (Invoker)

```java
package com.penrose.bibby.application.command;

@Component
public class CommandExecutor {
    private final List<Command<?>> history = new ArrayList<>();

    public <R> R execute(Command<R> command) {
        log.info("Executing command: {}", command.getName());

        try {
            R result = command.execute();
            history.add(command);  // Save for history/undo
            log.info("Command executed successfully: {}", command.getName());
            return result;
        } catch (Exception e) {
            log.error("Command execution failed: {}", command.getName(), e);
            throw new CommandExecutionException(command.getName(), e);
        }
    }

    public List<Command<?>> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void clearHistory() {
        history.clear();
    }
}
```

### Step 4: Using Commands

```java
@ShellComponent
public class BookCommands {
    private final BookRepository bookRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CommandExecutor commandExecutor;

    @ShellMethod("Add a new book")
    public String addBook(
        @ShellOption String title,
        @ShellOption String isbn
    ) {
        Command<BookId> command = new AddBookCommand(
            new Title(title),
            new ISBN(isbn),
            Set.of(),
            bookRepository,
            eventPublisher
        );

        BookId bookId = commandExecutor.execute(command);
        return "Book added with ID: " + bookId.getValue();
    }

    @ShellMethod("Check out a book")
    public String checkOut(@ShellOption Long bookId) {
        Command<Void> command = new CheckOutBookCommand(
            new BookId(bookId),
            bookRepository,
            eventPublisher
        );

        commandExecutor.execute(command);
        return "Book checked out successfully";
    }
}
```

---

## Advanced: Undo Support

Commands can support undo:

```java
public interface UndoableCommand<R> extends Command<R> {
    void undo();
}

public class CheckOutBookCommand implements UndoableCommand<Void> {
    private BookId bookId;
    private BookRepository repository;

    @Override
    public Void execute() {
        Book book = repository.findById(bookId).orElseThrow();
        book.checkOut();
        repository.save(book);
        return null;
    }

    @Override
    public void undo() {
        Book book = repository.findById(bookId).orElseThrow();
        book.returnToLibrary();  // Undo checkout
        repository.save(book);
    }
}
```

Command executor with undo:
```java
public class UndoableCommandExecutor {
    private final Stack<UndoableCommand<?>> executedCommands = new Stack<>();

    public <R> R execute(UndoableCommand<R> command) {
        R result = command.execute();
        executedCommands.push(command);
        return result;
    }

    public void undo() {
        if (!executedCommands.isEmpty()) {
            UndoableCommand<?> command = executedCommands.pop();
            command.undo();
        }
    }
}
```

---

## CQRS: Commands vs Queries

The Command pattern is the foundation of **CQRS** (Command Query Responsibility Segregation).

**Principle**: Separate commands (write operations) from queries (read operations).

**Commands** (modify state):
- `AddBookCommand`
- `CheckOutBookCommand`
- `UpdateBookCommand`

**Queries** (read state):
- `FindBookByIdQuery`
- `SearchBooksQuery`
- `GetBookStatisticsQuery`

Benefits:
- Different models for reads vs writes
- Can optimize each independently
- Clear separation of concerns

---

## When to Use Command

**Use Command when:**
1. You need undo/redo functionality
2. You want to queue operations
3. You need an audit trail of operations
4. You want to build macro commands (compose multiple commands)
5. You're implementing CQRS

**Real-world examples:**
- GUI buttons (each button is a command)
- Transaction systems
- Job queues
- Macro recording
- CQRS architectures

---

## Action Items

### 1. Implement More Domain Events (2-3 hours)

Add events for book lifecycle:
- `BookAddedEvent`
- `BookReturnedEvent`
- `BookRemovedEvent`
- Create listeners for each (statistics, notifications, audit)

### 2. Build Command Objects (3-4 hours)

Refactor your application service to use commands:
- Create `Command<R>` interface
- Implement `AddBookCommand`, `UpdateBookCommand`, `DeleteBookCommand`
- Create `CommandExecutor`
- Test each command independently

### 3. Add Command History (2 hours)

Extend `CommandExecutor` to track command history:
- Store executed commands
- Display command history in shell
- Export command log to file

### 4. Implement Undo for One Command (2-3 hours)

Make `CheckOutBookCommand` undoable:
- Create `UndoableCommand` interface
- Implement `undo()` method
- Test undo functionality

---

## Key Takeaways

### 1. Observer Decouples Subject from Observers
- Subject publishes events without knowing observers
- Observers subscribe independently
- Easy to add new observers without changing subject

### 2. Domain Events ARE Observer Pattern
- Spring's `@EventListener` is Observer
- Domain events enable loose coupling between aggregates
- One transaction = one aggregate + events

### 3. Command Turns Requests into Objects
- Methods become objects
- Enables undo, queuing, logging, composition
- Foundation of CQRS

### 4. Both Patterns Enable Extensibility
- Observer: Add new event listeners without changing publishers
- Command: Add new commands without changing infrastructure
- Open/Closed Principle in action

### 5. Spring Provides Both Patterns
- Observer: `ApplicationEventPublisher`, `@EventListener`
- Command: Not built-in, but easy to implement
- Use framework features where possible

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - Observer (p. 293), Command (p. 233)
- "Implementing Domain-Driven Design" by Vaughn Vernon - Domain Events
- "CQRS Journey" by Microsoft patterns & practices

**Articles**:
- Martin Fowler: "Event Sourcing"
- Greg Young: "CQRS Documents"
- Baeldung: "Spring Events"

**Code Examples**:
- Spring Framework: Event publishing
- Your Bibby app: Domain events!

---

## Mentor's Notes

**Observer** is everywhere once you see it. Every event system - from GUI frameworks to message queues - uses Observer. You've already been using it with Domain Events. Now you understand the pattern behind it.

**Command** is less common in typical applications but incredibly powerful when you need it. If you ever build an editor with undo, a job queue, or a CQRS system, you'll reach for Command.

Both patterns promote loose coupling and extensibility. Master them, and you'll write more maintainable code.

Next up: **State** and **Chain of Responsibility** - managing state transitions and request handling!

---

**Section 15 Complete** | **Next**: Section 16 - State & Chain of Responsibility Patterns
