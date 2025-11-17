# Section 9: Classes
**Clean Code Principle:** *"Classes should be small, focused, and have a single reason to change."*

---

## 📚 Principles Overview

### Why Class Design Matters

Uncle Bob says: **"The first rule of classes is that they should be small. The second rule is that they should be smaller than that."**

But "small" doesn't mean line count—it means **responsibility**. A class should do **one thing** and do it well. This is the **Single Responsibility Principle (SRP)**, the most important of the SOLID principles.

### The SOLID Principles

**SOLID** is an acronym for five class design principles that make software more maintainable:

1. **S**ingle Responsibility Principle (SRP)
2. **O**pen/Closed Principle (OCP)
3. **L**iskov Substitution Principle (LSP)
4. **I**nterface Segregation Principle (ISP)
5. **D**ependency Inversion Principle (DIP)

Let's dive into each one.

---

## 1️⃣ Single Responsibility Principle (SRP)

**Definition:** *A class should have one, and only one, reason to change.*

**What It Means:** If you can describe a class's purpose with "and" or "or", it's doing too much.

```java
// ❌ BAD: Multiple responsibilities (violates SRP)
public class UserManager {
    public void createUser(User user) { ... }           // User management
    public void sendWelcomeEmail(User user) { ... }     // Email sending
    public void logUserCreation(User user) { ... }      // Logging
    public String formatUserDisplay(User user) { ... }  // Formatting
}
```

**Why It Violates SRP:**
- Changes to email templates require changing UserManager
- Changes to logging format require changing UserManager
- Changes to display format require changing UserManager

**That's THREE reasons to change!**

```java
// ✅ GOOD: Single responsibilities (follows SRP)
public class UserService {
    public void createUser(User user) { ... }  // ONLY user management
}

public class EmailService {
    public void sendWelcomeEmail(User user) { ... }  // ONLY email sending
}

public class AuditLogger {
    public void logUserCreation(User user) { ... }  // ONLY logging
}

public class UserFormatter {
    public String formatUserDisplay(User user) { ... }  // ONLY formatting
}
```

---

## 2️⃣ Open/Closed Principle (OCP)

**Definition:** *Classes should be open for extension but closed for modification.*

**What It Means:** You should be able to add new functionality without changing existing code.

```java
// ❌ BAD: Must modify class to add new report types
public class ReportGenerator {
    public void generateReport(String type) {
        if (type.equals("PDF")) {
            // Generate PDF
        } else if (type.equals("CSV")) {
            // Generate CSV
        } else if (type.equals("HTML")) {  // Adding HTML requires modifying this class!
            // Generate HTML
        }
    }
}

// ✅ GOOD: Add new report types without modifying existing code
public interface ReportGenerator {
    void generate(Report report);
}

public class PdfReportGenerator implements ReportGenerator {
    public void generate(Report report) { ... }
}

public class CsvReportGenerator implements ReportGenerator {
    public void generate(Report report) { ... }
}

public class HtmlReportGenerator implements ReportGenerator {  // NEW! No modification needed
    public void generate(Report report) { ... }
}
```

---

## 3️⃣ Liskov Substitution Principle (LSP)

**Definition:** *Subtypes must be substitutable for their base types.*

**What It Means:** If class B extends class A, you should be able to use B anywhere you use A without breaking behavior.

```java
// ❌ BAD: Violates LSP (Square changes Rectangle behavior)
public class Rectangle {
    protected int width, height;

    public void setWidth(int w) { width = w; }
    public void setHeight(int h) { height = h; }
    public int getArea() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) {
        width = w;
        height = w;  // ❌ Unexpected side effect!
    }
}

// This breaks:
Rectangle r = new Square();
r.setWidth(5);
r.setHeight(10);
// Expected area: 50, Actual area: 100 (LSP violation!)

// ✅ GOOD: Don't use inheritance if it violates LSP
public interface Shape {
    int getArea();
}

public class Rectangle implements Shape {
    private int width, height;
    public int getArea() { return width * height; }
}

public class Square implements Shape {
    private int side;
    public int getArea() { return side * side; }
}
```

---

## 4️⃣ Interface Segregation Principle (ISP)

**Definition:** *Clients shouldn't be forced to depend on interfaces they don't use.*

**What It Means:** Many small, focused interfaces are better than one large, general-purpose interface.

