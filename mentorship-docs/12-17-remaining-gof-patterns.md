# SECTIONS 12-17: REMAINING GANG OF FOUR PATTERNS

## Complete Reference Guide

This document consolidates the remaining Gang of Four patterns (Sections 12-17) in a concise, practical format focused on your Bibby application.

---

## SECTION 12: DECORATOR & PROXY PATTERNS

### Decorator Pattern

**Intent**: Attach additional responsibilities to an object dynamically. Decorators provide a flexible alternative to subclassing for extending functionality.

**Real Example in Bibby - Enhanced Book Display**:
```java
// Component interface
public interface BookDisplay {
    String display();
}

// Concrete component
public class BasicBookDisplay implements BookDisplay {
    private final Book book;

    @Override
    public String display() {
        return book.getTitle().getValue();
    }
}

// Decorator base
public abstract class BookDisplayDecorator implements BookDisplay {
    protected final BookDisplay decoratedDisplay;

    public BookDisplayDecorator(BookDisplay display) {
        this.decoratedDisplay = display;
    }
}

// Concrete decorators
public class WithAuthorsDecorator extends BookDisplayDecorator {
    @Override
    public String display() {
        return decoratedDisplay.display() + " by " + getAuthors();
    }
}

public class WithIsbnDecorator extends BookDisplayDecorator {
    @Override
    public String display() {
        return decoratedDisplay.display() + " [ISBN: " + getIsbn() + "]";
    }
}

// Usage - stack decorators
BookDisplay display = new BasicBookDisplay(book);
display = new WithAuthorsDecorator(display);
display = new WithIsbnDecorator(display);
System.out.println(display.display());
// Output: "Clean Code by Robert Martin [ISBN: 978-0132350884]"
```

**When to Use**: Add responsibilities dynamically, avoid class explosion from subclassing.

### Proxy Pattern

**Intent**: Provide a surrogate or placeholder for another object to control access to it.

**Types of Proxies**:
1. **Virtual Proxy**: Lazy loading expensive objects
2. **Protection Proxy**: Access control
3. **Remote Proxy**: Remote object access
4. **Cache Proxy**: Cache results

**Real Example in Bibby - Lazy Loading Book Metadata**:
```java
// Subject interface
public interface BookMetadataProvider {
    BookMetadata getMetadata(ISBN isbn);
}

// Real subject
public class ExternalApiMetadataProvider implements BookMetadataProvider {
    @Override
    public BookMetadata getMetadata(ISBN isbn) {
        // Expensive API call
        return callExternalApi(isbn);
    }
}

// Virtual proxy with lazy loading
public class CachedMetadataProxy implements BookMetadataProvider {
    private final BookMetadataProvider realProvider;
    private final Map<ISBN, BookMetadata> cache = new HashMap<>();

    @Override
    public BookMetadata getMetadata(ISBN isbn) {
        return cache.computeIfAbsent(isbn, realProvider::getMetadata);
    }
}

// Usage
BookMetadataProvider provider = new CachedMetadataProxy(
    new ExternalApiMetadataProvider()
);
provider.getMetadata(isbn);  // Hits API
provider.getMetadata(isbn);  // Returns cached
```

**Spring AOP as Proxy**: Spring's `@Transactional`, `@Cacheable` use dynamic proxies!

---

## SECTION 13: COMPOSITE & BRIDGE PATTERNS

### Composite Pattern

**Intent**: Compose objects into tree structures to represent part-whole hierarchies. Lets clients treat individual objects and compositions uniformly.

**Real Example in Bibby - Bookcase Hierarchy**:
```java
// Component
public interface LibraryComponent {
    int getBookCount();
    List<Book> getAllBooks();
    void display(int indent);
}

// Leaf
public class Book implements LibraryComponent {
    @Override
    public int getBookCount() { return 1; }

    @Override
    public List<Book> getAllBooks() { return List.of(this); }
}

// Composite
public class Shelf implements LibraryComponent {
    private final List<Book> books = new ArrayList<>();

    @Override
    public int getBookCount() {
        return books.size();
    }

    @Override
    public List<Book> getAllBooks() {
        return new ArrayList<>(books);
    }
}

// Composite
public class Bookcase implements LibraryComponent {
    private final List<Shelf> shelves = new ArrayList<>();

    @Override
    public int getBookCount() {
        return shelves.stream()
            .mapToInt(Shelf::getBookCount)
            .sum();
    }

    @Override
    public List<Book> getAllBooks() {
        return shelves.stream()
            .flatMap(shelf -> shelf.getAllBooks().stream())
            .collect(Collectors.toList());
    }
}

// Usage - treat Book, Shelf, Bookcase uniformly
LibraryComponent component = bookcase;  // or shelf, or book
int totalBooks = component.getBookCount();  // Works for all!
```

