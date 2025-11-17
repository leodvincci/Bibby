# SECTION 17: VISITOR, ITERATOR & MEMENTO PATTERNS

## Final Behavioral Patterns for Operations and State

Welcome to Section 17 - the final section on Gang of Four patterns! We'll cover three more behavioral patterns: **Visitor**, **Iterator**, and **Memento**.

**Visitor** lets you add new operations to existing object structures without modifying them.

**Iterator** provides sequential access to elements without exposing internal representation (you already use this daily!).

**Memento** captures and restores an object's internal state - perfect for undo/redo functionality.

These patterns complete your design patterns toolkit!

---

## The Visitor Pattern

### What is the Visitor Pattern?

**Definition**: Represent an operation to be performed on elements of an object structure. Visitor lets you define new operations without changing the classes of the elements on which it operates.

**Key Insight**: Separate algorithms from the objects they operate on. Add new operations without modifying existing classes.

### The Problem: Adding Operations to Class Hierarchies

Suppose you have your library hierarchy (Bookcase → Shelf → Book) and want to perform different operations:
- Count total books
- Calculate total value
- Generate statistics report
- Export to different formats

**Without Visitor**:
```java
public interface LibraryComponent {
    int countBooks();
    double calculateValue();
    String generateReport();
    String exportToJson();
    String exportToXml();
    // Every new operation requires modifying ALL classes!
}

public class Book implements LibraryComponent {
    public int countBooks() { return 1; }
    public double calculateValue() { return this.price; }
    public String generateReport() { /* ... */ }
    public String exportToJson() { /* ... */ }
    public String exportToXml() { /* ... */ }
    // Modification required for every new operation!
}
```

**Problems**:
1. **Violates Open/Closed**: Must modify classes to add operations
2. **Mixed concerns**: Book class contains counting, reporting, exporting
3. **Hard to maintain**: Operations scattered across multiple classes
4. **Can't add operations from outside**: Client code can't add new operations

### The Solution: Visitor Objects

With Visitor, operations are separate visitor classes:

```java
public interface LibraryComponent {
    void accept(LibraryVisitor visitor);  // Only one method!
}

// Each operation is a visitor
public class BookCountVisitor implements LibraryVisitor {
    private int count = 0;

    public void visitBook(Book book) {
        count++;
    }

    public int getCount() { return count; }
}

// Usage
BookCountVisitor visitor = new BookCountVisitor();
bookcase.accept(visitor);
System.out.println("Total books: " + visitor.getCount());
```

**Beautiful!** Add new operations without touching existing classes.

---

## Visitor Pattern Structure

### Components

1. **Element Interface**: Defines `accept(Visitor)` method
2. **Concrete Elements**: Implement `accept()` to call visitor
3. **Visitor Interface**: Declares visit methods for each element type
4. **Concrete Visitors**: Implement specific operations

### UML Diagram

```
┌──────────────────┐
│ LibraryComponent │  (Element Interface)
├──────────────────┤
│+ accept(visitor) │
└────────┬─────────┘
         │
    ┌────┴────┬───────┬──────────┐
    │         │       │          │
┌───▼──┐  ┌──▼───┐ ┌─▼────┐  ┌──▼──────┐
│Book  │  │Shelf │ │Bookcase│  (Elements)
└──────┘  └──────┘ └────────┘
    │         │       │
    └─────────┼───────┘
              │ accept() calls
              ▼
┌──────────────────┐
│ LibraryVisitor   │  (Visitor Interface)
├──────────────────┤
│+ visitBook()     │
│+ visitShelf()    │
│+ visitBookcase() │
└────────┬─────────┘
         │
    ┌────┴────┬────────────┬─────────────┐
    │         │            │             │
┌───▼───┐ ┌──▼──────┐ ┌───▼──────┐ ┌───▼──────┐
│Count  │ │Value    │ │Statistics│ │Export    │
│Visitor│ │Visitor  │ │Visitor   │ │Visitor   │
└───────┘ └─────────┘ └──────────┘ └──────────┘
```

---

## Implementing Visitor in Bibby

### Step 1: Element Interface