```java
// ❌ BAD: Fat interface forces implementations to provide methods they don't need
public interface Worker {
    void work();
    void eat();
    void sleep();
}

public class Robot implements Worker {
    public void work() { ... }
    public void eat() { }  // ❌ Robots don't eat! Empty implementation = bad design
    public void sleep() { }  // ❌ Robots don't sleep!
}

// ✅ GOOD: Segregated interfaces
public interface Workable {
    void work();
}

public interface Eatable {
    void eat();
}

public interface Sleepable {
    void sleep();
}

public class Human implements Workable, Eatable, Sleepable {
    public void work() { ... }
    public void eat() { ... }
    public void sleep() { ... }
}

public class Robot implements Workable {
    public void work() { ... }  // Only implements what makes sense
}
```

---

## 5️⃣ Dependency Inversion Principle (DIP)

**Definition:** *Depend on abstractions, not concretions.*

**What It Means:** High-level modules shouldn't depend on low-level modules. Both should depend on abstractions (interfaces).

```java
// ❌ BAD: High-level class depends on low-level concrete class
public class BookService {
    private PostgreSQLBookRepository repository;  // ❌ Concrete dependency!

    public BookService() {
        this.repository = new PostgreSQLBookRepository();  // ❌ Hard-coded!
    }
}

// ✅ GOOD: Depend on abstraction (interface)
public class BookService {
    private final BookRepository repository;  // ✅ Interface dependency

    public BookService(BookRepository repository) {  // ✅ Injected
        this.repository = repository;
    }
}

// Can now swap implementations without changing BookService:
// - PostgreSQLBookRepository
// - MySQLBookRepository
// - InMemoryBookRepository (for testing!)
```

**Spring Boot handles this automatically via Dependency Injection!**

---

## 🔍 Your Code Analysis

### Issue #1: BookcaseService Violates SRP (🔴 CRITICAL)

**Location:** `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java:25-41`

```java
public String createNewBookCase(String label, int capacity){
    BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
    if(bookcaseEntity !=null){
        log.error("Failed to save Record - Record already exist",existingRecordError);
        throw existingRecordError;
    }
    else{
        bookcaseEntity = new BookcaseEntity(label,capacity);
        bookcaseRepository.save(bookcaseEntity);

        for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
            addShelf(bookcaseEntity,i,i);  // ❌ CREATING SHELVES (different responsibility!)
        }
        log.info("Created new bookcase: {}",bookcaseEntity.getBookcaseLabel());
        return "Created New Bookcase " + label + " with shelf capacity of " + capacity;
    }
}

public void addShelf(BookcaseEntity bookcaseEntity, int label, int position){
    ShelfEntity shelfEntity = new ShelfEntity();
    shelfEntity.setBookcaseId(bookcaseEntity.getBookcaseId());
    shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());
    shelfEntity.setShelfPosition(position);
    shelfRepository.save(shelfEntity);  // ❌ DIRECTLY USING ShelfRepository
}
```

**Problems:**
1. **Two Responsibilities:** Managing bookcases AND managing shelves
2. **Two Reasons to Change:** If shelf creation logic changes OR bookcase logic changes
3. **Violates SRP:** BookcaseService depends on both BookcaseRepository AND ShelfRepository

**Why This Is Bad:**
- Changes to shelf creation require modifying BookcaseService
- Can't test bookcase creation without involving shelf logic
- Hard to understand what BookcaseService's real job is

---

### Issue #2: BookcaseCommands Has Mixed Responsibilities

**Location:** `src/main/java/com/penrose/bibby/cli/BookcaseCommands.java`

```java
// RESPONSIBILITY 1: UI Formatting
public String bookcaseRowFormater(BookcaseEntity bookcaseEntity, int bookCount){
    return String.format(" %-12s \u001B[1m\u001B[38;5;63m%-2d...",
                         bookcaseEntity.getBookcaseLabel().toUpperCase(),
                         bookcaseEntity.getShelfCapacity(),
                         bookCount);
}

// RESPONSIBILITY 2: Building UI Options (with complex business logic!)
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();
    for (BookcaseEntity b : bookcaseEntities) {
        int shelfBookCount = 0;
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());
        for(ShelfEntity s : shelves){
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());
            shelfBookCount += bookList.size();  // ❌ Complex counting logic in UI layer!
        }
        options.put(bookcaseRowFormater(b,shelfBookCount), b.getBookcaseId().toString());
    }
    return options;
}

// RESPONSIBILITY 3: Command Orchestration
@Command(command = "browse")
public void listAllBookcases(){
    // ... ComponentFlow code
}
```

