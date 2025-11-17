# SECTION 14: STRATEGY & TEMPLATE METHOD PATTERNS

## Behavioral Patterns for Algorithm Encapsulation

Welcome to Section 14! We're entering the **behavioral patterns** category of the Gang of Four. Behavioral patterns are all about algorithms, responsibilities, and communication between objects.

In this section, we'll explore **Strategy** and **Template Method** - two patterns that encapsulate algorithms but in different ways. One uses **composition** (Strategy), the other uses **inheritance** (Template Method).

Both are incredibly useful in Bibby for sorting books, validating input, importing data, and more.

---

## The Strategy Pattern

### What is the Strategy Pattern?

**Definition**: Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from clients that use it.

**Key Insight**: Instead of hardcoding an algorithm in a class, inject it as a dependency. This allows you to swap algorithms at runtime without changing the client code.

### The Problem: Hardcoded Algorithms

Suppose you want to sort books by different criteria:

**Without Strategy** (hardcoded conditionals):
```java
public class BookService {
    public List<Book> sortBooks(List<Book> books, String sortBy) {
        if (sortBy.equals("title")) {
            return books.stream()
                .sorted(Comparator.comparing(Book::getTitle))
                .collect(Collectors.toList());
        } else if (sortBy.equals("author")) {
            return books.stream()
                .sorted(Comparator.comparing(this::getFirstAuthorName))
                .collect(Collectors.toList());
        } else if (sortBy.equals("year")) {
            return books.stream()
                .sorted(Comparator.comparing(Book::getPublicationYear))
                .collect(Collectors.toList());
        } else if (sortBy.equals("popularity")) {
            return books.stream()
                .sorted(Comparator.comparing(Book::getCheckoutCount).reversed())
                .collect(Collectors.toList());
        }
        throw new IllegalArgumentException("Unknown sort: " + sortBy);
    }
}
```

**Problems**:
1. **Violates Open/Closed Principle**: Must modify code to add new sort
2. **Hard to test**: Can't test sorting strategies independently
3. **Not extensible**: Clients can't provide custom sorts
4. **Conditional logic**: Gets messy as you add more sorts

### The Solution: Strategy Objects

With Strategy, each sort is a separate class:

```java
public interface BookSortStrategy {
    List<Book> sort(List<Book> books);
}

public class SortByTitle implements BookSortStrategy { ... }
public class SortByAuthor implements BookSortStrategy { ... }
public class SortByYear implements BookSortStrategy { ... }
```

Client code injects the strategy:
```java
BookList bookList = new BookList();
bookList.setSortStrategy(new SortByTitle());
List<Book> sorted = bookList.getSortedBooks(books);
```

**Benefits**: Extensible, testable, clean.

---

## Strategy Pattern Structure

### Components

1. **Strategy Interface**: Common interface for all algorithms
2. **Concrete Strategies**: Specific algorithm implementations
3. **Context**: Uses a strategy (holds a reference to it)

### UML Diagram

```
┌──────────────────┐
│  BookSortStrategy│  (Strategy Interface)
├──────────────────┤
│ + sort(books)    │
└────────┬─────────┘
         │
    ┌────┴────┬─────────┬───────────┐
    │         │         │           │
┌───▼──┐  ┌──▼───┐  ┌──▼────┐  ┌───▼────────┐
│Title │  │Author│  │ Year  │  │Checkout    │
│Sort  │  │Sort  │  │ Sort  │  │Count Sort  │
└──────┘  └──────┘  └───────┘  └────────────┘

┌──────────────┐
│  BookList    │  (Context)
├──────────────┤
│- strategy    │──uses──> BookSortStrategy
│+ setStrategy│
│+ getSorted   │
└──────────────┘
```

---

## Implementing Strategy in Bibby

### Step 1: Strategy Interface

