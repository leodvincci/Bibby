# Section 16: Records & Modern Java Features — Writing Less, Expressing More

**Estimated Reading Time:** 50 minutes
**Prerequisites:** Sections 13-15 (Lambdas, Streams, Optional)
**Applies To:** Your Bibby Library Management CLI Application

---

## Table of Contents

1. [What You'll Learn](#what-youll-learn)
2. [Modern Features Discovery](#modern-features-discovery)
3. [Records - Data Classes Done Right](#records---data-classes-done-right)
4. [Text Blocks - Readable Multi-Line Strings](#text-blocks---readable-multi-line-strings)
5. [Switch Expressions - No More Fallthrough](#switch-expressions---no-more-fallthrough)
6. [Pattern Matching for instanceof](#pattern-matching-for-instanceof)
7. [var - Type Inference](#var---type-inference)
8. [When NOT to Use Modern Features](#when-not-to-use-modern-features)
9. [Action Items](#action-items)
10. [Key Takeaways](#key-takeaways)

---

## What You'll Learn

By the end of this section, you'll understand:

- **Records** — Immutable data carriers (you're already using them!)
- **Text blocks** — Multi-line strings without escape hell (you're using these too!)
- **Switch expressions** — Return values, no fallthrough (opportunity!)
- **Pattern matching** — instanceof without casting (opportunity!)
- **var** — Local variable type inference (when to use, when not to)

**Most importantly**, you'll learn that modern features should **reduce boilerplate**, not add complexity.

---

## Modern Features Discovery

I audited your Bibby codebase for modern Java features (Java 14-17):

### ✅ Records (Java 14+) — Already Using!

**Count: 5 records**

```java
BookRequestDTO.java:3       → record BookRequestDTO(String title, String firstName, String lastName)
BookSummary.java:3          → record BookSummary(Long bookId, String title)
BookDetailView.java:3       → record BookDetailView(Long bookId, String title, String authors, ...)
ShelfSummary.java:3         → record ShelfSummary(Long shelfId, String label, long bookCount)
BookcaseDTO.java:3          → record BookcaseDTO(Long bookcaseId, String bookcaseLabel, int shelfCapacity)
```

**Verdict:** ✅ EXCELLENT! You're using records exactly where they belong—immutable DTOs and projections.

---

### ✅ Text Blocks (Java 15+) — Already Using!

**Count: 17 text blocks**

```java
BookCommands.java:138       → Multi-line shelf recommendation message
BookCommands.java:292       → Search results display
BookCommands.java:480       → "Book already checked out" message
BookCommands.java:491       → Checkout confirmation
BookCommands.java:558       → Check-in confirmation
ShelfRepository.java:15     → JPQL query with text block
```

**Verdict:** ✅ EXCELLENT! Text blocks make your CLI messages and SQL queries much more readable.

---

### ❌ Switch Expressions (Java 14+) — NOT Using

**Count: 0 switch statements/expressions**

Your code uses `if-else` chains instead. This is fine, but switch expressions would be cleaner in some places.

**Opportunity:** Menu option handling, status enum matching.

---

### ❌ Pattern Matching for instanceof (Java 16+) — NOT Using

**Count: 0 instanceof checks**

No instanceof in your code, so no opportunity here.

---

### ❌ var (Java 10+) — NOT Using

**Count: 0 var declarations**

You explicitly type every variable. This is fine for clarity, but `var` would reduce verbosity in some places.

**Opportunity:** Long generic types, obvious initializations.

---

## Records - Data Classes Done Right

### What Is a Record?

A **record** is a special kind of class that's **transparently immutable** and designed to **carry data**.

**Old Way (Pre-Java 14):**

```java
// 30+ lines of boilerplate!
public final class BookSummary {
    private final Long bookId;
    private final String title;

    public BookSummary(Long bookId, String title) {
        this.bookId = bookId;
        this.title = title;
    }

    public Long bookId() { return bookId; }
    public String title() { return title; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BookSummary other)) return false;
        return Objects.equals(bookId, other.bookId) &&
               Objects.equals(title, other.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId, title);
    }

    @Override
    public String toString() {
        return "BookSummary[bookId=" + bookId + ", title=" + title + "]";
    }
}
```

**New Way (Java 14+):**

```java
// 1 line!
public record BookSummary(Long bookId, String title) {}
```

**The compiler generates:**
- Private final fields
- Public constructor
- Accessor methods (no `get` prefix!)
- `equals()`, `hashCode()`, `toString()`
- Record is implicitly final (can't be extended)

---

### Your Records: Already Perfect!

**BookRequestDTO.java:**
```java
public record BookRequestDTO(String title, String firstName, String lastName) {}
```

✅ **Perfect use case!** DTOs (Data Transfer Objects) should be immutable records.

**Why records are better here:**
1. **Immutability** — Can't accidentally modify DTO after creation
2. **Thread-safe** — Safe to pass between threads
3. **No boilerplate** — Compiler generates all the methods
4. **Clear intent** — "This is just data, nothing else"

---

**BookSummary.java:**
```java
public record BookSummary(Long bookId, String title) {
}
```

✅ **Perfect use case!** Projection interfaces returned by Spring Data queries.

**Spring Data JPQL projection (ShelfRepository.java:15-26):**
```java
@Query("""
    SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
        s.shelfId,
        s.shelfLabel,
        COUNT(b.bookId)
    )
    FROM ShelfEntity s
    LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
    WHERE s.bookcaseId = :bookcaseId
    GROUP BY s.shelfId, s.shelfLabel
    ORDER BY s.shelfPosition ASC
    """)
List<ShelfSummary> findShelfSummariesByBookcaseId(@Param("bookcaseId") Long bookcaseId);
```

✅ **Brilliant!** The `new ShelfSummary(...)` constructor in JPQL works because records have canonical constructors.

---

**BookDetailView.java:**
```java
public record BookDetailView(Long bookId, String title, String authors, String bookcaseLabel, String shelfLabel, String bookStatus) {
}
```

✅ **Perfect use case!** View models for displaying data to users.

**Benefits:**
- **Immutable** — View data can't be accidentally changed
- **Self-documenting** — Clear what data is being displayed
- **Easy to test** — Can construct test data with `new BookDetailView(...)`

---

### When to Use Records

✅ **Use records for:**
1. **DTOs** (Data Transfer Objects)
   Example: `BookRequestDTO`

2. **Value objects**
   Example: `Money(BigDecimal amount, Currency currency)`

3. **Query projections**
   Example: `BookSummary`, `ShelfSummary`

4. **Return values with multiple components**
   Example: `QueryResult(List<Book> books, long totalCount)`

5. **Configuration data**
   Example: `DatabaseConfig(String url, String username, String password)`

❌ **Don't use records for:**
1. **Entities** (@Entity classes)
   Reason: JPA needs setters, default constructor, mutable state

2. **Builders**
   Reason: Builders accumulate state, records are immutable

3. **Classes with complex behavior**
   Reason: Records are for data, not behavior

4. **Classes that need inheritance**
   Reason: Records are implicitly final

---

### Customizing Records

Records can have:
- Custom constructors (compact and canonical)
- Instance methods
- Static methods and fields
- Validation

**Example: Validated Record**

```java
public record BookRequestDTO(String title, String firstName, String lastName) {
    // Compact constructor for validation
    public BookRequestDTO {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be blank");
        }
    }

    // Instance method
    public String fullAuthorName() {
        return firstName + " " + lastName;
    }
}
```

**Compact Constructor:**
- No parameter list (uses record components)
- Runs before field initialization
- Perfect for validation

**Canonical Constructor (alternative):**
```java
public record BookRequestDTO(String title, String firstName, String lastName) {
    // Explicit parameter list
    public BookRequestDTO(String title, String firstName, String lastName) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        this.title = title;  // Must explicitly assign
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
```

---

### Records with Default Values

```java
public record ShelfSummary(Long shelfId, String label, long bookCount) {
    // Provide default for bookCount
    public ShelfSummary(Long shelfId, String label) {
        this(shelfId, label, 0L);  // Delegate to canonical constructor
    }
}

// Usage:
ShelfSummary empty = new ShelfSummary(1L, "Shelf A");  // bookCount = 0
ShelfSummary full = new ShelfSummary(1L, "Shelf A", 42L);  // bookCount = 42
```

---

## Text Blocks - Readable Multi-Line Strings

### What Are Text Blocks?

**Text blocks** (Java 15+) are multi-line string literals that preserve formatting without escape characters.

### Your Code: Already Using Them!

**BookCommands.java:138-143 (Shelf Recommendation):**

```java
// ✅ Current: Text block (clean and readable!)
System.out.println(
    """
    \u001B[36m</>\033[0m: Recommended Shelf → \u001B[33mD-48\033[0m: Programming & Engineering.
         Fits best near "\u001B[31mThe Pragmatic Programmer\033[0m" and "\u001B[31mRefactoring\033[0m".
         These titles share themes of maintainable code and engineering craftsmanship.
         Placing them together makes your shelf flow logically by topic.
    """
);

// ❌ Without text blocks (escape hell!):
System.out.println(
    "\u001B[36m</>\033[0m: Recommended Shelf → \u001B[33mD-48\033[0m: Programming & Engineering.\n" +
    "     Fits best near \"\u001B[31mThe Pragmatic Programmer\033[0m\" and \"\u001B[31mRefactoring\033[0m\".\n" +
    "     These titles share themes of maintainable code and engineering craftsmanship.\n" +
    "     Placing them together makes your shelf flow logically by topic.\n"
);
```

**Benefits:**
- ✅ No `\n` escape sequences
- ✅ No `+` concatenation
- ✅ Preserves indentation
- ✅ Easier to read and maintain

---

**ShelfRepository.java:15-26 (JPQL Query):**

```java
// ✅ Current: Text block for query (beautiful!)
@Query("""
    SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
        s.shelfId,
        s.shelfLabel,
        COUNT(b.bookId)
    )
    FROM ShelfEntity s
    LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
    WHERE s.bookcaseId = :bookcaseId
    GROUP BY s.shelfId, s.shelfLabel
    ORDER BY s.shelfPosition ASC
    """)

// ❌ Without text blocks (nightmare!):
@Query("SELECT new com.penrose.bibby.library.shelf.ShelfSummary(" +
       "s.shelfId, s.shelfLabel, COUNT(b.bookId)) " +
       "FROM ShelfEntity s " +
       "LEFT JOIN BookEntity b ON b.shelfId = s.shelfId " +
       "WHERE s.bookcaseId = :bookcaseId " +
       "GROUP BY s.shelfId, s.shelfLabel " +
       "ORDER BY s.shelfPosition ASC")
```

**Benefits:**
- ✅ SQL is readable (looks like SQL!)
- ✅ Easy to copy/paste into SQL editor for testing
- ✅ No string concatenation errors

---

### Text Block Features

**1. Incidental Whitespace Removal:**

```java
String html = """
    <html>
        <body>
            <p>Hello</p>
        </body>
    </html>""";

// Result (indentation stripped to leftmost content):
// <html>
//     <body>
//         <p>Hello</p>
//     </body>
// </html>
```

The compiler removes common leading whitespace.

---

**2. Escape Sequences Still Work:**

```java
String colored = """
    \u001B[32mGreen text\u001B[0m
    \u001B[31mRed text\u001B[0m
    """;
```

Your code uses this for ANSI color codes!

---

**3. String.formatted() with Text Blocks:**

```java
// ✅ Your code (BookCommands.java:491):
System.out.println(String.format("""
    \n\u001B[32mConfirm Checkout\n\u001B[0m
            \033[31mTitle\u001B[0m %s
            \033[31mAuthor/s\u001B[0m %s

            \033[31mStatus %s

            \033[31mBookcase\u001B[0m %s
            \033[31mShelf\u001B[0m %s
    """, book.getTitle(), authors, book.getBookStatus(), bookcaseName, shelfName));

// Alternative with .formatted() (Java 15+):
String message = """
    \n\u001B[32mConfirm Checkout\n\u001B[0m
            \033[31mTitle\u001B[0m %s
            \033[31mAuthor/s\u001B[0m %s

            \033[31mStatus %s

            \033[31mBookcase\u001B[0m %s
            \033[31mShelf\u001B[0m %s
    """.formatted(book.getTitle(), authors, book.getBookStatus(), bookcaseName, shelfName);

System.out.println(message);
```

---

### When to Use Text Blocks

✅ **Use text blocks for:**
1. **Multi-line strings** (messages, error text)
2. **SQL/JPQL queries**
3. **JSON/XML/HTML templates**
4. **Regular expressions** (avoid double-escaping)
5. **File content** (scripts, config templates)

❌ **Don't use text blocks for:**
1. **Single-line strings** (overkill)
2. **User input** (they type strings, not text blocks)
3. **When you need trailing newline** (text blocks strip trailing whitespace)

---

## Switch Expressions - No More Fallthrough

### The Problem with Traditional Switch

**Traditional switch statement:**

```java
// ❌ Verbose, error-prone (easy to forget break)
String status;
switch (bookStatus) {
    case "AVAILABLE":
        status = "✅ Available";
        break;
    case "CHECKED_OUT":
        status = "📤 Checked Out";
        break;
    case "RESERVED":
        status = "🔒 Reserved";
        break;
    default:
        status = "❓ Unknown";
        break;
}
System.out.println(status);
```

**Issues:**
- Must remember `break` (forgetting causes fallthrough bugs)
- Can't return a value directly
- Verbose (lots of `break` statements)

---

### Switch Expressions (Java 14+)

**Switch expression:**

```java
// ✅ Concise, safe (no fallthrough)
String status = switch (bookStatus) {
    case "AVAILABLE" -> "✅ Available";
    case "CHECKED_OUT" -> "📤 Checked Out";
    case "RESERVED" -> "🔒 Reserved";
    default -> "❓ Unknown";
};
System.out.println(status);
```

**Benefits:**
- ✅ No `break` needed
- ✅ Returns a value
- ✅ Compiler enforces exhaustiveness (must cover all cases)
- ✅ Can't forget `break` (arrow syntax prevents fallthrough)

---

### Your Code Opportunity: Status Enum

**Current (if you had status matching):**

```java
String displayStatus;
if ("AVAILABLE".equals(book.getBookStatus())) {
    displayStatus = "Available for checkout";
} else if ("CHECKED_OUT".equals(book.getBookStatus())) {
    displayStatus = "Currently checked out";
} else {
    displayStatus = "Unknown status";
}
```

**With switch expression:**

```java
String displayStatus = switch (book.getBookStatus()) {
    case "AVAILABLE" -> "Available for checkout";
    case "CHECKED_OUT" -> "Currently checked out";
    default -> "Unknown status";
};
```

---

### Multi-Statement Cases

```java
String message = switch (bookStatus) {
    case "AVAILABLE" -> "Available for checkout";

    case "CHECKED_OUT" -> {
        // Multiple statements need yield
        String dueDate = calculateDueDate();
        yield "Checked out until " + dueDate;
    }

    case "RESERVED" -> "Reserved for pickup";
    default -> "Unknown status";
};
```

**yield** — Returns a value from a block (like `return` but for switch expressions).

---

### Pattern Matching in Switch (Java 17 Preview, 21 Stable)

```java
// Future: Switch on types with pattern matching
String formatted = switch (obj) {
    case BookEntity b -> "Book: " + b.getTitle();
    case AuthorEntity a -> "Author: " + a.getFullName();
    case null -> "No data";
    default -> "Unknown type: " + obj.getClass();
};
```

You don't need this yet (no instanceof in your code), but it's coming!

---

### Opportunity: Menu Option Handling

**Your Code: BookCommands.java:203-209** (Using if-else for menu):

```java
// ❌ Current: if-else chain
String searchType = result.getContext().get("searchType", String.class);
if (searchType.equalsIgnoreCase("author")){
    searchByAuthor();
}else if(searchType.equalsIgnoreCase("title")){
    searchByTitle();
}

// ✅ With switch expression (cleaner):
String searchType = result.getContext().get("searchType", String.class);
switch (searchType.toLowerCase()) {
    case "author" -> searchByAuthor();
    case "title" -> searchByTitle();
    case "genre" -> searchByGenre();
    case "shelf" -> searchByShelf();
    default -> System.out.println("Unknown search type");
}
```

**Note:** This is a **statement** switch (doesn't return a value), which is fine when you just need to call different methods.

---

## Pattern Matching for instanceof

### The Old Way

```java
// ❌ Pre-Java 16: Check type, then cast
if (entity instanceof BookEntity) {
    BookEntity book = (BookEntity) entity;  // ← Explicit cast
    System.out.println(book.getTitle());
}
```

**Problem:** Type check and cast are separate (verbose, error-prone).

---

### The New Way (Java 16+)

```java
// ✅ Java 16+: Pattern matching instanceof
if (entity instanceof BookEntity book) {  // ← Declares variable 'book'
    System.out.println(book.getTitle());  // ← No cast needed!
}
// 'book' is scoped to the if block
```

**Benefits:**
- ✅ No explicit cast
- ✅ Variable scoped to the block
- ✅ Less verbose

---

### Your Code: No Opportunity

You don't have any `instanceof` checks in Bibby, so this doesn't apply.

**When you'd use it:**
- Polymorphic collections (`List<Object>`)
- Handling different exception types
- Visitor patterns

**Example (if you had mixed entities):**

```java
List<Object> entities = repository.findAll();  // Mix of Book, Author, Shelf
for (Object entity : entities) {
    if (entity instanceof BookEntity book) {
        System.out.println("Book: " + book.getTitle());
    } else if (entity instanceof AuthorEntity author) {
        System.out.println("Author: " + author.getFullName());
    } else if (entity instanceof ShelfEntity shelf) {
        System.out.println("Shelf: " + shelf.getShelfLabel());
    }
}
```

---

## var - Type Inference

### What Is var?

`var` (Java 10+) lets the compiler **infer** the type of a local variable from the initializer.

```java
// ❌ Explicit type (verbose)
Map<String, List<BookEntity>> booksByGenre = new HashMap<>();

// ✅ var (compiler infers type)
var booksByGenre = new HashMap<String, List<BookEntity>>();
```

**Important:** `var` is **NOT** dynamic typing (like JavaScript). The type is still static, just inferred at compile-time.

---

### Your Code: Zero var Usage

You explicitly type every variable:

```java
// Your code (explicit types everywhere)
ComponentFlow flow = componentFlowBuilder.clone()...
String title = result.getContext().get("title", String.class);
Optional<BookEntity> bookEntity = bookService.findBookById(id);
List<ShelfSummary> shelfSummaries = shelfService.getShelfSummariesForBookcase(id);
```

**This is fine!** Explicit types are clear. But `var` can reduce verbosity in some places.

---

### When to Use var

✅ **Use var when the type is obvious:**

```java
// ✅ Good: Type is obvious from right side
var title = "Harry Potter";  // Obviously String
var count = 42;  // Obviously int
var books = new ArrayList<BookEntity>();  // Obviously ArrayList<BookEntity>
var optional = Optional.of(book);  // Obviously Optional<BookEntity>

// ✅ Good: Reduces verbosity for long generic types
var booksByAuthor = new HashMap<AuthorEntity, List<BookEntity>>();
// vs
HashMap<AuthorEntity, List<BookEntity>> booksByAuthor = new HashMap<>();

// ✅ Good: Builder patterns
var request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com"))
    .build();
```

---

❌ **Don't use var when the type is unclear:**

```java
// ❌ Bad: What type is this?
var result = getData();  // ← Could be anything!

// ✅ Better: Explicit type documents intent
BookEntity result = getData();

// ❌ Bad: Loses precision
var number = 100;  // ← int, but did you want long?

// ✅ Better: Explicit when precision matters
long number = 100L;

// ❌ Bad: Generic wildcards
var list = List.of();  // ← List<Object>, probably not what you want

// ✅ Better: Explicit generic type
List<String> list = List.of();
```

---

### Opportunities in Your Code

**BookCommands.java:98-105:**

```java
// ❌ Current: Long, repetitive types
ComponentFlow flow = componentFlowBuilder.clone()
        .withStringInput("title")
        .name("Book Title:_")
        .and()
        .withStringInput("author_count")
        .name("Number of Authors:_")
        .and()
        .build();

// ✅ With var (less verbose):
var flow = componentFlowBuilder.clone()
        .withStringInput("title")
        .name("Book Title:_")
        .and()
        .withStringInput("author_count")
        .name("Number of Authors:_")
        .and()
        .build();
```

**Benefit:** The builder pattern makes it obvious `flow` is a `ComponentFlow`.

---

**BookcaseCommands.java:64:**

```java
// ❌ Current: Long generic type repeated
Map<String, String> options = new LinkedHashMap<>();
List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();

// ✅ With var:
var options = new LinkedHashMap<String, String>();
var bookcaseEntities = bookcaseService.getAllBookcases();
```

**Note:** For `options`, you still need the generic types on the right (`<String, String>`) so the compiler knows the type.

---

### var Limitations

**1. Can't use for fields:**
```java
// ❌ Error: var only for local variables
public class BookService {
    private var repository;  // ← COMPILE ERROR
}
```

**2. Can't use for method parameters:**
```java
// ❌ Error: var only for local variables
public void process(var book) {  // ← COMPILE ERROR
}
```

**3. Can't use without initializer:**
```java
// ❌ Error: var needs initializer to infer type
var title;  // ← COMPILE ERROR
title = "Harry Potter";
```

**4. Can't use with null:**
```java
// ❌ Error: null has no type
var book = null;  // ← COMPILE ERROR
```

**5. Can't use with lambda:**
```java
// ❌ Error: Lambda needs target type
var predicate = book -> book.getTitle().contains("Java");  // ← COMPILE ERROR

// ✅ Fixed: Explicit type
Predicate<BookEntity> predicate = book -> book.getTitle().contains("Java");
```

---

### var Best Practices

**Guidelines:**
1. Use `var` when the type is **obvious** from the right-hand side
2. Use `var` for **long generic types** (reduces noise)
3. Use `var` for **builder patterns** (type is clear from builder)
4. **Don't use var** when it hurts readability
5. **Don't use var** everywhere just because you can

**Your Code:**
- ✅ Explicit types are fine (clear and documented)
- ✅ Consider `var` for ComponentFlow builders (reduces verbosity)
- ✅ Consider `var` for long generic types (`Map<String, List<...>>`)
- ❌ Don't use `var` for simple method calls where return type isn't obvious

---

## When NOT to Use Modern Features

### 1. Records for Entities

**❌ Don't:**
```java
@Entity
public record BookEntity(Long bookId, String title, Set<AuthorEntity> authors) {}
```

**Why Not:**
- JPA needs setters for lazy loading
- Entities are mutable by design
- Records are immutable

**✅ Do:**
```java
@Entity
public class BookEntity {
    @Id
    private Long bookId;
    private String title;
    // ... setters, getters, lifecycle methods
}
```

---

### 2. Text Blocks for Short Strings

**❌ Don't:**
```java
String message = """
    Hello""";
```

**Why Not:**
- Overkill for single-line strings
- Less readable than regular string

**✅ Do:**
```java
String message = "Hello";
```

---

### 3. Switch Expressions for Single Case

**❌ Don't:**
```java
String status = switch (book.getBookStatus()) {
    case "AVAILABLE" -> "Available";
    default -> "Unavailable";
};
```

**Why Not:**
- Ternary is cleaner for two cases

**✅ Do:**
```java
String status = "AVAILABLE".equals(book.getBookStatus()) ? "Available" : "Unavailable";
```

---

### 4. var Everywhere

**❌ Don't:**
```java
var a = getData();
var b = process(a);
var c = transform(b);
```

**Why Not:**
- Types are unclear
- Hurts readability
- No benefit

**✅ Do:**
```java
BookEntity book = getData();
BookDTO dto = process(book);
String json = transform(dto);
```

---

## Action Items

### Priority 1 (High Value Improvements)

- [ ] **Add validation to BookRequestDTO record**
  - Add compact constructor with null/blank checks
  - Throw IllegalArgumentException with helpful messages
  - Test: Try to create DTO with blank title

- [ ] **Add helper method to BookRequestDTO**
  - `public String fullAuthorName()` that returns "firstName lastName"
  - Use in BookService instead of concatenating manually

### Priority 2 (Code Quality)

- [ ] **Consider switch expression for menu handling**
  - BookCommands.java:203-209 (searchType dispatch)
  - Replace if-else with switch expression
  - More scalable as menu grows

- [ ] **Experiment with var in builder chains**
  - Try using `var` for ComponentFlow builders
  - See if it improves or hurts readability
  - Keep explicit types if `var` makes code less clear

### Priority 3 (Optional/Learning)

- [ ] **Extract text block templates to constants**
  - Create `private static final String CHECKOUT_TEMPLATE = """..."""`
  - Reuse templates with `.formatted()`
  - Centralize message formatting

- [ ] **Create BookStatus enum**
  - Replace String constants with enum (`AVAILABLE`, `CHECKED_OUT`, `RESERVED`)
  - Use switch expression to map enum to display text
  - Safer than string comparisons

---

## Key Takeaways

### 1. You're Already Using Modern Features!

✅ **Records** — 5 perfect use cases (DTOs, projections)
✅ **Text blocks** — 17 locations (queries, messages)

**No changes needed!** You're already writing modern, clean Java.

---

### 2. Records Are for Data, Not Behavior

**Use records when:**
- Immutable data carrier (DTOs, value objects)
- No business logic (just getters)
- Equality based on values (not identity)

**Don't use records when:**
- JPA entities (need mutability)
- Builders (accumulate state)
- Complex behavior (methods > data)

---

### 3. Text Blocks Make Multi-Line Strings Readable

```java
// ✅ Clear, maintainable
@Query("""
    SELECT new BookSummary(b.id, b.title)
    FROM BookEntity b
    WHERE b.title LIKE :search
    """)

// ❌ Escape hell
@Query("SELECT new BookSummary(b.id, b.title) " +
       "FROM BookEntity b " +
       "WHERE b.title LIKE :search")
```

---

### 4. Switch Expressions Eliminate Fallthrough Bugs

```java
// ✅ Can't forget break (no fallthrough)
String status = switch (bookStatus) {
    case "AVAILABLE" -> "✅ Available";
    case "CHECKED_OUT" -> "📤 Out";
    default -> "Unknown";
};

// ❌ Easy to forget break
String status;
switch (bookStatus) {
    case "AVAILABLE":
        status = "✅ Available";
        // ← FORGOT break! Falls through to CHECKED_OUT!
    case "CHECKED_OUT":
        status = "📤 Out";
        break;
}
```

---

### 5. var Is About Readability, Not Laziness

```java
// ✅ Good: Type is obvious
var books = new ArrayList<BookEntity>();

// ❌ Bad: Type is unclear
var result = getData();  // What type?

// ✅ Better: Explicit when unclear
BookEntity result = getData();
```

**Rule:** If a reader can't infer the type in 2 seconds, don't use `var`.

---

### 6. Modern Features Should Reduce Boilerplate

**Before records:**
- 30+ lines of boilerplate (constructor, getters, equals, hashCode, toString)

**After records:**
- 1 line: `public record BookSummary(Long bookId, String title) {}`

**Before text blocks:**
- Escape sequences (`\n`), concatenation (`+`), quote escaping (`\"`)

**After text blocks:**
- Natural formatting, readable SQL/messages

**Modern features aren't about being "cool"—they're about clarity.**

---

### 7. Bibby's Modern Features Report Card

| Feature | Usage | Grade | Notes |
|---------|-------|-------|-------|
| **Records** | 5 records | A+ | Perfect use cases (DTOs, projections) |
| **Text blocks** | 17 text blocks | A+ | Queries and messages |
| **Switch expressions** | 0 | B | Opportunity in menu handling |
| **Pattern matching** | N/A | N/A | No instanceof in code |
| **var** | 0 | B | Opportunity in builder chains |

**Overall:** A- (Excellent use of records and text blocks!)

---

## Practice Exercise

**Challenge:** Add validation to `BookRequestDTO` with a compact constructor.

**Steps:**

1. Open `BookRequestDTO.java`
2. Add compact constructor:
   ```java
   public BookRequestDTO {
       if (title == null || title.isBlank()) {
           throw new IllegalArgumentException("Title cannot be blank");
       }
       // Add checks for firstName, lastName
   }
   ```
3. Add helper method:
   ```java
   public String fullAuthorName() {
       return firstName + " " + lastName;
   }
   ```
4. Test: Try to create DTO with blank title (should throw exception)
5. Use `fullAuthorName()` in `BookService.createNewBook()`

**Bonus:** Convert menu handling (BookCommands.java:203-209) to switch expression.

---

**Mentor's Note:**

You're already writing modern Java without even thinking about it. Your use of **records** and **text blocks** shows excellent judgment—you're using them where they belong, not forcing them everywhere.

**Records** have eliminated hundreds of lines of boilerplate from your DTOs. Imagine maintaining 30 lines per DTO (constructor, getters, equals, hashCode, toString) across 5 DTOs—that's 150 lines you don't have to write or test.

**Text blocks** have made your SQL queries and CLI messages readable. Compare your beautiful JPQL query in `ShelfRepository.java` to what it would look like with string concatenation—night and day.

Modern features aren't about chasing the latest Java version. They're about **writing less code that expresses more intent**. You're already doing this.

Keep that mindset as you learn switch expressions and var. Use them where they **improve clarity**, not just because they're "modern."

---

*Section 16 Complete — 50 min read*
*Next: Section 17 — JUnit & Testing Strategy*
*Progress: 16 / 33 sections (48%)*