**When to Use**: Tree structures, part-whole hierarchies, uniform treatment of individuals and groups.

### Bridge Pattern

**Intent**: Decouple an abstraction from its implementation so the two can vary independently.

**Real Example in Bibby - Book Exporters**:
```java
// Implementation interface
public interface ExportFormat {
    String format(Book book);
}

// Concrete implementations
public class JsonFormat implements ExportFormat {
    @Override
    public String format(Book book) {
        return "{\"title\":\"" + book.getTitle() + "\"}";
    }
}

public class XmlFormat implements ExportFormat {
    @Override
    public String format(Book book) {
        return "<book><title>" + book.getTitle() + "</title></book>";
    }
}

// Abstraction
public abstract class BookExporter {
    protected final ExportFormat format;

    protected BookExporter(ExportFormat format) {
        this.format = format;
    }

    public abstract void export(Book book);
}

// Refined abstractions
public class FileExporter extends BookExporter {
    @Override
    public void export(Book book) {
        String content = format.format(book);
        Files.writeString(Path.of("book.txt"), content);
    }
}

public class EmailExporter extends BookExporter {
    @Override
    public void export(Book book) {
        String content = format.format(book);
        emailService.send(content);
    }
}

// Usage - mix and match!
BookExporter exporter = new FileExporter(new JsonFormat());
exporter = new EmailExporter(new XmlFormat());
```

**When to Use**: Two dimensions that vary independently, avoid cartesian explosion of subclasses.

---

## SECTION 14: STRATEGY & TEMPLATE METHOD PATTERNS

### Strategy Pattern

**Intent**: Define a family of algorithms, encapsulate each one, and make them interchangeable.

**Real Example in Bibby - Book Sorting**:
```java
// Strategy interface
public interface BookSortStrategy {
    List<Book> sort(List<Book> books);
}

// Concrete strategies
public class SortByTitle implements BookSortStrategy {
    @Override
    public List<Book> sort(List<Book> books) {
        return books.stream()
            .sorted(Comparator.comparing(b -> b.getTitle().getValue()))
            .collect(Collectors.toList());
    }
}

public class SortByAuthor implements BookSortStrategy {
    @Override
    public List<Book> sort(List<Book> books) {
        return books.stream()
            .sorted(Comparator.comparing(this::getFirstAuthorName))
            .collect(Collectors.toList());
    }
}

public class SortByCheckoutCount implements BookSortStrategy {
    @Override
    public List<Book> sort(List<Book> books) {
        return books.stream()
            .sorted(Comparator.comparingInt(Book::getCheckoutCount).reversed())
            .collect(Collectors.toList());
    }
}

// Context
public class BookList {
    private BookSortStrategy sortStrategy;

    public void setSortStrategy(BookSortStrategy strategy) {
        this.sortStrategy = strategy;
    }

    public List<Book> getSortedBooks(List<Book> books) {
        return sortStrategy.sort(books);
    }
}

// Usage
BookList bookList = new BookList();
bookList.setSortStrategy(new SortByTitle());
List<Book> sorted = bookList.getSortedBooks(books);

// Change strategy at runtime
bookList.setSortStrategy(new SortByCheckoutCount());
sorted = bookList.getSortedBooks(books);
```

**When to Use**: Multiple algorithms for same task, eliminate conditionals, runtime algorithm selection.

### Template Method Pattern

**Intent**: Define the skeleton of an algorithm in a method, deferring some steps to subclasses.