```java
package com.penrose.bibby.library.book.sort;

public interface BookSortStrategy {
    /**
     * Sort the given list of books according to this strategy
     */
    List<Book> sort(List<Book> books);
}
```

### Step 2: Concrete Strategies

```java
package com.penrose.bibby.library.book.sort;

public class SortByTitle implements BookSortStrategy {
    @Override
    public List<Book> sort(List<Book> books) {
        return books.stream()
            .sorted(Comparator.comparing(book -> book.getTitle().getValue()))
            .collect(Collectors.toList());
    }
}
```

```java
public class SortByAuthor implements BookSortStrategy {
    @Override
    public List<Book> sort(List<Book> books) {
        return books.stream()
            .sorted(Comparator.comparing(this::getFirstAuthorName))
            .collect(Collectors.toList());
    }

    private String getFirstAuthorName(Book book) {
        return book.getAuthors().stream()
            .findFirst()
            .map(Author::getName)
            .orElse("");
    }
}
```

```java
public class SortByPublicationYear implements BookSortStrategy {
    @Override
    public List<Book> sort(List<Book> books) {
        return books.stream()
            .sorted(Comparator.comparing(Book::getPublicationYear).reversed())
            .collect(Collectors.toList());
    }
}
```

```java
public class SortByCheckoutCount implements BookSortStrategy {
    @Override
    public List<Book> sort(List<Book> books) {
        return books.stream()
            .sorted(Comparator.comparing(Book::getCheckoutCount).reversed())
            .collect(Collectors.toList());
    }
}
```

### Step 3: Context Class

```java
package com.penrose.bibby.library.book;

public class BookList {
    private BookSortStrategy sortStrategy;

    public void setSortStrategy(BookSortStrategy strategy) {
        this.sortStrategy = strategy;
    }

    public List<Book> getSortedBooks(List<Book> books) {
        if (sortStrategy == null) {
            return new ArrayList<>(books);  // No sorting
        }
        return sortStrategy.sort(books);
    }
}
```

### Step 4: Usage

```java
List<Book> books = bookRepository.findAll();
BookList bookList = new BookList();

// Sort by title
bookList.setSortStrategy(new SortByTitle());
List<Book> sorted = bookList.getSortedBooks(books);
sorted.forEach(book -> System.out.println(book.getTitle()));

// Change strategy at runtime
bookList.setSortStrategy(new SortByCheckoutCount());
sorted = bookList.getSortedBooks(books);
sorted.forEach(book ->
    System.out.println(book.getTitle() + " - " + book.getCheckoutCount() + " checkouts")
);

// Custom strategy on the fly (Java 8 lambda!)
bookList.setSortStrategy(booksToSort ->
    booksToSort.stream()
        .sorted(Comparator.comparing(Book::getIsbn))
        .collect(Collectors.toList())
);
sorted = bookList.getSortedBooks(books);
```

**Powerful!** Strategies can be swapped at runtime, even using lambdas.

---

## Strategy in Spring Shell

Use Strategy for user-selectable options:

```java
@ShellComponent
public class BookCommands {

    @ShellMethod("List books sorted by criteria")
    public String listBooks(
        @ShellOption(defaultValue = "title") String sortBy
    ) {
        List<Book> books = bookRepository.findAll();

        BookSortStrategy strategy = switch (sortBy.toLowerCase()) {
            case "title" -> new SortByTitle();
            case "author" -> new SortByAuthor();
            case "year" -> new SortByPublicationYear();
            case "popularity" -> new SortByCheckoutCount();
            default -> throw new IllegalArgumentException("Unknown sort: " + sortBy);
        };

        BookList bookList = new BookList();
        bookList.setSortStrategy(strategy);
        List<Book> sorted = bookList.getSortedBooks(books);

        return sorted.stream()
            .map(Book::getTitle)
            .collect(Collectors.joining("\n"));
    }
}
```

Usage:
```
bibby:> list-books --sort-by title
bibby:> list-books --sort-by popularity
```

