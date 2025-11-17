# SECTION 13: COMPOSITE & BRIDGE PATTERNS

## Structural Patterns for Hierarchies and Abstraction

Welcome to Section 13! We're continuing our exploration of structural design patterns. In this section, we'll dive into **Composite** and **Bridge** patterns - two patterns that solve very different problems but both promote flexible, maintainable designs.

**Composite** helps you work with tree structures (like your bookcase → shelf → book hierarchy).
**Bridge** helps you decouple abstractions from implementations so they can vary independently.

---

## The Composite Pattern

### What is the Composite Pattern?

**Definition**: Compose objects into tree structures to represent part-whole hierarchies. Composite lets clients treat individual objects and compositions of objects uniformly.

**Key Insight**: You can have a single interface for both **leaf nodes** (individual items) and **composite nodes** (collections of items). This makes tree traversal simple and elegant.

### The Problem: Part-Whole Hierarchies

In Bibby, you have a natural hierarchy:
```
Bookcase
├── Shelf 1
│   ├── Book 1
│   ├── Book 2
│   └── Book 3
├── Shelf 2
│   ├── Book 4
│   └── Book 5
└── Shelf 3
    └── Book 6
```

You want to answer questions like:
- How many books are in this bookcase?
- How many books are on this shelf?
- What are all the books in this container (shelf or bookcase)?

**Without Composite**, you'd write separate code for each level:
```java
// For a book
int count = 1;

// For a shelf
int count = shelf.getBooks().size();

// For a bookcase
int count = 0;
for (Shelf shelf : bookcase.getShelves()) {
    count += shelf.getBooks().size();
}
```

**Ugly!** Different logic for each level. Not scalable.

### The Solution: Uniform Interface

With Composite, every level implements the same interface:
```java
interface LibraryComponent {
    int getBookCount();
    List<Book> getAllBooks();
}
```

Now:
- `Book` implements `LibraryComponent` (returns 1 book)
- `Shelf` implements `LibraryComponent` (returns count of books it contains)
- `Bookcase` implements `LibraryComponent` (recursively counts books in all shelves)

**Beautiful!** Same interface, polymorphic behavior.

---

## Composite Pattern Structure

### Components

1. **Component**: Common interface for leaves and composites
2. **Leaf**: Individual object with no children (e.g., `Book`)
3. **Composite**: Container object with children (e.g., `Shelf`, `Bookcase`)

### UML Diagram

```
┌────────────────────┐
│ LibraryComponent   │  (Component)
├────────────────────┤
│ + getBookCount()   │
│ + getAllBooks()    │
│ + display(indent)  │
└──────────┬─────────┘
           │
      ┌────┴────────────────┐
      │                     │
┌─────▼────┐          ┌─────▼──────┐
│   Book   │  (Leaf)  │   Shelf    │  (Composite)
└──────────┘          ├────────────┤
                      │ - books[]  │
                      └─────┬──────┘
                            │
                      ┌─────▼──────┐
                      │  Bookcase  │  (Composite)
                      ├────────────┤
                      │ - shelves[]│
                      └────────────┘
```

---

## Implementing Composite in Bibby

### Step 1: Component Interface

```java
package com.penrose.bibby.library;

public interface LibraryComponent {
    /**
     * Returns the total number of books in this component
     */
    int getBookCount();

    /**
     * Returns all books in this component (recursive for composites)
     */
    List<Book> getAllBooks();

    /**
     * Display this component with indentation (for tree visualization)
     */
    void display(int indent);
}
```

### Step 2: Leaf (Book)

```java
package com.penrose.bibby.library.book;

public class Book implements LibraryComponent {
    private final BookId id;
    private final Title title;
    private final ISBN isbn;
    private final Set<AuthorId> authorIds;

    @Override
    public int getBookCount() {
        return 1;  // A book is a single book
    }

    @Override
    public List<Book> getAllBooks() {
        return List.of(this);  // Returns itself
    }

    @Override
    public void display(int indent) {
        String indentation = " ".repeat(indent);
        System.out.println(indentation + "📖 " + title.getValue());
    }

    // ... other Book methods
}
```

### Step 3: Composite (Shelf)

```java
package com.penrose.bibby.library.shelf;

public class Shelf implements LibraryComponent {
    private final ShelfId id;
    private final ShelfNumber number;
    private final List<Book> books = new ArrayList<>();

    public void addBook(Book book) {
        books.add(book);
    }

    public void removeBook(Book book) {
        books.remove(book);
    }

    @Override
    public int getBookCount() {
        // Sum of all books on this shelf
        return books.size();
    }

    @Override
    public List<Book> getAllBooks() {
        // Return all books (already flat at this level)
        return new ArrayList<>(books);
    }

    @Override
    public void display(int indent) {
        String indentation = " ".repeat(indent);
        System.out.println(indentation + "📚 Shelf " + number.getValue());

        // Display each book with more indentation
        for (Book book : books) {
            book.display(indent + 2);
        }
    }
}
```

