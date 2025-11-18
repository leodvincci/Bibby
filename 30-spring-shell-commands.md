# Section 30: Spring Shell — Building Interactive CLI Commands

**Estimated Time:** 65 minutes
**Prerequisite Sections:** 21 (Spring IoC & Dependency Injection), 22 (Component Scanning & Stereotypes)

---

## Introduction

While most Spring Boot applications expose REST APIs, **Spring Shell** enables you to build **interactive command-line interfaces (CLIs)** with the same dependency injection, component scanning, and service-layer architecture you already know. Bibby uses Spring Shell extensively to create a delightful library management CLI with interactive prompts, colored output, and smooth navigation flows.

In this section, we'll analyze Bibby's two command classes — **BookCommands** and **BookcaseCommands** — to understand:

- How `@Command` annotation registers CLI commands
- How `ComponentFlow` creates interactive multi-step user input
- Command vs Service separation of concerns
- Performance pitfalls (N+1 query problem in CLI code)
- Testing Spring Shell commands

By the end, you'll know how to build polished, maintainable CLI applications using Spring Shell.

---

## What is Spring Shell?

**Spring Shell** is a framework for building command-line applications with Spring Boot. It provides:

- **Command registration** via `@Command` annotation
- **Parameter binding** with `@Option` annotation
- **Interactive flows** via `ComponentFlow` API
- **Auto-completion** and **help text** generation
- **Integration** with Spring's dependency injection

Unlike traditional CLI tools where you parse `args[]` manually, Spring Shell handles argument parsing, validation, and command routing automatically.

---

## Bibby's Command Structure

Bibby has **2 command classes**:

| File | Class | Command Group | Methods |
|------|-------|---------------|---------|
| `BookCommands.java` | `BookCommands` | "Book Commands" | 10 commands (add, search, shelf, check-out, check-in, etc.) |
| `BookcaseCommands.java` | `BookcaseCommands` | "Bookcase Commands" | 2 commands (add, browse) |