```java
package com.penrose.bibby.library;

public interface LibraryComponent {
    /**
     * Accept a visitor to perform an operation
     */
    void accept(LibraryVisitor visitor);
}
```

### Step 2: Concrete Elements

```java
package com.penrose.bibby.library.book;

public class Book implements LibraryComponent {
    private final BookId id;
    private final Title title;
    private final Money price;
    private final int checkoutCount;

    @Override
    public void accept(LibraryVisitor visitor) {
        visitor.visitBook(this);  // Double dispatch!
    }

    // Getters
    public Money getPrice() { return price; }
    public int getCheckoutCount() { return checkoutCount; }
}
```

```java
package com.penrose.bibby.library.shelf;

public class Shelf implements LibraryComponent {
    private final ShelfId id;
    private final List<Book> books;

    @Override
    public void accept(LibraryVisitor visitor) {
        visitor.visitShelf(this);

        // Visit all books on this shelf
        for (Book book : books) {
            book.accept(visitor);
        }
    }

    public List<Book> getBooks() {
        return Collections.unmodifiableList(books);
    }
}
```

```java
package com.penrose.bibby.library.bookcase;

public class Bookcase implements LibraryComponent {
    private final BookcaseId id;
    private final BookcaseName name;
    private final List<Shelf> shelves;

    @Override
    public void accept(LibraryVisitor visitor) {
        visitor.visitBookcase(this);

        // Visit all shelves in this bookcase
        for (Shelf shelf : shelves) {
            shelf.accept(visitor);
        }
    }

    public BookcaseName getName() { return name; }
    public List<Shelf> getShelves() {
        return Collections.unmodifiableList(shelves);
    }
}
```

### Step 3: Visitor Interface

```java
package com.penrose.bibby.library.visitor;

public interface LibraryVisitor {
    void visitBook(Book book);
    void visitShelf(Shelf shelf);
    void visitBookcase(Bookcase bookcase);
}
```

### Step 4: Concrete Visitors

**Book Count Visitor**:
```java
package com.penrose.bibby.library.visitor;

public class BookCountVisitor implements LibraryVisitor {
    private int totalBooks = 0;

    @Override
    public void visitBook(Book book) {
        totalBooks++;
    }

    @Override
    public void visitShelf(Shelf shelf) {
        // Just traverse - books will be visited separately
    }

    @Override
    public void visitBookcase(Bookcase bookcase) {
        // Just traverse - shelves and books will be visited separately
    }

    public int getTotalBooks() {
        return totalBooks;
    }
}
```

**Value Calculator Visitor**:
```java
public class LibraryValueVisitor implements LibraryVisitor {
    private Money totalValue = Money.ZERO;

    @Override
    public void visitBook(Book book) {
        totalValue = totalValue.add(book.getPrice());
    }

    @Override
    public void visitShelf(Shelf shelf) {
        // Just traverse
    }

    @Override
    public void visitBookcase(Bookcase bookcase) {
        // Just traverse
    }

    public Money getTotalValue() {
        return totalValue;
    }
}
```

**Statistics Visitor**:
```java
public class StatisticsVisitor implements LibraryVisitor {
    private int totalBooks = 0;
    private int totalCheckouts = 0;
    private Money totalValue = Money.ZERO;
    private int shelfCount = 0;
    private int bookcaseCount = 0;

    @Override
    public void visitBook(Book book) {
        totalBooks++;
        totalCheckouts += book.getCheckoutCount();
        totalValue = totalValue.add(book.getPrice());
    }

    @Override
    public void visitShelf(Shelf shelf) {
        shelfCount++;
    }

    @Override
    public void visitBookcase(Bookcase bookcase) {
        bookcaseCount++;
    }

    public LibraryStatistics getStatistics() {
        return new LibraryStatistics(
            totalBooks,
            totalCheckouts,
            totalValue,
            shelfCount,
            bookcaseCount,
            totalBooks > 0 ? (double) totalCheckouts / totalBooks : 0
        );
    }
}
```