### Step 4: Composite (Bookcase)

```java
package com.penrose.bibby.library.bookcase;

public class Bookcase implements LibraryComponent {
    private final BookcaseId id;
    private final BookcaseName name;
    private final List<Shelf> shelves = new ArrayList<>();

    public void addShelf(Shelf shelf) {
        shelves.add(shelf);
    }

    public void removeShelf(Shelf shelf) {
        shelves.remove(shelf);
    }

    @Override
    public int getBookCount() {
        // Recursively sum books from all shelves
        return shelves.stream()
            .mapToInt(Shelf::getBookCount)
            .sum();
    }

    @Override
    public List<Book> getAllBooks() {
        // Recursively collect books from all shelves
        return shelves.stream()
            .flatMap(shelf -> shelf.getAllBooks().stream())
            .collect(Collectors.toList());
    }

    @Override
    public void display(int indent) {
        String indentation = " ".repeat(indent);
        System.out.println(indentation + "🗄️  Bookcase: " + name.getValue());

        // Display each shelf with more indentation
        for (Shelf shelf : shelves) {
            shelf.display(indent + 2);
        }
    }
}
```

### Step 5: Usage - Uniform Treatment!

```java
// Build hierarchy
Bookcase bookcase = new Bookcase(id, new BookcaseName("Living Room"));

Shelf shelf1 = new Shelf(shelfId1, new ShelfNumber(1));
shelf1.addBook(book1);  // "Clean Code"
shelf1.addBook(book2);  // "Refactoring"

Shelf shelf2 = new Shelf(shelfId2, new ShelfNumber(2));
shelf2.addBook(book3);  // "Domain-Driven Design"

bookcase.addShelf(shelf1);
bookcase.addShelf(shelf2);

// Uniform interface - works for all levels!
LibraryComponent component;

component = book1;
System.out.println(component.getBookCount());  // 1

component = shelf1;
System.out.println(component.getBookCount());  // 2

component = bookcase;
System.out.println(component.getBookCount());  // 3

// Display hierarchy
bookcase.display(0);
```

**Output**:
```
🗄️  Bookcase: Living Room
  📚 Shelf 1
    📖 Clean Code
    📖 Refactoring
  📚 Shelf 2
    📖 Domain-Driven Design
```

**Beautiful!** Same code works for books, shelves, and bookcases.

---

## When to Use Composite

**Use Composite when:**
1. You have a part-whole hierarchy (tree structure)
2. You want to treat individual objects and compositions uniformly
3. You need to perform operations recursively (count, search, sum)
4. You have nested structures

**Real-world examples:**
- File systems (folders contain files and folders)
- UI components (containers hold buttons and other containers)
- Organization charts (departments contain teams)
- Your library structure!

---

## The Bridge Pattern

### What is the Bridge Pattern?

**Definition**: Decouple an abstraction from its implementation so that the two can vary independently.

**Key Insight**: When you have **two dimensions** that vary independently (e.g., shapes and colors, or exporters and formats), use Bridge to avoid a combinatorial explosion of subclasses.

### The Problem: Cartesian Product Explosion

Suppose you want to export books in different formats (JSON, XML, CSV) to different destinations (File, Email, Database).

**Without Bridge**, you'd need:
```java
class JsonFileExporter { }
class JsonEmailExporter { }
class JsonDatabaseExporter { }
class XmlFileExporter { }
class XmlEmailExporter { }
class XmlDatabaseExporter { }
class CsvFileExporter { }
class CsvEmailExporter { }
class CsvDatabaseExporter { }
// 3 formats × 3 destinations = 9 classes!
```

Add a new format? Create 3 more classes. Add a new destination? Create 3 more classes.

**This is unmaintainable.**

### The Solution: Separate the Dimensions

Bridge separates:
- **Abstraction**: What to do (export to file, export to email)
- **Implementation**: How to format (JSON, XML, CSV)

```
Abstraction (Exporter)  ──uses──>  Implementation (Format)

FileExporter                       JsonFormat
EmailExporter                      XmlFormat
DatabaseExporter                   CsvFormat
```

Now: 3 exporters + 3 formats = **6 classes** (not 9!). Adding a new format only requires 1 new class, not 3.

---

## Bridge Pattern Structure

### Components

1. **Abstraction**: High-level control logic
2. **Refined Abstraction**: Extended abstraction with more specific behavior
3. **Implementation**: Interface for concrete implementations
4. **Concrete Implementation**: Actual implementation details