**Problems:**
1. **Three responsibilities:** Formatting, business logic (counting books), command handling
2. **Poor cohesion:** Formatting methods mixed with command methods
3. **Business logic in UI layer:** Book counting should be in a service

**This violates SRP badly.**

---

### Issue #3: Anemic Domain Models (No Cohesion)

**Location:** `src/main/java/com/penrose/bibby/library/bookcase/BookcaseEntity.java`

```java
@Entity
@Table(name = "bookcases")
public class BookcaseEntity {
    private Long bookcaseId;
    private String bookcaseLabel;
    private int shelfCapacity;

    // 30 lines of getters/setters
    // ZERO business logic
}
```

**Location:** `src/main/java/com/penrose/bibby/library/author/AuthorEntity.java`

```java
@Entity
public class AuthorEntity {
    private Long authorId;
    private String firstName;
    private String lastName;
    private String fullName;  // ❌ REDUNDANT! Can be computed from firstName + lastName

    public AuthorEntity(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = String.format("%s %s", firstName, lastName);  // ❌ Duplication
    }

    public void setFullName(String fullName) {  // ❌ DANGEROUS! Can become inconsistent
        this.fullName = fullName;
    }

    // All getters/setters, no behavior
}
```

**Problems:**
1. **Anemic domain model:** Data structures pretending to be objects
2. **No cohesion:** Data and behavior are separated (behavior lives in services)
3. **Data inconsistency risk:** `fullName` can become out of sync with `firstName`/`lastName`
4. **Redundant field:** `fullName` should be a computed property

**We covered this in Section 5, but it's a class design issue too.**

---

### Issue #4: ShelfService Has No Real Responsibility

**Location:** `src/main/java/com/penrose/bibby/library/shelf/ShelfService.java`

```java
@Service
public class ShelfService {
    ShelfRepository shelfRepository;

    public List<ShelfEntity> getAllShelves(Long bookCaseId){
        return shelfRepository.findByBookcaseId(bookCaseId);  // ❌ Just delegates
    }

    public Optional<ShelfEntity> findShelfById(Long shelfId) {
        return shelfRepository.findById(shelfId);  // ❌ Just delegates
    }

    public List<ShelfEntity> findByBookcaseId(Long bookcaseId) {
        return shelfRepository.findByBookcaseId(bookcaseId);  // ❌ DUPLICATE method!
    }

    public List<ShelfSummary> getShelfSummariesForBookcase(Long bookcaseId) {
        return shelfRepository.findShelfSummariesByBookcaseId(bookcaseId);  // ❌ Just delegates
    }
}
```

**Problems:**
1. **No added value:** Every method is a one-line delegation to the repository
2. **Duplicate methods:** `getAllShelves()` and `findByBookcaseId()` do the same thing
3. **Unnecessary layer:** This service could be removed entirely
4. **False abstraction:** Pretends to add value but doesn't

**Why Does This Exist?**
You might be following a pattern you've seen ("every repository needs a service"), but that's **cargo cult programming**. Only create a service if it adds real value (business logic, orchestration, etc.).

---

### Issue #5: Pre-Instantiated Exception (Violates OCP)

**Location:** `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java:18`

```java
@Service
public class BookcaseService {
    private final ResponseStatusException existingRecordError =
        new ResponseStatusException(HttpStatus.CONFLICT,
                                    "Bookcase with the label already exist");  // ❌ Field-level

    public String createNewBookCase(String label, int capacity){
        if(bookcaseEntity != null){
            throw existingRecordError;  // ❌ Always throws same instance
        }
    }
}
```

**Problems:**
1. **Reused exception instance:** Stack trace will be from first use, not current use
2. **Fixed error message:** Can't include the actual label that caused the conflict
3. **Hard to extend:** Adding new validation requires modifying the class (violates OCP)

---

### Issue #6: Poor Cohesion in BookService

**Location:** `src/main/java/com/penrose/bibby/library/book/BookService.java`

BookService has **10 methods** that fall into different responsibilities:

```java
@Service
public class BookService {
    // RESPONSIBILITY 1: Book creation
    public void createNewBook(BookRequestDTO bookRequestDTO) { ... }

    // RESPONSIBILITY 2: Book searching
    public BookEntity findBookByTitle(String title) { ... }
    public Optional<BookEntity> findBookById(Long bookId) { ... }
    public List<BookEntity> findBooksByShelf(Long id) { ... }

    // RESPONSIBILITY 3: Book status management
    public void checkOutBook(BookEntity bookEntity) { ... }
    public void checkInBook(String bookTitle) { ... }

    // RESPONSIBILITY 4: Author management (!)
    public List<AuthorEntity> findAuthorsByBookId(Long bookId) { ... }

    // RESPONSIBILITY 5: Book details/summaries
    public List<BookSummary> getBooksForShelf(Long shelfId) { ... }
    public BookDetailView getBookDetails(Long bookId) { ... }
}
```

**Problems:**
1. **Low cohesion:** Methods don't work together toward a single purpose
2. **Multiple responsibilities:** Searching, status management, author management, formatting
3. **Author management in BookService?** That should be in AuthorService
4. **Growing complexity:** As features are added, this class will become unmaintainable

**This is on the edge of violating SRP.** It's not terrible yet, but it's trending in the wrong direction.

---

## 🛠️ Refactoring Examples

### Example 1: Fix BookcaseService SRP Violation

**BEFORE (violates SRP):**
```java
@Service
public class BookcaseService {
    private final BookcaseRepository bookcaseRepository;
    private final ShelfRepository shelfRepository;  // ❌ Shouldn't be here

    public String createNewBookCase(String label, int capacity){
        // Create bookcase
        bookcaseEntity = new BookcaseEntity(label, capacity);
        bookcaseRepository.save(bookcaseEntity);

        // Create shelves (different responsibility!)
        for(int i = 0; i < capacity; i++){
            addShelf(bookcaseEntity, i, i);
        }
        return "Created New Bookcase...";
    }

    public void addShelf(BookcaseEntity bookcaseEntity, int label, int position){
        ShelfEntity shelfEntity = new ShelfEntity();
        shelfEntity.setBookcaseId(bookcaseEntity.getBookcaseId());
        shelfEntity.setShelfLabel("Shelf " + label);
        shelfRepository.save(shelfEntity);  // ❌ Using wrong repository
    }
}
```

**AFTER (follows SRP):**

**Step 1:** Move shelf creation to ShelfService

```java
@Service
public class ShelfService {
    private final ShelfRepository shelfRepository;

    // NEW: Business logic for creating shelves
    public void createShelvesForBookcase(Long bookcaseId, int count) {
        for (int i = 0; i < count; i++) {
            ShelfEntity shelf = new ShelfEntity();
            shelf.setBookcaseId(bookcaseId);
            shelf.setShelfLabel("Shelf " + i);
            shelf.setShelfPosition(i);
            shelfRepository.save(shelf);
        }
    }

    // Keep existing methods...
}
```

**Step 2:** Update BookcaseService to use ShelfService

```java
@Service
public class BookcaseService {
    private final BookcaseRepository bookcaseRepository;
    private final ShelfService shelfService;  // ✅ Inject ShelfService, not ShelfRepository

    public BookcaseService(BookcaseRepository bookcaseRepository,
                           ShelfService shelfService) {
        this.bookcaseRepository = bookcaseRepository;
        this.shelfService = shelfService;
    }

    public String createNewBookCase(String label, int capacity) {
        validateBookcaseDoesNotExist(label);

        BookcaseEntity bookcase = new BookcaseEntity(label, capacity);
        bookcaseRepository.save(bookcase);

        shelfService.createShelvesForBookcase(bookcase.getBookcaseId(), capacity);  // ✅ Delegate

        log.info("Created new bookcase: {}", label);
        return "Created New Bookcase " + label + " with " + capacity + " shelves";
    }

    private void validateBookcaseDoesNotExist(String label) {
        BookcaseEntity existing = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
        if (existing != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Bookcase with label '" + label + "' already exists"  // ✅ Include label
            );
        }
    }
}
```

**Benefits:**
- ✅ Single Responsibility: BookcaseService only manages bookcases
- ✅ ShelfService only manages shelves
- ✅ Clear separation of concerns
- ✅ Easier to test in isolation
- ✅ Exception message includes actual label

---

### Example 2: Extract BookcaseFormatter from BookcaseCommands