**Real Example in Bibby - Book Import Process**:
```java
// Abstract class with template method
public abstract class BookImporter {

    // Template method - defines algorithm skeleton
    public final ImportResult importBook(String source) {
        // Step 1: Parse (varies by subclass)
        BookData data = parseSource(source);

        // Step 2: Validate (common)
        if (!validate(data)) {
            return ImportResult.failed("Validation failed");
        }

        // Step 3: Create book (varies by subclass)
        Book book = createBook(data);

        // Step 4: Save (common)
        bookRepository.save(book);

        // Step 5: Post-processing (optional hook)
        afterImport(book);

        return ImportResult.success(book.getId());
    }

    // Abstract methods - must implement
    protected abstract BookData parseSource(String source);
    protected abstract Book createBook(BookData data);

    // Hook methods - can override
    protected void afterImport(Book book) {
        // Default: do nothing
    }

    // Common methods
    private boolean validate(BookData data) {
        return data.getTitle() != null && !data.getTitle().isBlank();
    }
}

// Concrete implementations
public class CsvBookImporter extends BookImporter {
    @Override
    protected BookData parseSource(String csv) {
        String[] parts = csv.split(",");
        return new BookData(parts[0], parts[1], parts[2]);
    }

    @Override
    protected Book createBook(BookData data) {
        return Book.builder()
            .title(data.getTitle())
            .isbn(data.getIsbn())
            .build();
    }
}

public class JsonBookImporter extends BookImporter {
    @Override
    protected BookData parseSource(String json) {
        return jsonParser.parse(json, BookData.class);
    }

    @Override
    protected Book createBook(BookData data) {
        return Book.builder()
            .title(data.getTitle())
            .isbn(data.getIsbn())
            .build();
    }

    @Override
    protected void afterImport(Book book) {
        eventPublisher.publish(new BookImportedEvent(book.getId()));
    }
}
```

**When to Use**: Algorithmic invariant parts + variant steps, avoid code duplication, control extension points.

**Strategy vs Template Method**: Strategy uses composition (inject strategy), Template Method uses inheritance (subclass).

---

## SECTION 15: OBSERVER & COMMAND PATTERNS

### Observer Pattern

**Intent**: Define a one-to-many dependency between objects so that when one changes state, all dependents are notified.

**Real Example in Bibby - Domain Events (You Already Use This!)**:
```java
// Subject (in Spring, it's ApplicationEventPublisher)
public class Book {
    public void checkOut() {
        this.status = AvailabilityStatus.CHECKED_OUT;
        // Notify observers
        registerEvent(new BookCheckedOutEvent(this.id));
    }
}

// Observers
@Component
public class StatisticsUpdater {
    @EventListener
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        updateStatistics(event.getBookId());
    }
}

@Component
public class NotificationSender {
    @EventListener
    public void onBookCheckedOut(BookCheckedOutEvent event) {
        sendNotification("Book checked out: " + event.getBookId());
    }
}
```

**Connection to DDD**: Domain Events (Section 6) ARE the Observer pattern!

### Command Pattern

**Intent**: Encapsulate a request as an object, allowing parameterization and queuing of requests.

**Real Example in Bibby - Command Objects for Use Cases**:
```java
// Command interface
public interface Command<R> {
    R execute();
}

// Concrete commands
public class AddBookCommand implements Command<BookId> {
    private final Title title;
    private final ISBN isbn;
    private final Set<AuthorId> authorIds;
    private final BookRepository repository;

    @Override
    public BookId execute() {
        Book book = Book.builder()
            .id(BookId.generate())
            .title(title)
            .isbn(isbn)
            .authors(authorIds)
            .build();

        repository.save(book);
        return book.getId();
    }
}

public class CheckOutBookCommand implements Command<Void> {
    private final BookId bookId;
    private final BookRepository repository;

    @Override
    public Void execute() {
        Book book = repository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.checkOut();
        repository.save(book);
        return null;
    }
}

// Command executor
public class CommandExecutor {
    public <R> R execute(Command<R> command) {
        // Can add logging, validation, undo, etc.
        log.info("Executing command: " + command.getClass());
        R result = command.execute();
        log.info("Command completed");
        return result;
    }
}

// Usage
CommandExecutor executor = new CommandExecutor();
BookId id = executor.execute(new AddBookCommand(title, isbn, authors, repo));
```

**Benefits**: Undo/redo, command queuing, logging, macro commands.

**Connection to CQRS**: Command objects separate commands (write) from queries (read).

---

## SECTION 16: STATE & CHAIN OF RESPONSIBILITY PATTERNS

### State Pattern

**Intent**: Allow an object to alter its behavior when its internal state changes.