**Export to JSON Visitor**:
```java
public class JsonExportVisitor implements LibraryVisitor {
    private final StringBuilder json = new StringBuilder();
    private int indent = 0;

    @Override
    public void visitBookcase(Bookcase bookcase) {
        append("{");
        indent++;
        appendLine();
        append("\"type\": \"bookcase\",");
        appendLine();
        append("\"name\": \"" + bookcase.getName().getValue() + "\",");
        appendLine();
        append("\"shelves\": [");
        indent++;
    }

    @Override
    public void visitShelf(Shelf shelf) {
        appendLine();
        append("{");
        indent++;
        appendLine();
        append("\"type\": \"shelf\",");
        appendLine();
        append("\"books\": [");
        indent++;
    }

    @Override
    public void visitBook(Book book) {
        appendLine();
        append("{");
        indent++;
        appendLine();
        append("\"title\": \"" + book.getTitle().getValue() + "\",");
        appendLine();
        append("\"price\": " + book.getPrice().getAmount());
        indent--;
        appendLine();
        append("}");
    }

    public String getJson() {
        return json.toString();
    }

    private void append(String text) {
        json.append(" ".repeat(indent * 2)).append(text);
    }

    private void appendLine() {
        json.append("\n");
    }
}
```

### Step 5: Usage

```java
@Service
public class LibraryStatisticsService {
    private final BookcaseRepository bookcaseRepository;

    public LibraryStatistics calculateStatistics() {
        List<Bookcase> bookcases = bookcaseRepository.findAll();

        StatisticsVisitor visitor = new StatisticsVisitor();

        for (Bookcase bookcase : bookcases) {
            bookcase.accept(visitor);
        }

        return visitor.getStatistics();
    }

    public int countAllBooks() {
        List<Bookcase> bookcases = bookcaseRepository.findAll();

        BookCountVisitor visitor = new BookCountVisitor();

        for (Bookcase bookcase : bookcases) {
            bookcase.accept(visitor);
        }

        return visitor.getTotalBooks();
    }

    public Money calculateTotalValue() {
        List<Bookcase> bookcases = bookcaseRepository.findAll();

        LibraryValueVisitor visitor = new LibraryValueVisitor();

        for (Bookcase bookcase : bookcases) {
            bookcase.accept(visitor);
        }

        return visitor.getTotalValue();
    }
}
```

---

## When to Use Visitor

**Use Visitor when:**
1. You need to perform operations across a complex object structure
2. Operations are unrelated to the objects' primary responsibility
3. Object structure rarely changes but operations change frequently
4. You want to avoid polluting classes with unrelated operations

**Real-world examples:**
- Compiler abstract syntax tree (AST) operations
- Document object model (DOM) traversal
- Reporting on complex hierarchies
- Export to multiple formats

**Warning**: Visitor is powerful but complex. Use it when you truly need to separate operations from object structure.

---

## The Iterator Pattern

### What is the Iterator Pattern?

**Definition**: Provide a way to access the elements of an aggregate object sequentially without exposing its underlying representation.

**Key Insight**: You already use this pattern every day! Java's `Iterator` interface IS the Iterator pattern.

### You're Already Using It!

```java
List<Book> books = bookRepository.findAll();

// Using iterator directly
Iterator<Book> iterator = books.iterator();
while (iterator.hasNext()) {
    Book book = iterator.next();
    System.out.println(book.getTitle());
}

// Using for-each (syntactic sugar for iterator)
for (Book book : books) {
    System.out.println(book.getTitle());
}

// Using streams (built on iterators)
books.stream()
     .filter(Book::isAvailable)
     .forEach(book -> System.out.println(book.getTitle()));
```

### Custom Iterator Example

If you wanted to iterate over books in a specific order:

```java
package com.penrose.bibby.library.bookcase;

public class Bookcase implements Iterable<Book> {
    private final List<Shelf> shelves;

    @Override
    public Iterator<Book> iterator() {
        return new BookcaseIterator();
    }

    private class BookcaseIterator implements Iterator<Book> {
        private int shelfIndex = 0;
        private int bookIndex = 0;

        @Override
        public boolean hasNext() {
            while (shelfIndex < shelves.size()) {
                Shelf currentShelf = shelves.get(shelfIndex);
                if (bookIndex < currentShelf.getBooks().size()) {
                    return true;
                }
                shelfIndex++;
                bookIndex = 0;
            }
            return false;
        }

        @Override
        public Book next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Shelf currentShelf = shelves.get(shelfIndex);
            Book book = currentShelf.getBooks().get(bookIndex);
            bookIndex++;
            return book;
        }
    }
}
```