**BEFORE (violates SRP):**
```java
@Component
public class BookcaseCommands {
    // Mixed responsibilities: formatting + commands

    public String bookcaseRowFormater(BookcaseEntity bookcaseEntity, int bookCount){
        return String.format(" %-12s \u001B[1m\u001B[38;5;63m%-2d...", ...);
    }

    @Command(command = "browse")
    public void listAllBookcases(){ ... }
}
```

**AFTER (follows SRP):**

**Create BookcaseFormatter:**
```java
@Component
public class BookcaseFormatter {
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_PURPLE = "\u001B[38;5;63m";
    private static final String ANSI_WHITE = "\u001B[38;5;15m";

    public String formatBookcaseRow(BookcaseEntity bookcase, int bookCount) {
        return String.format(
            " %-12s %s%s%-2d%s%sShelves    %s%s%-2d%s%sBooks",
            bookcase.getBookcaseLabel().toUpperCase(),
            ANSI_BOLD, ANSI_PURPLE, bookcase.getShelfCapacity(),
            ANSI_RESET, ANSI_WHITE,
            ANSI_BOLD, ANSI_PURPLE, bookCount,
            ANSI_RESET, ANSI_WHITE
        );
    }
}
```

**Update BookcaseCommands:**
```java
@Component
@Command(command = "bookcase", group = "Bookcase Commands")
public class BookcaseCommands {
    private final ComponentFlow.Builder componentFlowBuilder;
    private final BookcaseService bookcaseService;
    private final BookcaseFormatter formatter;  // ✅ Inject formatter

    @Command(command = "browse")
    public void listAllBookcases() {
        Map<String, String> options = buildBookcaseOptions();
        // ... ComponentFlow code
    }

    private Map<String, String> buildBookcaseOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        List<BookcaseEntity> bookcases = bookcaseService.getAllBookcases();

        for (BookcaseEntity bookcase : bookcases) {
            int bookCount = bookcaseService.getTotalBookCount(bookcase.getBookcaseId());
            String displayText = formatter.formatBookcaseRow(bookcase, bookCount);  // ✅ Use formatter
            options.put(displayText, bookcase.getBookcaseId().toString());
        }

        return options;
    }
}
```

**But wait!** We still have business logic (counting books) in the command class. Let's fix that too:

**Add method to BookcaseService:**
```java
@Service
public class BookcaseService {
    private final BookcaseRepository bookcaseRepository;
    private final ShelfService shelfService;
    private final BookService bookService;

    public int getTotalBookCount(Long bookcaseId) {
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(bookcaseId);
        return shelves.stream()
            .mapToInt(shelf -> bookService.findBooksByShelf(shelf.getShelfId()).size())
            .sum();
    }
}
```

**Now BookcaseCommands is clean:**
```java
private Map<String, String> buildBookcaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    List<BookcaseEntity> bookcases = bookcaseService.getAllBookcases();

    for (BookcaseEntity bookcase : bookcases) {
        int bookCount = bookcaseService.getTotalBookCount(bookcase.getBookcaseId());  // ✅ Service handles it
        String displayText = formatter.formatBookcaseRow(bookcase, bookCount);
        options.put(displayText, bookcase.getBookcaseId().toString());
    }

    return options;
}
```

**Benefits:**
- ✅ BookcaseCommands only handles command orchestration
- ✅ BookcaseFormatter only handles formatting
- ✅ BookcaseService handles business logic (book counting)
- ✅ Each class has ONE reason to change

---

### Example 3: Fix AuthorEntity Redundant Field

**BEFORE (inconsistency risk):**
```java
public class AuthorEntity {
    private String firstName;
    private String lastName;
    private String fullName;  // ❌ Can become inconsistent

    public AuthorEntity(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = String.format("%s %s", firstName, lastName);
    }

    public void setFullName(String fullName) {  // ❌ DANGEROUS
        this.fullName = fullName;  // firstName = "John", lastName = "Doe", fullName = "Jane Smith" 😱
    }
}
```

**AFTER (computed property):**
```java
public class AuthorEntity {
    private String firstName;
    private String lastName;
    // ✅ fullName removed from persistence

    public AuthorEntity(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Transient  // ✅ Not persisted to database
    public String getFullName() {
        return String.format("%s %s", firstName, lastName);  // ✅ Always consistent
    }

    // No setFullName() - can't become inconsistent!
}
```