---

## When to Use Strategy

**Use Strategy when:**
1. You have multiple algorithms for the same task
2. You want to eliminate conditional statements
3. Algorithms should be interchangeable at runtime
4. Clients should choose which algorithm to use

**Real-world examples:**
- Sorting algorithms
- Compression algorithms (ZIP, GZIP, BZIP2)
- Payment methods (Credit Card, PayPal, Bitcoin)
- Validation strategies

---

## The Template Method Pattern

### What is the Template Method Pattern?

**Definition**: Define the skeleton of an algorithm in a method, deferring some steps to subclasses. Template Method lets subclasses redefine certain steps without changing the algorithm's structure.

**Key Insight**: Put the **invariant parts** of an algorithm in a base class, and let subclasses implement the **variant parts**.

### The Problem: Duplicated Algorithm Structure

Suppose you want to import books from different sources (CSV, JSON, XML). The process is always:
1. Parse source
2. Validate data
3. Create book
4. Save book
5. Log result

Steps 2, 4, 5 are the same. Only steps 1 and 3 vary.

**Without Template Method**:
```java
public class CsvBookImporter {
    public void importBook(String csv) {
        // Parse (CSV-specific)
        String[] parts = csv.split(",");

        // Validate (duplicated!)
        if (parts[0].isBlank()) throw new ValidationException();

        // Create book (CSV-specific)
        Book book = new Book(parts[0], parts[1]);

        // Save (duplicated!)
        bookRepository.save(book);

        // Log (duplicated!)
        log.info("Imported book: {}", book.getId());
    }
}

public class JsonBookImporter {
    public void importBook(String json) {
        // Parse (JSON-specific)
        JsonNode node = objectMapper.readTree(json);

        // Validate (duplicated!)
        if (node.get("title").asText().isBlank()) throw new ValidationException();

        // Create book (JSON-specific)
        Book book = new Book(node.get("title").asText(), node.get("isbn").asText());

        // Save (duplicated!)
        bookRepository.save(book);

        // Log (duplicated!)
        log.info("Imported book: {}", book.getId());
    }
}
```

**Duplication!** Validate, save, and log are identical.

### The Solution: Template Method

Extract common steps into a base class with a template method:

```java
public abstract class BookImporter {
    // Template method - defines algorithm skeleton
    public final ImportResult importBook(String source) {
        BookData data = parseSource(source);       // Step 1: Varies
        if (!validate(data)) return failure();     // Step 2: Common
        Book book = createBook(data);               // Step 3: Varies
        bookRepository.save(book);                  // Step 4: Common
        log.info("Imported: {}", book.getId());    // Step 5: Common
        return success(book);
    }

    protected abstract BookData parseSource(String source);  // Subclass implements
    protected abstract Book createBook(BookData data);       // Subclass implements

    private boolean validate(BookData data) { ... }          // Common logic
}
```

**Clean!** No duplication. Subclasses only implement what varies.

---

## Template Method Pattern Structure

### Components

1. **Abstract Class**: Defines template method and common steps
2. **Template Method**: `final` method that calls other methods
3. **Primitive Operations**: `abstract` methods subclasses must implement
4. **Hook Methods**: Optional methods subclasses can override

### UML Diagram

```
┌───────────────────────┐
│   BookImporter        │ (Abstract Class)
├───────────────────────┤
│+ importBook()         │ ← Template Method (final)
│# parseSource()        │ ← Abstract (subclass implements)
│# createBook()         │ ← Abstract (subclass implements)
│# afterImport()        │ ← Hook (optional override)
│- validate()           │ ← Common logic (private)
└───────────┬───────────┘
            │
     ┌──────┴──────┬────────────┐
     │             │            │
┌────▼──────┐  ┌──▼──────┐  ┌──▼───────┐
│CsvImporter│  │JsonImporter│ │XmlImporter│
└───────────┘  └───────────┘  └──────────┘
```