Usage:
```java
Bookcase bookcase = bookcaseRepository.findById(id).orElseThrow();

// Iterate over all books in bookcase
for (Book book : bookcase) {
    System.out.println(book.getTitle());
}
```

**In practice**: You rarely need to implement custom iterators in Java. Use the built-in collections and streams.

---

## The Memento Pattern

### What is the Memento Pattern?

**Definition**: Without violating encapsulation, capture and externalize an object's internal state so that the object can be restored to this state later.

**Key Insight**: Save snapshots of an object's state for undo/redo functionality, without exposing internal implementation.

### The Problem: Undo Without Breaking Encapsulation

Suppose you want to undo book edits. You need to save the book's state before changes.

**Naive approach** (breaks encapsulation):
```java
// Save state externally
String oldTitle = book.getTitle().getValue();
String oldIsbn = book.getIsbn().getValue();
int oldYear = book.getPublicationYear();

// Make changes
book.setTitle(new Title("New Title"));

// Undo (accessing internals)
book.setTitle(new Title(oldTitle));
book.setIsbn(new ISBN(oldIsbn));
book.setPublicationYear(oldYear);
```

**Problems**:
1. **Tight coupling**: Undo code knows about Book's internals
2. **Fragile**: Adding fields to Book breaks undo
3. **No encapsulation**: Book's state is exposed

### The Solution: Memento Objects

With Memento, Book creates its own snapshot:

```java
// Save
BookMemento memento = book.save();

// Make changes
book.setTitle(new Title("New Title"));

// Undo
book.restore(memento);
```

Book controls what's saved. Memento is opaque to external code.

---

## Memento Pattern Structure

### Components