**Benefits:**
- ✅ Single source of truth (firstName + lastName)
- ✅ Impossible for fullName to be inconsistent
- ✅ Less storage (one fewer column)
- ✅ Follows DRY principle

---

## 🎯 Class Organization Best Practices

### Practice 1: The Newspaper Metaphor

Classes should be organized like newspaper articles:
1. **Public constants** at the top
2. **Private fields** next
3. **Constructor(s)**
4. **Public methods** (most important first)
5. **Private helper methods** last

```java
// ✅ GOOD: Organized like a newspaper
public class BookService {
    // 1. Constants
    private static final int MAX_CHECKOUT_DAYS = 14;

    // 2. Fields
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    // 3. Constructor
    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    // 4. Public methods (high-level)
    public Optional<BookEntity> findBookByTitle(String title) {
        validateTitle(title);
        return Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    }

    public void checkOutBook(BookEntity book) {
        validateBook(book);
        book.setBookStatus(BookStatus.CHECKED_OUT.toString());
        book.setCheckoutDate(LocalDateTime.now());
        bookRepository.save(book);
    }

    // 5. Private helpers (low-level details)
    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
    }

    private void validateBook(BookEntity book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
    }
}
```

**Read from top to bottom:** High-level → Low-level details

---

### Practice 2: Cohesion - Methods Should Use Instance Variables

**High cohesion** = most methods use most instance variables.

```java
// ❌ LOW COHESION: Methods use different subsets of variables
public class Employee {
    private String name;
    private double salary;
    private String department;
    private LocalDate hireDate;

    public String formatName() {
        return name.toUpperCase();  // Uses only name
    }

    public double calculateBonus() {
        return salary * 0.1;  // Uses only salary
    }

    public String getDepartmentReport() {
        return department + " Department";  // Uses only department
    }

    public int getYearsOfService() {
        return LocalDate.now().getYear() - hireDate.getYear();  // Uses only hireDate
    }
}
// This class should be split into 4 classes!

// ✅ HIGH COHESION: Most methods use most variables
public class Rectangle {
    private int width;
    private int height;

    public int getArea() {
        return width * height;  // Uses width AND height
    }

    public int getPerimeter() {
        return 2 * (width + height);  // Uses width AND height
    }

    public boolean isSquare() {
        return width == height;  // Uses width AND height
    }
}
// All methods use both instance variables = high cohesion
```

---

## 🚨 Your Critical Issues & Solutions

### Issue Summary Table

| Issue | Severity | Violates | Fix Time |
|-------|----------|----------|----------|
| BookcaseService manages shelves | 🔴 HIGH | SRP | 1-2 hours |
| BookcaseCommands mixed responsibilities | 🔴 HIGH | SRP | 2-3 hours |
| Anemic domain models | 🟡 MEDIUM | SRP, OOP | 4-6 hours |
| ShelfService adds no value | 🟡 MEDIUM | Design | 1 hour |
| Pre-instantiated exception | 🟢 LOW | OCP | 15 min |
| AuthorEntity redundant field | 🟢 LOW | DRY | 30 min |

---

## ✅ Your Action Items

### 🔴 **Priority 1: Fix BookcaseService SRP Violation** (1-2 hours)

**Action:**
1. Move shelf creation logic from BookcaseService to ShelfService
2. Create `createShelvesForBookcase()` method in ShelfService
3. Remove `ShelfRepository` dependency from BookcaseService
4. Update `createNewBookCase()` to call `shelfService.createShelvesForBookcase()`

**Files:**
- `library/bookcase/BookcaseService.java`
- `library/shelf/ShelfService.java`

---

### 🔴 **Priority 2: Extract BookcaseFormatter** (2-3 hours)

**Action:**
1. Create `BookcaseFormatter` class
2. Move `bookcaseRowFormater()` to formatter
3. Extract ANSI color constants
4. Move book counting logic from BookcaseCommands to BookcaseService
5. Create `getTotalBookCount()` in BookcaseService
6. Update BookcaseCommands to use formatter and service

**Files:**
- Create: `cli/BookcaseFormatter.java`
- Update: `cli/BookcaseCommands.java`
- Update: `library/bookcase/BookcaseService.java`

---

### 🟡 **Priority 3: Fix AuthorEntity Redundant Field** (30 minutes)

**Action:**
1. Remove `fullName` field from AuthorEntity
2. Make `getFullName()` a `@Transient` computed property
3. Remove `setFullName()` method
4. Update database migration to drop `full_name` column (if exists)