**Real Example in Bibby - Book Availability States**:
```java
// State interface
public interface AvailabilityState {
    void checkOut(Book book);
    void returnToLibrary(Book book);
    void reportLost(Book book);
    String getStatusName();
}

// Concrete states
public class AvailableState implements AvailabilityState {
    @Override
    public void checkOut(Book book) {
        book.setState(new CheckedOutState());
        book.incrementCheckoutCount();
    }

    @Override
    public void returnToLibrary(Book book) {
        throw new IllegalStateException("Book is already available");
    }

    @Override
    public void reportLost(Book book) {
        book.setState(new LostState());
    }

    @Override
    public String getStatusName() { return "AVAILABLE"; }
}

public class CheckedOutState implements AvailabilityState {
    @Override
    public void checkOut(Book book) {
        throw new IllegalStateException("Book is already checked out");
    }

    @Override
    public void returnToLibrary(Book book) {
        book.setState(new AvailableState());
    }

    @Override
    public void reportLost(Book book) {
        book.setState(new LostState());
    }

    @Override
    public String getStatusName() { return "CHECKED_OUT"; }
}

public class LostState implements AvailabilityState {
    @Override
    public void checkOut(Book book) {
        throw new IllegalStateException("Book is lost");
    }

    @Override
    public void returnToLibrary(Book book) {
        book.setState(new AvailableState());  // Found it!
    }

    @Override
    public void reportLost(Book book) {
        // Already lost
    }

    @Override
    public String getStatusName() { return "LOST"; }
}

// Context (Book)
public class Book {
    private AvailabilityState state = new AvailableState();

    public void checkOut() {
        state.checkOut(this);  // Delegate to state
    }

    public void returnToLibrary() {
        state.returnToLibrary(this);
    }

    public void reportLost() {
        state.reportLost(this);
    }

    void setState(AvailabilityState newState) {
        this.state = newState;
    }
}
```

**Benefits**: Eliminates state conditionals, each state is a class, easy to add new states.

### Chain of Responsibility Pattern

**Intent**: Avoid coupling sender to receiver by giving multiple objects a chance to handle the request.

**Real Example in Bibby - Validation Chain**:
```java
// Handler interface
public interface ValidationHandler {
    ValidationResult validate(Book book);
    void setNext(ValidationHandler next);
}

// Abstract handler
public abstract class AbstractValidationHandler implements ValidationHandler {
    private ValidationHandler next;

    @Override
    public void setNext(ValidationHandler next) {
        this.next = next;
    }

    protected ValidationResult validateNext(Book book) {
        if (next != null) {
            return next.validate(book);
        }
        return ValidationResult.valid();
    }
}

// Concrete handlers
public class TitleValidator extends AbstractValidationHandler {
    @Override
    public ValidationResult validate(Book book) {
        if (book.getTitle() == null || book.getTitle().getValue().isBlank()) {
            return ValidationResult.invalid("Title is required");
        }
        return validateNext(book);
    }
}

public class IsbnValidator extends AbstractValidationHandler {
    @Override
    public ValidationResult validate(Book book) {
        if (book.getIsbn() == null) {
            return ValidationResult.invalid("ISBN is required");
        }
        return validateNext(book);
    }
}

public class AuthorValidator extends AbstractValidationHandler {
    @Override
    public ValidationResult validate(Book book) {
        if (book.getAuthorIds().isEmpty()) {
            return ValidationResult.invalid("At least one author required");
        }
        return validateNext(book);
    }
}

// Setup chain
ValidationHandler chain = new TitleValidator();
ValidationHandler isbn = new IsbnValidator();
ValidationHandler author = new AuthorValidator();

chain.setNext(isbn);
isbn.setNext(author);

// Usage
ValidationResult result = chain.validate(book);
if (!result.isValid()) {
    throw new ValidationException(result.getError());
}
```

**When to Use**: Multiple handlers for a request, dynamic handler composition, decouple sender from receivers.

---

## SECTION 17: VISITOR, ITERATOR, MEMENTO PATTERNS

### Visitor Pattern

**Intent**: Represent an operation to be performed on elements of an object structure. Lets you define new operations without changing the classes.