### UML Diagram

```
┌────────────────┐               ┌──────────────┐
│BookExporter    │──uses────────>│ExportFormat  │  (Implementation)
│ (Abstraction)  │               ├──────────────┤
├────────────────┤               │+ format()    │
│+ export()      │               └──────┬───────┘
└───────┬────────┘                      │
        │                         ┌─────┴─────┬─────────┐
 ┌──────┴───────┬──────────┐     │           │         │
 │              │          │  ┌──▼────┐  ┌───▼───┐ ┌──▼───┐
┌▼──────┐  ┌────▼───┐  ┌──▼──▼──┐    │  │  Xml  │ │ Csv  │
│File   │  │ Email  │  │Database│    │  │Format │ │Format│
│Exporter│  │Exporter│  │Exporter│    │  └───────┘ └──────┘
└────────┘  └────────┘  └────────┘  ┌─▼────────┐
                                    │   Json   │
                                    │  Format  │
                                    └──────────┘
```

---

## Implementing Bridge in Bibby

### Step 1: Implementation Interface (Format)

```java
package com.penrose.bibby.export.format;

public interface ExportFormat {
    String format(Book book);
    String formatList(List<Book> books);
}
```

### Step 2: Concrete Implementations

```java
public class JsonFormat implements ExportFormat {
    @Override
    public String format(Book book) {
        return String.format(
            "{\"id\":%d,\"title\":\"%s\",\"isbn\":\"%s\"}",
            book.getId().getValue(),
            book.getTitle().getValue(),
            book.getIsbn().getValue()
        );
    }

    @Override
    public String formatList(List<Book> books) {
        String items = books.stream()
            .map(this::format)
            .collect(Collectors.joining(","));
        return "[" + items + "]";
    }
}
```

```java
public class XmlFormat implements ExportFormat {
    @Override
    public String format(Book book) {
        return String.format(
            "<book><id>%d</id><title>%s</title><isbn>%s</isbn></book>",
            book.getId().getValue(),
            book.getTitle().getValue(),
            book.getIsbn().getValue()
        );
    }

    @Override
    public String formatList(List<Book> books) {
        String items = books.stream()
            .map(this::format)
            .collect(Collectors.joining());
        return "<books>" + items + "</books>";
    }
}
```

```java
public class CsvFormat implements ExportFormat {
    @Override
    public String format(Book book) {
        return String.format(
            "%d,%s,%s",
            book.getId().getValue(),
            book.getTitle().getValue(),
            book.getIsbn().getValue()
        );
    }

    @Override
    public String formatList(List<Book> books) {
        String header = "id,title,isbn\n";
        String rows = books.stream()
            .map(this::format)
            .collect(Collectors.joining("\n"));
        return header + rows;
    }
}
```

### Step 3: Abstraction (Exporter)

```java
package com.penrose.bibby.export;

public abstract class BookExporter {
    protected final ExportFormat format;  // Bridge to implementation

    protected BookExporter(ExportFormat format) {
        this.format = format;
    }

    public abstract void export(List<Book> books);
}
```

### Step 4: Refined Abstractions

```java
public class FileExporter extends BookExporter {
    private final Path outputPath;

    public FileExporter(ExportFormat format, Path outputPath) {
        super(format);
        this.outputPath = outputPath;
    }

    @Override
    public void export(List<Book> books) {
        String content = format.formatList(books);
        try {
            Files.writeString(outputPath, content);
            log.info("Exported {} books to {}", books.size(), outputPath);
        } catch (IOException e) {
            throw new ExportException("Failed to export to file", e);
        }
    }
}
```

```java
public class EmailExporter extends BookExporter {
    private final EmailService emailService;
    private final String recipient;

    public EmailExporter(ExportFormat format, EmailService emailService, String recipient) {
        super(format);
        this.emailService = emailService;
        this.recipient = recipient;
    }

    @Override
    public void export(List<Book> books) {
        String content = format.formatList(books);
        emailService.send(
            recipient,
            "Book Export",
            "Attached book list:\n\n" + content
        );
        log.info("Emailed {} books to {}", books.size(), recipient);
    }
}
```

```java
public class DatabaseExporter extends BookExporter {
    private final ExportRepository repository;

    public DatabaseExporter(ExportFormat format, ExportRepository repository) {
        super(format);
        this.repository = repository;
    }

    @Override
    public void export(List<Book> books) {
        String content = format.formatList(books);
        ExportRecord record = new ExportRecord(
            LocalDateTime.now(),
            content,
            books.size()
        );
        repository.save(record);
        log.info("Saved {} books to database export table", books.size());
    }
}
```