**Files:**
- `library/author/AuthorEntity.java`

---

### 🟡 **Priority 4: Fix Pre-Instantiated Exception** (15 minutes)

**Action:**
Replace field-level exception with inline creation that includes the actual label:

```java
// REMOVE this field
private final ResponseStatusException existingRecordError = ...

// REPLACE with inline throw that includes label
private void validateBookcaseDoesNotExist(String label) {
    BookcaseEntity existing = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
    if (existing != null) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Bookcase with label '" + label + "' already exists"
        );
    }
}
```

**Files:**
- `library/bookcase/BookcaseService.java`

---

### 🟢 **Priority 5: Evaluate ShelfService** (1 hour study)

**Action:**
Decide if ShelfService adds value or should be removed:
- If it only delegates to repository → **remove it, use repository directly**
- If it has business logic (after adding shelf creation) → **keep it**

**Current assessment:** After adding `createShelvesForBookcase()`, it has real business logic. Keep it, but remove the duplicate `getAllShelves()` and `findByBookcaseId()` methods.

---

## 📊 SOLID Scorecard for Your Codebase

| Principle | Current Grade | Issues | Target |
|-----------|---------------|--------|--------|
| Single Responsibility | ⚠️ C+ | BookcaseService, Commands | A |
| Open/Closed | ⚠️ B- | Pre-instantiated exceptions | A |
| Liskov Substitution | ✅ A | No violations (minimal inheritance) | A |
| Interface Segregation | ✅ B+ | Could use smaller interfaces | A |
| Dependency Inversion | ✅ A- | Spring handles this well | A |

**Overall SOLID Grade: B-**

Good foundation thanks to Spring Boot, but SRP violations need addressing.

---

## 🎓 Key Takeaways

1. **SRP is the most important SOLID principle.** One class = one responsibility = one reason to change.

2. **Ask "What is this class's job?"** If the answer includes "and", you have multiple responsibilities.

3. **High cohesion = good design.** Methods should work together using shared instance variables.

4. **Anemic domain models are anti-patterns.** Entities should have behavior, not just getters/setters.

5. **Services should add value.** If a service is just delegating to a repository, remove it.

6. **Organize classes like newspapers.** Public first, private last. High-level before low-level.

7. **Depend on abstractions (interfaces), not concretions.** Spring Boot makes this easy via DI.

8. **Open/Closed Principle enables extension without modification.** Use interfaces and polymorphism.

9. **Computed properties > redundant fields.** Avoid data inconsistency (like AuthorEntity.fullName).

10. **Classes should be small.** Not in lines, but in responsibility.

---

## 📚 Further Study

### Books
- **"Agile Software Development: Principles, Patterns, and Practices"** - Robert C. Martin
  *The definitive guide to SOLID principles.*

- **"Domain-Driven Design"** - Eric Evans
  *Learn to build rich domain models instead of anemic ones.*

### Articles
- **Uncle Bob: "The Single Responsibility Principle"**
  https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html

- **Martin Fowler: "Anemic Domain Model"**
  https://martinfowler.com/bliki/AnemicDomainModel.html

### Videos
- **SOLID Principles by Uncle Bob**
  Search YouTube for "Robert Martin SOLID Principles"

---

## 💭 Mentor's Final Thoughts

Leo, here's the truth: **your class design is better than most junior developers' code** because you're using Spring Boot, which enforces dependency injection (DIP).

But you have two critical SRP violations:
1. **BookcaseService creating shelves** (different responsibility)
2. **BookcaseCommands handling formatting + business logic** (mixed concerns)

These are **easy fixes** (2-3 hours total) and will dramatically improve your code's maintainability.

The bigger issue is your **anemic domain models**. Your entities are just data bags. We started addressing this in Section 5, but now you understand the **class design** reason why it's bad: **low cohesion**.

Here's my challenge: **After implementing the action items above, add ONE business method to BookEntity**—maybe `checkOut()` or `isAvailable()`—and see how it feels to have behavior live with data.

You're building the foundation for professional-grade code. These principles (especially SRP) will serve you for your entire career.

Keep going. You're doing great.

— Your Mentor

---

**Next:** Section 10 - Systems (architecture, dependency injection, scaling)
**Previous:** [Section 8 - Unit Tests](./08-unit-tests.md)
**Home:** [Master Index](./00-master-index.md)