Both extend `AbstractShellComponent` (Spring Shell's base class) and are annotated with `@Component` for component scanning.

---

## How @Command Works

### Class-Level @Command

The `@Command` annotation on the class defines a **command group**:

**BookCommands.java:24-26**
```java
@Component
@Command(command = "book", group = "Book Commands")
public class BookCommands extends AbstractShellComponent {
```

This means:
- All methods in this class belong to the `book` command group
- CLI commands will be prefixed with `book` (e.g., `book add`, `book search`)
- Help text groups these under "Book Commands"

**BookcaseCommands.java:20-22**
```java
@Component
@Command(command = "bookcase", group = "Bookcase Commands")
public class BookcaseCommands extends AbstractShellComponent {
```

Now you have:
- `bookcase add` to create a new bookcase
- `bookcase browse` to navigate bookcases

---

### Method-Level @Command

Each **public method** annotated with `@Command` becomes a CLI command:

**BookCommands.java:88-89**
```java
@Command(command = "add", description = "Add a new book to your library database")
public void addBook() throws InterruptedException {
```

This creates the command: **`book add`**

**BookCommands.java:188-189**
```java
@Command(command = "search", description = "Search for books by title, author, genre, or location using an interactive prompt.")
public void searchBook() throws InterruptedException {
```

This creates: **`book search`**

**BookcaseCommands.java:44-45**
```java
@Command(command = "add", description = "Create a new bookcase in the library.")
public void addBookcase() throws InterruptedException {
```

This creates: **`bookcase add`**

Notice how method names don't matter — the `command` attribute defines the actual CLI command.

---

## ComponentFlow: Interactive Multi-Step Input

The **ComponentFlow API** is Spring Shell's killer feature. Instead of requiring users to type long commands with many arguments, you can create **interactive prompts** that guide users step-by-step.

### Basic Pattern

**BookcaseCommands.java:46-57**
```java
@Command(command = "add", description = "Create a new bookcase in the library.")
public void addBookcase() throws InterruptedException {
    ComponentFlow flow = componentFlowBuilder.clone()
            .withStringInput("bookcaseLabel")
            .name("Give this bookcase a label:_")
            .and()
            .withStringInput("shelfCapacity")
            .name("How many shelves?:_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();
    bookcaseService.createNewBookCase(
        result.getContext().get("bookcaseLabel"),
        Integer.parseInt(result.getContext().get("shelfCapacity"))
    );
}
```

**What Happens:**
1. User types `bookcase add`
2. Prompt: "Give this bookcase a label:_" → User types "Fiction"
3. Prompt: "How many shelves?:_" → User types "5"
4. Flow completes, method retrieves values from `result.getContext()`
5. Service method called with `("Fiction", 5)`

### Dependency Injection for ComponentFlow.Builder

Notice the `componentFlowBuilder` field:

**BookCommands.java:60-69**
```java
private final ComponentFlow.Builder componentFlowBuilder;

public BookCommands(ComponentFlow.Builder componentFlowBuilder, BookService bookService, ...) {
    this.componentFlowBuilder = componentFlowBuilder;
    this.bookService = bookService;
    // ... other services
}
```

Spring Shell provides `ComponentFlow.Builder` as a bean. You inject it once, then call `.clone()` for each flow. This ensures each flow is independent.

---

## ComponentFlow Input Types

### 1. withStringInput() — Text Entry

**BookCommands.java:98-105**
```java
flow = componentFlowBuilder.clone()
        .withStringInput("title")
        .name("Book Title:_")
        .and()
        .withStringInput("author_count")
        .name("Number of Authors:_")
        .and()
        .build();
```

User sees:
```
Book Title:_ The Pragmatic Programmer
Number of Authors:_ 2
```

Retrieve values:
```java
String title = result.getContext().get("title", String.class);
int authorCount = Integer.parseInt(result.getContext().get("author_count", String.class));
```

⚠️ **Issue:** No validation! User could type "abc" for author count → `NumberFormatException`

---

### 2. withSingleItemSelector() — Menu Selection

**BookCommands.java:192-198**
```java
ComponentFlow flow = componentFlowBuilder.clone()
        .withSingleItemSelector("searchType")
        .name("How would you like to search?")
        .selectItems(buildSearchOptions())
        .max(10)
        .and()
        .build();
```

The `buildSearchOptions()` helper returns a `Map<String, String>`:

**BookCommands.java:405-415**
```java
private Map<String, String> buildSearchOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    options.put("Show all books  — \u001B[32mView the complete library\n\u001B[0m", "all");
    options.put("Title or keyword  —  \u001B[32mSearch by words in the title\n\u001B[0m", "title");
    options.put("Author  —  \u001B[32mFind books by author name\n\u001B[0m", "author");
    options.put("Genre  —  \u001B[32mFilter books by literary category\n\u001B[0m", "genre");
    options.put("Shelf/Location  —  \u001B[32mLocate books by physical shelf ID\n\u001B[0m", "shelf");
    options.put("Status  — \u001B[32mShow available or checked-out books\n\u001B[0m", "status");
    return options;
}
```

**Key:** Display text with ANSI colors (left side of map)
**Value:** Internal identifier (right side of map)

User sees a menu with arrow-key navigation. When they select "Author", the method receives `"author"`:

**BookCommands.java:201-209**
```java
ComponentFlow.ComponentFlowResult result = flow.run();
String searchType = result.getContext().get("searchType", String.class);

if (searchType.equalsIgnoreCase("author")) {
    searchByAuthor();
} else if (searchType.equalsIgnoreCase("title")) {
    searchByTitle();
}
```

---

## Reusable Helper Methods

Notice how Bibby extracts option-building logic into private methods:

**BookCommands.java:417-426**
```java
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();
    for (BookcaseEntity b : bookcaseEntities) {
        options.put(b.getBookcaseLabel(), b.getBookcaseId().toString());
    }
    return options;
}
```

**BookCommands.java:441-447**
```java
private Map<String, String> yesNoOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    options.put("Yes  — \u001B[32mLet's Do It\n\u001B[0m", "Yes");
    options.put("No  —  \u001B[32mNot this time\n\u001B[0m", "No");
    return options;
}
```

This keeps command methods clean and focused on flow orchestration.

---

## ⚠️ Performance Issue: N+1 Query Problem in CLI Code

**BookcaseCommands.java:60-74** contains a **critical performance bug**:

```java
private Map<String, String> bookCaseOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    List<BookcaseEntity> bookcaseEntities = bookcaseService.getAllBookcases();

    for (BookcaseEntity b : bookcaseEntities) {
        int shelfBookCount = 0;
        List<ShelfEntity> shelves = shelfService.findByBookcaseId(b.getBookcaseId());  // ❌ Query 1

        for (ShelfEntity s : shelves) {
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());  // ❌ Query 2 (nested!)
            shelfBookCount += bookList.size();
        }

        options.put(bookcaseRowFormater(b, shelfBookCount), b.getBookcaseId().toString());
    }
    return options;
}
```

**Problem:**
- 1 query to fetch all bookcases
- For each bookcase: 1 query to fetch shelves → **N queries**
- For each shelf: 1 query to count books → **N × M queries**

If you have 10 bookcases with 5 shelves each:
- **1 + 10 + (10 × 5) = 61 database queries** just to display a menu!

**Solution:**
Use a **single aggregate query** with JOINs and GROUP BY:

```java
// In BookcaseRepository.java (add this method)
@Query("""
    SELECT new com.penrose.bibby.library.bookcase.BookcaseSummary(
        bc.bookcaseId,
        bc.bookcaseLabel,
        bc.shelfCapacity,
        COUNT(b.bookId)
    )
    FROM BookcaseEntity bc
    LEFT JOIN ShelfEntity s ON s.bookcaseId = bc.bookcaseId
    LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
    GROUP BY bc.bookcaseId, bc.bookcaseLabel, bc.shelfCapacity
    ORDER BY bc.bookcaseLabel ASC
    """)
List<BookcaseSummary> findBookcaseSummaries();
```

Now **1 query** replaces 61.

---

## Good Pattern: Using Projections in CLI Commands

**BookcaseCommands.java:96-104** shows the **correct approach**:

```java
public void selectShelf(Long bookCaseId) {
    List<ShelfSummary> shelfSummaries = shelfService.getShelfSummariesForBookcase(bookCaseId);

    Map<String, String> bookShelfOptions = new LinkedHashMap<>();
    for (ShelfSummary s : shelfSummaries) {
        bookShelfOptions.put(String.format(
            "%-10s    \u001B[38;5;197m%-2d\u001B[22m\u001B[38;5;38m Books \u001B[0m",
            s.label(), s.bookCount()), s.shelfId().toString());
    }
    // ...
}
```

This uses `ShelfSummary` projection (from Section 25), which retrieves shelf label + book count in **one efficient query** with GROUP BY.

**ShelfRepository.java:15-27**
```java
@Query("""
    SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
        s.shelfId, s.shelfLabel, COUNT(b.bookId)
    )
    FROM ShelfEntity s
    LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
    WHERE s.bookcaseId = :bookcaseId
    GROUP BY s.shelfId, s.shelfLabel
    ORDER BY s.shelfPosition ASC
    """)
List<ShelfSummary> findShelfSummariesByBookcaseId(@Param("bookcaseId") Long bookcaseId);
```

✅ **One query**, clean projection, no loops.

---

## Dangerous Optional.get() Calls

**BookCommands.java:361-363**
```java
Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
System.out.println("\nBook Was Found \nBookcase: " + bookcaseEntity.get().getBookcaseLabel() + "...");
```

❌ **Problem:** Calls `.get()` without checking `.isPresent()`

If `shelfEntity` is empty → **NoSuchElementException** crashes the CLI.

**Fix:**
```java
Optional<ShelfEntity> shelfOpt = shelfService.findShelfById(bookEntity.getShelfId());
if (shelfOpt.isEmpty()) {
    System.out.println("Book has no assigned shelf.");
    return;
}

ShelfEntity shelf = shelfOpt.get();
Optional<BookcaseEntity> bookcaseOpt = bookcaseService.findBookCaseById(shelf.getBookcaseId());
if (bookcaseOpt.isEmpty()) {
    System.out.println("Shelf's bookcase not found.");
    return;
}

BookcaseEntity bookcase = bookcaseOpt.get();
System.out.println("Bookcase: " + bookcase.getBookcaseLabel() + " | Shelf: " + shelf.getShelfLabel());
```

Or use `.orElseThrow()` with a custom exception:
```java
ShelfEntity shelf = shelfOpt.orElseThrow(() -> new EntityNotFoundException("Shelf not found"));
```

---

## Command vs Service Separation

✅ **Good:** Commands delegate to services

**BookCommands.java:242-252**
```java
BookEntity bookEnt = bookService.findBookByTitle(title);
if (bookEnt == null) {
    System.out.println("Book Not Found In Library");
} else {
    Long shelfId = Long.parseLong(result.getContext().get("bookshelf", String.class));
    bookEnt.setShelfId(shelfId);
    bookService.updateBook(bookEnt);
    System.out.println("Added Book To the Shelf!");
}
```

Commands handle:
- User interaction (prompts, menus)
- Output formatting (colors, messages)
- Flow orchestration (which service to call when)

Services handle:
- Business logic
- Database operations
- Validation

❌ **Bad:** Commands directly call repositories (violates layer boundaries)

Never inject repositories into command classes. Always go through services.

---

## UX Enhancements in Bibby

Bibby includes several **delightful UX touches**:

### 1. ANSI Color Codes

**BookCommands.java:408**
```java
options.put("Show all books  — \u001B[32mView the complete library\n\u001B[0m", "all");
```

`\u001B[32m` = green text, `\u001B[0m` = reset to default

### 2. Thread.sleep() for Pacing

**BookCommands.java:120-127**
```java
System.out.println("\n\u001B[36m</>\033[0m: Ah, a brand-new book...");
Thread.sleep(1750);
System.out.printf("\u001B[36m</>\033[0m:'%s', right?", title);
Thread.sleep(2350);
System.out.println("\n\u001B[36m</>\033[0m: I'll handle adding it to the database...");
Thread.sleep(3800);
```

Creates a **conversational, library-assistant feel** instead of instant output.

### 3. ASCII Art & Personality

**BookCommands.java:482-486**
```java
System.out.println(
    """
    \u001B[38;5;63m  .---.
    \u001B[38;5;63m (* @ *)  \u001B[36m"This one's already off the shelf. No double-dipping on checkouts."
    \u001B[38;5;63m  \\|=|/
    """);
```

A friendly robot librarian character!

### 4. Custom Loading Bars

**LoadingBar.java:5-14**
```java
public static void showProgressBar(String taskName, int totalSteps, int delayMs) throws InterruptedException {
    System.out.println(taskName);
    for (int i = 0; i <= totalSteps; i++) {
        int percent = (i * 100) / totalSteps;
        String bar = "\uD83D\uDFE9".repeat(i) + " ".repeat(totalSteps - i);
        System.out.print("\r[" + bar + "] " + percent + "%");
        Thread.sleep(delayMs);
    }
    System.out.println("\n✅ Done!");
}
```

Used in **BookCommands.java:173, 285, 326, 352** to show fake progress for database operations.

---

## Testing Spring Shell Commands

Use `@SpringBootTest` with `ShellTestClient`:

**BookCommandsTest.java** (example to create)
```java
@SpringBootTest
class BookCommandsTest {

    @Autowired
    private ShellTestClient client;

    @Test
    void testBookAddCommand() {
        // Simulate user input
        ShellTestClient.InteractiveShellSession session = client
            .interactive()
            .run();

        session.write("book add");
        session.awaitCompletion();

        // Verify prompt appears
        assertThat(session.screen().lines())
            .anyMatch(line -> line.contains("Book Title"));

        // Provide inputs
        session.write("Clean Code");
        session.write("1");  // Number of authors
        session.write("Robert");  // First name
        session.write("Martin");  // Last name

        session.awaitCompletion();

        // Verify book was created
        assertThat(bookRepository.findByTitle("Clean Code")).isPresent();
    }
}
```

For integration tests, mock the services:

```java
@SpringBootTest
@MockBean(BookService.class)
class BookCommandsIntegrationTest {

    @Autowired
    private BookService bookService;

    @Test
    void testSearchByTitle() {
        // Given
        when(bookService.findBookByTitle("Refactoring"))
            .thenReturn(new BookEntity("Refactoring", ...));

        // When
        client.interactive().run()
            .write("book search")
            .write("title")
            .write("Refactoring")
            .awaitCompletion();

        // Then
        verify(bookService).findBookByTitle("Refactoring");
    }
}
```

---

## Summary: Bibby's Spring Shell Implementation

| Aspect | Grade | Explanation |
|--------|-------|-------------|
| **@Command Usage** | A | Clean class-level and method-level annotations with descriptive help text |
| **ComponentFlow** | A | Excellent use of interactive flows with menus, string inputs, and chaining |
| **Dependency Injection** | A | Proper constructor injection for ComponentFlow.Builder and services |
| **Service Separation** | A | Commands delegate to services, no business logic in CLI layer |
| **UX Design** | A+ | Outstanding use of colors, delays, ASCII art, and personality |
| **Performance** | D | Critical N+1 query bug in `bookCaseOptions()` (61 queries instead of 1) |
| **Error Handling** | C | Multiple unsafe `.get()` calls without `.isPresent()` checks |
| **Validation** | D | No input validation (e.g., parsing "abc" as integer crashes) |
| **Projections** | A | Good use of `ShelfSummary` and `BookSummary` in `selectShelf()` |
| **Overall** | B | Delightful UX undermined by performance and safety issues |

---

## Action Items

### Immediate (Critical)

1. **Fix N+1 Query in bookCaseOptions()** — Create `BookcaseSummary` projection with single aggregate query
2. **Add Optional Safety** — Replace all `.get()` with `.orElseThrow()` or `.isPresent()` checks
3. **Add Input Validation** — Validate numeric inputs before `Integer.parseInt()`, add try-catch blocks

### Short-Term (Quality)

4. **Extract UX Logic** — Move ASCII art, color codes, and messages to constants or resource files
5. **Add Unit Tests** — Test each command with `ShellTestClient` and mocked services
6. **Handle Edge Cases** — What if user enters empty string? Non-existent book? Already checked-out book?

### Long-Term (Enhancement)

7. **Add @Option Parameters** — Support `book add --title "Clean Code" --author "Martin"` for non-interactive use
8. **Implement Validation Groups** — Use Bean Validation (Section 28) for input DTOs
9. **Add Transaction Management** — Ensure `check-out` and `check-in` are atomic (Section 29)

---

## What You've Learned

✅ How `@Command` registers CLI commands with Spring Shell
✅ How `ComponentFlow` creates interactive multi-step user experiences
✅ How to inject `ComponentFlow.Builder` and reuse it with `.clone()`
✅ How to use `withStringInput()` and `withSingleItemSelector()` for prompts
✅ How to build option maps with `LinkedHashMap` for menu ordering
✅ How projections improve CLI performance (`ShelfSummary` vs nested loops)
✅ Why N+1 queries happen in CLI code and how to fix them
✅ Why `.get()` without `.isPresent()` is dangerous
✅ How to separate concerns (commands orchestrate, services execute)
✅ How to test Spring Shell commands with `ShellTestClient`

**Next Up:** Section 31 (Comprehensive Code Review) — We'll consolidate all findings from Sections 1-30 into a unified roadmap for improving Bibby's codebase.