1. **Originator**: Object whose state is saved (Book)
2. **Memento**: Snapshot of originator's state
3. **Caretaker**: Manages mementos (doesn't inspect them)

### UML Diagram

```
┌──────────────┐              ┌─────────────┐
│   Book       │  creates     │BookMemento  │
│ (Originator) ├─────────────>│             │
├──────────────┤              ├─────────────┤
│+ save()      │              │- title      │
│+ restore()   │              │- isbn       │
│+ edit()      │              │- year       │
└──────────────┘              └─────────────┘
       │                             ▲
       │                             │
       │                          stores
       │                             │
       ▼                             │
┌──────────────┐                     │
│BookEditHistory│                    │
│ (Caretaker)   ├────────────────────┘
├───────────────┤
│- history[]    │
│+ save(book)   │
│+ undo(book)   │
└───────────────┘
```

---

## Implementing Memento in Bibby

### Step 1: Memento Class

```java
package com.penrose.bibby.library.book;

/**
 * Memento - captures Book's state
 * Package-private: only Book can access internals
 */
class BookMemento {
    private final Title title;
    private final Description description;
    private final int publicationYear;
    private final LocalDateTime savedAt;

    // Package-private constructor
    BookMemento(Book book) {
        this.title = book.getTitle();
        this.description = book.getDescription();
        this.publicationYear = book.getPublicationYear();
        this.savedAt = LocalDateTime.now();
    }

    // Package-private getters (only Book can access)
    Title getTitle() {
        return title;
    }

    Description getDescription() {
        return description;
    }

    int getPublicationYear() {
        return publicationYear;
    }

    LocalDateTime getSavedAt() {
        return savedAt;
    }
}
```

### Step 2: Originator (Book)

```java
package com.penrose.bibby.library.book;

public class Book {
    private Title title;
    private Description description;
    private int publicationYear;

    /**
     * Create memento of current state
     */
    public BookMemento save() {
        return new BookMemento(this);
    }

    /**
     * Restore state from memento
     */
    public void restore(BookMemento memento) {
        this.title = memento.getTitle();
        this.description = memento.getDescription();
        this.publicationYear = memento.getPublicationYear();

        log.info("Restored book {} to state from {}",
                 this.id, memento.getSavedAt());
    }

    /**
     * Edit book (mutable operations)
     */
    public void updateTitle(Title newTitle) {
        this.title = newTitle;
    }

    public void updateDescription(Description newDescription) {
        this.description = newDescription;
    }

    public void updatePublicationYear(int year) {
        this.publicationYear = year;
    }
}
```

### Step 3: Caretaker (Edit History)

```java
package com.penrose.bibby.library.book;

public class BookEditHistory {
    private final Stack<BookMemento> history = new Stack<>();
    private final Stack<BookMemento> redoStack = new Stack<>();
    private static final int MAX_HISTORY_SIZE = 50;

    /**
     * Save current state before editing
     */
    public void save(Book book) {
        BookMemento memento = book.save();
        history.push(memento);

        // Clear redo stack when new save occurs
        redoStack.clear();

        // Limit history size
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);  // Remove oldest
        }

        log.debug("Saved book state. History size: {}", history.size());
    }

    /**
     * Undo last change
     */
    public boolean undo(Book book) {
        if (history.isEmpty()) {
            log.warn("Cannot undo: no history available");
            return false;
        }

        // Save current state to redo stack
        redoStack.push(book.save());

        // Restore previous state
        BookMemento previousState = history.pop();
        book.restore(previousState);

        log.info("Undid change. History size: {}", history.size());
        return true;
    }

    /**
     * Redo last undone change
     */
    public boolean redo(Book book) {
        if (redoStack.isEmpty()) {
            log.warn("Cannot redo: no redo history available");
            return false;
        }

        // Save current state to undo stack
        history.push(book.save());

        // Restore next state
        BookMemento nextState = redoStack.pop();
        book.restore(nextState);

        log.info("Redid change. Redo stack size: {}", redoStack.size());
        return true;
    }

    /**
     * Clear all history
     */
    public void clear() {
        history.clear();
        redoStack.clear();
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public int getHistorySize() {
        return history.size();
    }
}
```

### Step 4: Usage in Application Service

```java
package com.penrose.bibby.application.book;

@Service
public class BookEditService {
    private final BookRepository bookRepository;
    private final Map<BookId, BookEditHistory> editHistories = new ConcurrentHashMap<>();

    @Transactional
    public void editBook(BookId bookId, EditBookCommand command) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        // Get or create edit history for this book
        BookEditHistory history = editHistories.computeIfAbsent(
            bookId,
            id -> new BookEditHistory()
        );

        // Save state before editing
        history.save(book);

        // Apply edits
        if (command.getNewTitle() != null) {
            book.updateTitle(command.getNewTitle());
        }
        if (command.getNewDescription() != null) {
            book.updateDescription(command.getNewDescription());
        }
        if (command.getNewYear() != null) {
            book.updatePublicationYear(command.getNewYear());
        }

        bookRepository.save(book);
    }

    @Transactional
    public boolean undoEdit(BookId bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        BookEditHistory history = editHistories.get(bookId);
        if (history == null || !history.canUndo()) {
            return false;
        }

        boolean undone = history.undo(book);
        if (undone) {
            bookRepository.save(book);
        }

        return undone;
    }

    @Transactional
    public boolean redoEdit(BookId bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        BookEditHistory history = editHistories.get(bookId);
        if (history == null || !history.canRedo()) {
            return false;
        }

        boolean redone = history.redo(book);
        if (redone) {
            bookRepository.save(book);
        }

        return redone;
    }
}
```

### Step 5: Shell Commands

```java
@ShellComponent
public class BookCommands {
    private final BookEditService bookEditService;

    @ShellMethod("Edit book details")
    public String editBook(
        @ShellOption Long id,
        @ShellOption(defaultValue = "") String title,
        @ShellOption(defaultValue = "") String description
    ) {
        BookId bookId = new BookId(id);

        EditBookCommand command = new EditBookCommand(
            title.isEmpty() ? null : new Title(title),
            description.isEmpty() ? null : new Description(description),
            null
        );

        bookEditService.editBook(bookId, command);

        return "Book updated successfully. Use 'undo-book-edit' to undo.";
    }

    @ShellMethod("Undo last book edit")
    public String undoBookEdit(@ShellOption Long id) {
        BookId bookId = new BookId(id);

        boolean undone = bookEditService.undoEdit(bookId);

        if (undone) {
            return "Edit undone successfully";
        } else {
            return "Nothing to undo";
        }
    }

    @ShellMethod("Redo last undone book edit")
    public String redoBookEdit(@ShellOption Long id) {
        BookId bookId = new BookId(id);

        boolean redone = bookEditService.redoEdit(bookId);

        if (redone) {
            return "Edit redone successfully";
        } else {
            return "Nothing to redo";
        }
    }
}
```

---

## When to Use Memento

**Use Memento when:**
1. You need to save/restore object state (undo/redo)
2. You want to preserve encapsulation (don't expose internals)
3. You need snapshots for rollback
4. You're implementing transactional behavior

**Real-world examples:**
- Text editor undo/redo
- Database transactions (savepoints)
- Game state saves
- Configuration snapshots

---

## Action Items

### 1. Implement Visitor for Library Statistics (3-4 hours)

Create visitors for your library hierarchy:
- `BookCountVisitor`
- `LibraryValueVisitor`
- `StatisticsVisitor`
- `JsonExportVisitor`
- Test each visitor independently

### 2. Add Memento for Book Editing (2-3 hours)

Implement undo/redo for book edits:
- Create `BookMemento` class
- Add `save()` and `restore()` to Book
- Implement `BookEditHistory` caretaker
- Add shell commands for undo/redo
- Test undo/redo functionality

### 3. Custom Iterator for Filtered Books (2 hours)

Create an iterator that only returns available books:
```java
public class AvailableBooksIterator implements Iterator<Book> {
    // Filter out checked out books
}
```

### 4. Visitor for Different Export Formats (2 hours)

Create visitors for exporting library data:
- `CsvExportVisitor`
- `XmlExportVisitor`
- `MarkdownExportVisitor`

---

## Key Takeaways

### 1. Visitor Separates Operations from Structure
- Add operations without modifying classes
- Great for stable hierarchies with varying operations
- Uses double dispatch for polymorphism

### 2. Iterator Provides Sequential Access
- Already built into Java collections
- Rarely need custom iterators
- Streams are built on iterators

### 3. Memento Preserves Encapsulation
- Save/restore state without exposing internals
- Perfect for undo/redo
- Caretaker manages mementos

### 4. All Three Are Behavioral Patterns
- Focus on responsibilities and communication
- Visitor: operations on structures
- Iterator: traversal
- Memento: state management

### 5. Choose the Right Tool
- Don't over-engineer
- Use built-in Java features when possible
- Apply patterns when they truly fit

---

## Gang of Four Patterns: Complete! 🎉

You've now learned all 23 Gang of Four design patterns:

**Creational**: Factory Method, Abstract Factory, Builder, Prototype, Singleton

**Structural**: Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy

**Behavioral**: Chain of Responsibility, Command, Iterator, Mediator, Memento, Observer, State, Strategy, Template Method, Visitor

**Congratulations!** You have a complete design patterns toolkit.

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - Visitor (p. 331), Iterator (p. 257), Memento (p. 283)
- "Head First Design Patterns" - Excellent final chapters

**Articles**:
- Refactoring Guru: Complete catalog of all patterns
- Martin Fowler: Pattern catalog

**Practice**:
- Refactor your Bibby code using patterns
- Recognize patterns in frameworks you use
- Don't force patterns where they don't fit

---

## Mentor's Final Words on Design Patterns

You've completed all Gang of Four patterns. That's a huge accomplishment!

**Now what?**

1. **Don't memorize**: Understand when to use each pattern
2. **Recognize patterns**: See them in Spring, JDK, frameworks
3. **Apply judiciously**: Patterns solve problems - don't create problems to use patterns
4. **Refactor to patterns**: Start simple, refactor to patterns when needed
5. **Keep learning**: Patterns are a foundation, not the destination

The goal isn't to use all patterns. The goal is to **recognize when a pattern fits** and apply it elegantly.

Next up: **Part 3 - The Pragmatic Programmer!** We'll learn principles that transcend patterns and make you a better engineer overall.

---

**Section 17 Complete** | **Part 2 Complete!** | **Next**: Part 3 - The Pragmatic Programmer

**Sections 12-17 created as full individual files** ✅