---

## Implementing Template Method in Bibby

### Step 1: Data Transfer Object

```java
package com.penrose.bibby.library.book.import;

public class BookData {
    private final String title;
    private final String isbn;
    private final List<String> authors;

    // Constructor, getters
}
```

### Step 2: Abstract Template Class

```java
package com.penrose.bibby.library.book.import;

public abstract class BookImporter {
    protected final BookRepository bookRepository;

    public BookImporter(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Template method - defines the algorithm structure
     * FINAL so subclasses cannot override the algorithm flow
     */
    public final ImportResult importBook(String source) {
        log.info("Starting book import from source");

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

        // Step 5: Post-processing hook (optional)
        afterImport(book);

        log.info("Successfully imported book: {}", book.getId());
        return ImportResult.success(book.getId());
    }

    /**
     * Primitive operation - must be implemented by subclasses
     */
    protected abstract BookData parseSource(String source);

    /**
     * Primitive operation - must be implemented by subclasses
     */
    protected abstract Book createBook(BookData data);

    /**
     * Hook method - subclasses can override but don't have to
     */
    protected void afterImport(Book book) {
        // Default: do nothing
    }

    /**
     * Common validation logic
     */
    private boolean validate(BookData data) {
        if (data.getTitle() == null || data.getTitle().isBlank()) {
            log.warn("Validation failed: title is blank");
            return false;
        }
        if (data.getIsbn() == null || data.getIsbn().isBlank()) {
            log.warn("Validation failed: ISBN is blank");
            return false;
        }
        return true;
    }
}
```

### Step 3: Concrete Implementations

```java
public class CsvBookImporter extends BookImporter {

    public CsvBookImporter(BookRepository bookRepository) {
        super(bookRepository);
    }

    @Override
    protected BookData parseSource(String csv) {
        String[] parts = csv.split(",");
        String title = parts[0].trim();
        String isbn = parts[1].trim();
        List<String> authors = parts.length > 2
            ? List.of(parts[2].split(";"))
            : List.of();

        return new BookData(title, isbn, authors);
    }

    @Override
    protected Book createBook(BookData data) {
        return Book.builder()
            .id(BookId.generate())
            .title(new Title(data.getTitle()))
            .isbn(new ISBN(data.getIsbn()))
            .build();
    }
}
```

```java
public class JsonBookImporter extends BookImporter {
    private final ObjectMapper objectMapper;

    public JsonBookImporter(BookRepository bookRepository, ObjectMapper objectMapper) {
        super(bookRepository);
        this.objectMapper = objectMapper;
    }

    @Override
    protected BookData parseSource(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String title = node.get("title").asText();
            String isbn = node.get("isbn").asText();
            List<String> authors = new ArrayList<>();
            node.get("authors").forEach(a -> authors.add(a.asText()));

            return new BookData(title, isbn, authors);
        } catch (JsonProcessingException e) {
            throw new ImportException("Failed to parse JSON", e);
        }
    }

    @Override
    protected Book createBook(BookData data) {
        return Book.builder()
            .id(BookId.generate())
            .title(new Title(data.getTitle()))
            .isbn(new ISBN(data.getIsbn()))
            .build();
    }

    @Override
    protected void afterImport(Book book) {
        // Publish event after JSON import
        eventPublisher.publish(new BookImportedEvent(book.getId()));
    }
}
```

### Step 4: Usage

```java
// CSV import
BookImporter importer = new CsvBookImporter(bookRepository);
ImportResult result = importer.importBook("Clean Code,978-0132350884,Robert Martin");

if (result.isSuccess()) {
    System.out.println("Imported book: " + result.getBookId());
}

// JSON import
importer = new JsonBookImporter(bookRepository, objectMapper);
String json = "{\"title\":\"Refactoring\",\"isbn\":\"978-0201485677\",\"authors\":[\"Martin Fowler\"]}";
result = importer.importBook(json);
```