**Real Example in Bibby - Book Statistics**:
```java
// Element interface
public interface LibraryElement {
    void accept(LibraryVisitor visitor);
}

// Concrete elements
public class Book implements LibraryElement {
    @Override
    public void accept(LibraryVisitor visitor) {
        visitor.visitBook(this);
    }
}

public class Shelf implements LibraryElement {
    private List<Book> books;

    @Override
    public void accept(LibraryVisitor visitor) {
        visitor.visitShelf(this);
        books.forEach(book -> book.accept(visitor));
    }
}

// Visitor interface
public interface LibraryVisitor {
    void visitBook(Book book);
    void visitShelf(Shelf shelf);
    void visitBookcase(Bookcase bookcase);
}

// Concrete visitors
public class BookCountVisitor implements LibraryVisitor {
    private int totalBooks = 0;

    @Override
    public void visitBook(Book book) {
        totalBooks++;
    }

    @Override
    public void visitShelf(Shelf shelf) {
        // Just traverse
    }

    @Override
    public void visitBookcase(Bookcase bookcase) {
        // Just traverse
    }

    public int getTotalBooks() { return totalBooks; }
}

public class CheckoutStatisticsVisitor implements LibraryVisitor {
    private int totalCheckouts = 0;

    @Override
    public void visitBook(Book book) {
        totalCheckouts += book.getCheckoutCount();
    }

    // ... other methods
}

// Usage
Bookcase bookcase = // ...
BookCountVisitor counter = new BookCountVisitor();
bookcase.accept(counter);
System.out.println("Total books: " + counter.getTotalBooks());
```

**When to Use**: Perform operations on complex object structures, add operations without modifying elements.

### Iterator Pattern

**Intent**: Provide a way to access elements of a collection sequentially without exposing representation.

**Note**: Java's `Iterator` interface already provides this!

```java
// Already built-in!
List<Book> books = bookRepository.findAll();
Iterator<Book> iterator = books.iterator();

while (iterator.hasNext()) {
    Book book = iterator.next();
    System.out.println(book.getTitle());
}

// Or use for-each (syntactic sugar for iterator)
for (Book book : books) {
    System.out.println(book.getTitle());
}
```

**When to Use**: Already used everywhere with Java collections!

### Memento Pattern

**Intent**: Capture and externalize an object's internal state for later restoration.

**Real Example in Bibby - Book Edit History**:
```java
// Memento
public class BookMemento {
    private final Title title;
    private final Description description;
    private final LocalDateTime savedAt;

    public BookMemento(Book book) {
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.savedAt = LocalDateTime.now();
    }

    public Title getTitle() { return title; }
    public Description getDescription() { return description; }
}

// Originator
public class Book {
    private Title title;
    private Description description;

    public BookMemento save() {
        return new BookMemento(this);
    }

    public void restore(BookMemento memento) {
        this.title = memento.getTitle();
        this.description = memento.getDescription();
    }
}

// Caretaker
public class BookEditHistory {
    private final Stack<BookMemento> history = new Stack<>();

    public void save(Book book) {
        history.push(book.save());
    }

    public void undo(Book book) {
        if (!history.isEmpty()) {
            book.restore(history.pop());
        }
    }
}

// Usage
Book book = // ...
BookEditHistory history = new BookEditHistory();

history.save(book);  // Save state
book.setTitle(new Title("New Title"));

history.undo(book);  // Restore previous state
```

**When to Use**: Undo/redo functionality, snapshots, rollback.

---

## Pattern Selection Guide

### Creational Patterns
- **Need flexible object creation?** → Factory Method
- **Complex object construction?** → Builder
- **Single instance?** → Spring DI (NOT Singleton pattern)

### Structural Patterns
- **Incompatible interface?** → Adapter
- **Simplify complex subsystem?** → Facade
- **Add behavior dynamically?** → Decorator
- **Control access?** → Proxy
- **Tree structures?** → Composite

### Behavioral Patterns
- **Multiple algorithms?** → Strategy
- **Algorithm skeleton?** → Template Method
- **Notify dependents?** → Observer (Domain Events)
- **Encapsulate requests?** → Command
- **State-based behavior?** → State
- **Chain of handlers?** → Chain of Responsibility
- **Operations on structures?** → Visitor

---

## Action Items for Sections 12-17

### Week 1: Structural Patterns Practice
1. Implement Decorator for book display formatting
2. Create Proxy for caching external API calls
3. Build Composite for library hierarchy

### Week 2: Behavioral Patterns Practice
1. Refactor sorting to use Strategy pattern
2. Create validation Chain of Responsibility
3. Implement State pattern for book availability

### Week 3: Integration
1. Identify patterns already in your code
2. Refactor one feature using appropriate pattern
3. Document pattern usage in your codebase

---

**Sections 12-17 Complete** | **Part 2: Gang of Four Design Patterns Complete!**

Next: Part 3 - The Pragmatic Programmer Principles