### Step 5: Usage - Mix and Match!

```java
List<Book> books = bookRepository.findAll();

// Export to file as JSON
BookExporter exporter = new FileExporter(
    new JsonFormat(),
    Path.of("books.json")
);
exporter.export(books);

// Export to email as XML
exporter = new EmailExporter(
    new XmlFormat(),
    emailService,
    "user@example.com"
);
exporter.export(books);

// Export to database as CSV
exporter = new DatabaseExporter(
    new CsvFormat(),
    exportRepository
);
exporter.export(books);

// Easy to add new combination!
exporter = new FileExporter(
    new CsvFormat(),  // Different format
    Path.of("books.csv")
);
exporter.export(books);
```

**Flexible!** Any exporter works with any format.

---

## When to Use Bridge

**Use Bridge when:**
1. You have two dimensions that vary independently
2. You want to avoid class explosion from combinations
3. You need to switch implementations at runtime
4. Changes in implementation shouldn't affect abstraction

**Real-world examples:**
- UI toolkit and platform (Windows, macOS, Linux)
- Database driver abstraction (JDBC)
- Graphics rendering (Direct3D, OpenGL, Vulkan)
- Your export system!

---

## Composite vs Bridge: Key Differences

| Aspect | Composite | Bridge |
|--------|-----------|--------|
| **Problem** | Part-whole hierarchies | Orthogonal dimensions |
| **Structure** | Tree structure | Abstraction ─uses→ Implementation |
| **Goal** | Uniform treatment of leaves and composites | Independent variation |
| **Example** | Bookcase → Shelf → Book | Exporter + Format |
| **Relationship** | "is composed of" | "uses" |

---

## Action Items

### 1. Implement Composite for Library Hierarchy (3-4 hours)

Refactor your Bibby domain model to use Composite:
- Create `LibraryComponent` interface
- Implement in `Book`, `Shelf`, `Bookcase`
- Add `getBookCount()`, `getAllBooks()`, `display()` methods
- Test recursive operations

### 2. Implement Bridge for Book Export (2-3 hours)

Create export functionality using Bridge:
- `ExportFormat` interface with `JsonFormat`, `XmlFormat`, `CsvFormat`
- `BookExporter` abstraction with `FileExporter`, `EmailExporter`
- Test all combinations (3 formats × 2 destinations = 6 combinations with only 5 classes!)

### 3. Add Search Across Hierarchy (2 hours)

Extend `LibraryComponent` with search:
```java
interface LibraryComponent {
    List<Book> searchByTitle(String query);
    List<Book> searchByAuthor(String authorName);
}
```

Implement recursively in composites, directly in leaves.

### 4. Visualize Your Library (1 hour)

Create a Spring Shell command that displays your library hierarchy:
```
@ShellMethod("Display library structure")
public void displayLibrary() {
    List<Bookcase> bookcases = bookcaseRepository.findAll();
    for (Bookcase bookcase : bookcases) {
        bookcase.display(0);
    }
}
```

---

## Key Takeaways

### 1. Composite Enables Uniform Treatment
- Same interface for leaves and composites
- Recursive operations are elegant
- Perfect for tree structures

### 2. Bridge Prevents Class Explosion
- Separate orthogonal dimensions
- N abstractions + M implementations = N + M classes (not N × M)
- Flexible runtime combination

### 3. Favor Composition Over Inheritance
- Both patterns use composition heavily
- More flexible than deep inheritance hierarchies

### 4. Recognize the Patterns in Your Domain
- Composite: Bookcases, shelves, books
- Bridge: Export destinations and formats
- Apply patterns where they fit naturally

### 5. Patterns Solve Specific Problems
- Don't force patterns where they don't fit
- Recognize the problem, apply the pattern
- Keep it simple when possible

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - Composite (p. 163), Bridge (p. 151)
- "Head First Design Patterns" - Great visual explanations

**Articles**:
- Refactoring Guru: "Composite Pattern"
- Refactoring Guru: "Bridge Pattern"

**Code Examples**:
- Java Swing: Component hierarchy (Composite)
- JDBC: DriverManager (Bridge)

---

## Mentor's Notes

**Composite** is intuitive once you see it - it matches natural hierarchies in your domain. If you have a tree structure, Composite is your friend.

**Bridge** is subtler but powerful. When you find yourself creating tons of subclasses for every combination of features, step back and ask: "Are there two independent dimensions here?" If yes, use Bridge.

Both patterns demonstrate the power of **composition over inheritance**. You'll use these patterns more than you think!

Next: **Strategy** and **Template Method** patterns - two ways to encapsulate algorithms!

---

**Section 13 Complete** | **Next**: Section 14 - Strategy & Template Method Patterns