**Elegant!** Common logic is centralized. Subclasses only implement what varies.

---

## Strategy vs Template Method

Both patterns encapsulate algorithms but in different ways:

| Aspect | Strategy | Template Method |
|--------|----------|-----------------|
| **Composition vs Inheritance** | Composition (inject strategy) | Inheritance (extend base class) |
| **Runtime vs Compile-time** | Change at runtime | Fixed at compile-time |
| **Flexibility** | Very flexible (swap strategies) | Less flexible (subclass bound) |
| **Encapsulation** | Algorithms fully encapsulated | Algorithm structure visible |
| **When to Use** | Interchangeable algorithms | Common algorithm structure with variant steps |

**Rule of Thumb**: Prefer Strategy (composition) over Template Method (inheritance) when possible. Composition is more flexible.

**When Template Method Shines**: When you have a well-defined algorithm structure that rarely changes, and you want to enforce the flow.

---

## Action Items

### 1. Implement Book Sorting with Strategy (2-3 hours)

Create strategies for sorting:
- `SortByTitle`, `SortByAuthor`, `SortByPublicationYear`, `SortByCheckoutCount`
- `BookList` context class
- Spring Shell command with `--sort-by` option
- Test all strategies

### 2. Implement Book Import with Template Method (3-4 hours)

Create importers:
- `BookImporter` abstract base class with template method
- `CsvBookImporter`, `JsonBookImporter`
- Validation logic in base class
- Test both importers

### 3. Add Validation Strategy (2 hours)

Use Strategy for different validation rules:
```java
interface ValidationStrategy {
    ValidationResult validate(Book book);
}

class StrictValidation implements ValidationStrategy { ... }
class RelaxedValidation implements ValidationStrategy { ... }
```

### 4. Refactor Existing Code (2 hours)

Find places in your code where you have:
- Multiple conditionals for different algorithms → Replace with Strategy
- Duplicated code with slight variations → Replace with Template Method

---

## Key Takeaways

### 1. Strategy Encapsulates Algorithms as Objects
- Each algorithm is a separate class
- Algorithms are interchangeable
- Runtime selection

### 2. Template Method Defines Algorithm Structure
- Base class defines the flow
- Subclasses implement variant steps
- Compile-time binding

### 3. Composition Over Inheritance
- Strategy uses composition (more flexible)
- Template Method uses inheritance (more rigid)
- Prefer Strategy when possible

### 4. Eliminate Conditionals
- Both patterns remove `if/else` for algorithm selection
- Cleaner, more maintainable code
- Open/Closed Principle: open for extension, closed for modification

### 5. Java 8 Lambdas Work Great with Strategy
- Strategy interface can be a functional interface
- Pass strategies as lambdas
- Very concise!

---

## Further Study

**Books**:
- "Design Patterns" by Gang of Four - Strategy (p. 315), Template Method (p. 325)
- "Head First Design Patterns" - Excellent examples

**Articles**:
- Refactoring Guru: "Strategy Pattern"
- Refactoring Guru: "Template Method Pattern"
- Baeldung: "Strategy Design Pattern in Java"

**Code Examples**:
- Spring Security: Authentication strategies
- Java `Comparator`: Strategy pattern!

---

## Mentor's Notes

**Strategy** is one of the most useful patterns you'll apply. Anytime you have an `if/else` or `switch` selecting between algorithms, consider Strategy.

**Template Method** is great when you have a clear, stable algorithm structure. Don't overuse it - inheritance can be inflexible.

In modern Java, Strategy often wins because lambdas make it so concise. But Template Method has its place for complex, multi-step processes.

Master both, and you'll write cleaner, more extensible code!

Next: **Observer** and **Command** patterns - reacting to events and encapsulating requests!

---

**Section 14 Complete** | **Next**: Section 15 - Observer & Command Patterns
