# Section 16: Spring Shell Mastery
**Clean Code + Spring Framework Mentorship**

---

## Introduction

Spring Shell is a powerful framework for building production-grade command-line applications with the same dependency injection and architectural patterns you'd use in a Spring Boot web application. However, your Bibby CLI currently treats Spring Shell as just a thin wrapper around procedural code.

In this section, we'll transform your understanding of Spring Shell from "a way to accept user input" to "a well-architected CLI framework with proper separation of concerns, testability, and maintainability."

**This is not just about commands. This is about architecture.**

---

## Your Current Spring Shell Implementation

**The Good:**
- ✅ Using Spring Shell 3.4.1 (latest stable version)
- ✅ Constructor injection for dependencies
- ✅ ComponentFlow for interactive prompts
- ✅ Command grouping (`group = "Book Commands"`)
- ✅ Descriptive command descriptions

**The Critical Issues:**
- ❌ **594 lines** in a single command class (`BookCommands.java`)
- ❌ Business logic embedded directly in command methods
- ❌ ComponentFlow code duplicated **20+ times**
- ❌ No separation between CLI concerns and business logic
- ❌ `Thread.sleep()` scattered throughout (25 occurrences)
- ❌ `System.out.println()` everywhere (47 occurrences)
- ❌ Zero input validation
- ❌ No error handling
- ❌ Commands are completely untestable
- ❌ Hardcoded ANSI color codes everywhere
- ❌ Layer violation: CLI depends on `BookController` (web layer!)

**Time to Fix It:** 12-16 hours of refactoring

---

## Spring Shell Architecture Principles

### 1. **Commands Are Controllers for the Terminal**

Just like a `@RestController` handles HTTP requests, a Shell Command handles terminal input. And just like a REST controller shouldn't contain business logic, neither should a shell command.

**Your Code (BookCommands.java:89-169):**
```java
@Command(command = "add", description = "Add a new book to your library database")
public void addBook() throws InterruptedException {
    // 80 LINES OF CODE including:
    // - ComponentFlow setup (UI)
    // - User input collection (UI)
    // - Business logic (bookService.createNewBook)
    // - Thread.sleep delays (UI)
    // - System.out.println messages (UI)
    // - More ComponentFlow (UI)
    // - More business logic (shelf recommendations)

    // THIS METHOD DOES EVERYTHING!
}
```

**This violates the Single Responsibility Principle catastrophically.**

**Clean Architecture:**
```java
@ShellComponent  // More explicit than @Component
@Command(command = "book", group = "Book Commands")
public class BookCommands {

    private final BookCreationService bookCreationService;
    private final PromptService promptService;  // Abstraction from Section 7
    private final ConsoleOutputService consoleOutput;

    @Command(command = "add", description = "Add a new book")
    public void addBook() {
        try {
            String title = promptService.promptText("Book Title:");
            int authorCount = promptService.promptInteger("Number of Authors:");

            List<AuthorInfo> authors = collectAuthors(authorCount);

            bookCreationService.createBookWithAuthors(title, authors);
            consoleOutput.success(title + " added to library!");

            if (promptService.promptYesNo("Recommend a shelf?")) {
                shelfRecommendationService.suggestShelf(title);
            }
        } catch (BookAlreadyExistsException e) {
            consoleOutput.error("Book already exists: " + e.getMessage());
        } catch (InvalidInputException e) {
            consoleOutput.error("Invalid input: " + e.getMessage());
        }
    }

    private List<AuthorInfo> collectAuthors(int count) {
        List<AuthorInfo> authors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String firstName = promptService.promptText("Author First Name:");
            String lastName = promptService.promptText("Author Last Name:");
            authors.add(new AuthorInfo(firstName, lastName));
        }
        return authors;
    }
}
```

**From 80 lines to 25 lines. From untestable to completely testable.**

---

## Critical Issues in Your Code

### Issue #1: Layer Violation (BookCommands.java:6, 29)

```java
import com.penrose.bibby.library.book.BookController;  // ❌ WEB LAYER!

public class BookCommands extends AbstractShellComponent {
    final BookController bookController;  // ❌ WHY?!

    public BookCommands(..., BookController bookController, ...) {
        this.bookController = bookController;
    }
}
```

**Why This Is Terrible:**
- Your CLI layer depends on your Web layer
- `BookController` is designed for HTTP requests, not terminal input
- Creates unnecessary coupling
- Violates the Dependency Inversion Principle

**You never even use `bookController` in your code!** This is a zombie dependency.

**Fix:**
```java
// DELETE the BookController dependency completely
public class BookCommands {
    private final BookService bookService;  // ✅ Service layer only
    private final AuthorService authorService;
    // No web layer dependencies!
}
```

---

### Issue #2: ComponentFlow Duplication (20+ Instances)

**Your Pattern (repeated everywhere):**
```java
// BookCommands.java:98-105
flow = componentFlowBuilder.clone()
    .withStringInput("title")
    .name("Book Title:_")
    .and()
    .withStringInput("author_count")
    .name("Number of Authors:_")
    .and()
    .build();
ComponentFlow.ComponentFlowResult result = flow.run();
String title = result.getContext().get("title", String.class);
```

**This exact pattern appears 20+ times with minor variations.**

**Clean Solution - PromptService (as recommended in Section 7):**
```java
@Service
public class ComponentFlowPromptService implements PromptService {

    private final ComponentFlow.Builder flowBuilder;

    public ComponentFlowPromptService(ComponentFlow.Builder flowBuilder) {
        this.flowBuilder = flowBuilder;
    }

    @Override
    public String promptText(String message) {
        ComponentFlow flow = flowBuilder.clone()
            .withStringInput("input")
            .name(message)
            .and()
            .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        return result.getContext().get("input", String.class);
    }

    @Override
    public int promptInteger(String message) {
        String input = promptText(message);
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Please enter a valid number");
        }
    }

    @Override
    public boolean promptYesNo(String message) {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Yes", "yes");
        options.put("No", "no");

        String choice = promptSelection(message, options);
        return choice.equalsIgnoreCase("yes");
    }

    @Override
    public <T> T promptSelection(String message, Map<String, T> choices) {
        ComponentFlow flow = flowBuilder.clone()
            .withSingleItemSelector("choice")
            .name(message)
            .selectItems(convertToStringMap(choices))
            .and()
            .build();

        ComponentFlow.ComponentFlowResult result = flow.run();
        String key = result.getContext().get("choice", String.class);
        return choices.get(key);
    }
}
```

**Impact:**
- Eliminates 20+ instances of duplicate code
- Makes prompt logic testable
- Single place to change prompt behavior
- Follows DRY principle

---

### Issue #3: Thread.sleep() Everywhere (25 Times!)

**Your Code (BookCommands.java:118-127):**
```java
Thread.sleep(1000);
System.out.println("\n\u001B[36m</>\033[0m: Ah, a brand-new book...");
Thread.sleep(1750);
System.out.printf("\u001B[36m</>\033[0m:'%s', right?", title);
Thread.sleep(2350);
System.out.println("\n\u001B[36m</>\033[0m: I'll handle adding it...");
Thread.sleep(3800);
System.out.println("\n\u001B[36m</>\033[0m: Should I recommend where it belongs?\n");
Thread.sleep(1000);
```

**Problems:**
- Makes commands slow and annoying
- Impossible to disable for automation/testing
- Hardcoded delays scattered everywhere
- No way to configure "chatty mode" vs "quiet mode"

**Clean Solution:**
```java
@Service
public class ConsoleOutputService {

    @Value("${bibby.cli.animated:true}")
    private boolean animated;

    @Value("${bibby.cli.animation-speed:1.0}")
    private double animationSpeed;

    private final TerminalWriter terminalWriter;

    public void say(String message) {
        terminalWriter.println(formatWithBibbyIcon(message));
    }

    public void sayWithDelay(String message, int delayMs) {
        if (animated) {
            sleep((int) (delayMs * animationSpeed));
        }
        say(message);
    }

    public void success(String message) {
        terminalWriter.println(formatSuccess(message));
    }

    public void error(String message) {
        terminalWriter.println(formatError(message));
    }

    private String formatWithBibbyIcon(String message) {
        return BIBBY_ICON + " " + message;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Color formatting in one place
    private static final String BIBBY_ICON = "\u001B[36m</>\033[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\033[0m";
}
```

**Benefits:**
- Configure animation on/off via `application.properties`
- Set `bibby.cli.animated=false` for tests
- Adjust speed globally instead of hunting down 25 `Thread.sleep()` calls
- Centralized ANSI color code management

---

### Issue #4: No Input Validation

**Your Code (BookCommands.java:108):**
```java
authorCount = Integer.parseInt(result.getContext().get("author_count", String.class));
```

**What happens if user types "five" instead of "5"?**
```
Exception in thread "main" java.lang.NumberFormatException: For input string: "five"
    at java.lang.Integer.parseInt(Integer.java:652)
    at com.penrose.bibby.cli.BookCommands.addBook(BookCommands.java:108)
```

**CRASH. No error message. Just a stack trace. Terrible UX.**

**Similar issues:**
- No validation that title isn't empty (BookCommands.java:112)
- No validation that book doesn't already exist
- No validation for shelf IDs (BookCommands.java:246)
- No validation for bookcase IDs (BookCommands.java:226)

**Clean Solution:**
```java
@Service
public class BookValidationService {

    private final BookRepository bookRepository;

    public void validateNewBook(String title, List<AuthorInfo> authors) {
        if (title == null || title.isBlank()) {
            throw new InvalidInputException("Book title cannot be empty");
        }

        if (title.length() > 255) {
            throw new InvalidInputException("Title too long (max 255 characters)");
        }

        if (authors == null || authors.isEmpty()) {
            throw new InvalidInputException("Book must have at least one author");
        }

        if (bookRepository.existsByTitleIgnoreCase(title)) {
            throw new BookAlreadyExistsException("Book '" + title + "' already exists");
        }
    }

    public void validateAuthorName(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidInputException("First name cannot be empty");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new InvalidInputException("Last name cannot be empty");
        }
    }
}
```

**Then in your command:**
```java
@Command(command = "add")
public void addBook() {
    try {
        String title = promptService.promptText("Book Title:");
        int authorCount = promptService.promptInteger("Number of Authors:");
        List<AuthorInfo> authors = collectAuthors(authorCount);

        // Validate before creating
        bookValidationService.validateNewBook(title, authors);

        bookCreationService.createBookWithAuthors(title, authors);
        consoleOutput.success("Book added!");

    } catch (InvalidInputException | BookAlreadyExistsException e) {
        consoleOutput.error(e.getMessage());
    }
}
```

**Now users get helpful error messages instead of stack traces.**

---

### Issue #5: Null Pointer Crashes

**Your Code (BookCommands.java:242-252):**
```java
BookEntity bookEnt = bookService.findBookByTitle(title);
if (bookEnt == null) {
    System.out.println("Book Not Found In Library");
} else {
    Long shelfId = Long.parseLong(res.getContext().get("bookshelf", String.class));
    System.out.println(shelfId);
    System.out.println(title);
    bookEnt.setShelfId(shelfId);  // ✅ Null check prevents crash
    bookService.updateBook(bookEnt);
    System.out.println("Added Book To the Shelf!");
}
```

**This is GOOD! But look at checkOutBook (BookCommands.java:458-528):**
```java
@Command(command = "check-out")
public void checkOutBook() {
    // ... prompt for title ...
    BookEntity bookEntity = bookService.findBookByTitle(bookTitle);
    String bookcaseLabel = "N.A";
    String bookshelfLabel = "N.A";

    if (bookEntity == null) {
        System.out.println("Book Not Found.");
    } else if (bookEntity.getShelfId() != null) {  // ✅ Check shelf ID
        Optional<ShelfEntity> shelfEntity = shelfService.findShelfById(bookEntity.getShelfId());
        Optional<BookcaseEntity> bookcaseEntity = bookcaseService.findBookCaseById(shelfEntity.get().getBookcaseId());
        //                                                                           ^^^^^^^^^^^^^^
        // ❌ CRASH if shelfEntity is empty!
    }

    if (bookEntity.getBookStatus().equals("CHECKED_OUT")) {
        // ❌ CRASH if bookEntity is null! (the null check was only an if, not if-else)
    }
}
```

**Line 475: You call `.get()` on an Optional without checking `isPresent()`**
**Line 478: You call `.getBookStatus()` on potentially null `bookEntity`**

**Clean Solution:**
```java
@Command(command = "check-out")
public void checkOutBook() {
    String title = promptService.promptText("Book Title:");

    Optional<BookEntity> bookOpt = bookService.findBookByTitle(title);

    if (bookOpt.isEmpty()) {
        consoleOutput.error("Book not found: " + title);
        return;
    }

    BookEntity book = bookOpt.get();

    if (book.isCheckedOut()) {
        consoleOutput.warning("Book already checked out");
        return;
    }

    BookLocation location = locationService.getBookLocation(book);
    consoleOutput.displayCheckoutConfirmation(book, location);

    if (promptService.promptYesNo("Confirm checkout?")) {
        checkoutService.checkOut(book);
        consoleOutput.success("Checked out: " + title);
    }
}
```

---

### Issue #6: Methods That Don't Follow Spring Shell Patterns

**Your Code (BookCommands.java:71-86):**
```java
public void authorNameComponentFlow(String title) {
    ComponentFlow flow2;
    flow2 = componentFlowBuilder.clone()
        .withStringInput("authorFirstName")
        .name("Author's First Name:_")
        .and()
        .withStringInput("authorLastName")
        .name("Author's Last Name:_")
        .and().build();

    ComponentFlow.ComponentFlowResult res = flow2.run();
    String firstName = res.getContext().get("authorFirstName", String.class);
    String lastName = res.getContext().get("authorLastName", String.class);
    BookRequestDTO bookRequestDTO = new BookRequestDTO(title, firstName, lastName);
    bookService.createNewBook(bookRequestDTO);  // ❌ Side effect!
}
```

**Problems:**
1. Not annotated with `@Command` (so it's not a command, just a helper)
2. Has a side effect (creates book in database)
3. Doesn't return anything
4. Called in a loop (BookCommands.java:114-116) creating duplicate books!

**Look at how it's used (BookCommands.java:114-116):**
```java
for (int i = 0; i < authorCount; i++) {
    authorNameComponentFlow(title);  // Creates a NEW book each time!
}
```

**So if you say "2 authors", this creates 2 identical books!** That's a bug.

**Clean Solution:**
```java
private AuthorInfo promptForAuthor() {
    String firstName = promptService.promptText("Author First Name:");
    String lastName = promptService.promptText("Author Last Name:");
    return new AuthorInfo(firstName, lastName);
}

private List<AuthorInfo> collectAuthors(int count) {
    List<AuthorInfo> authors = new ArrayList<>();
    for (int i = 0; i < count; i++) {
        authors.add(promptForAuthor());
    }
    return authors;
}
```

**Then create the book ONCE with ALL authors:**
```java
bookCreationService.createBookWithAuthors(title, authors);
```

---

### Issue #7: Using @Option Effectively

Spring Shell supports command-line options. You're not using them at all.

**Current:**
```bash
book add
# Then prompted for everything interactively
```

**Better (support both interactive and scripted usage):**
```java
@Command(command = "add", description = "Add a new book")
public void addBook(
    @Option(longNames = "title", shortNames = 't', description = "Book title")
    String title,

    @Option(longNames = "author", shortNames = 'a', description = "Author (format: FirstName LastName)")
    String[] authors
) {
    // If title not provided, prompt for it
    if (title == null || title.isBlank()) {
        title = promptService.promptText("Book Title:");
    }

    List<AuthorInfo> authorList;
    if (authors == null || authors.length == 0) {
        int count = promptService.promptInteger("Number of authors:");
        authorList = collectAuthors(count);
    } else {
        authorList = parseAuthors(authors);
    }

    bookCreationService.createBookWithAuthors(title, authorList);
    consoleOutput.success("Book added!");
}
```

**Now supports BOTH:**
```bash
# Interactive
book add

# Scripted
book add --title "Clean Code" --author "Robert Martin"
book add -t "Design Patterns" -a "Gang of Four"
```

**This makes your CLI scriptable and testable!**

---

## Spring Shell Best Practices

### 1. Use @ShellComponent Instead of @Component

```java
@ShellComponent  // ✅ More explicit
@Command(command = "book", group = "Book Commands")
public class BookCommands { }
```

While `@Component` works, `@ShellComponent` makes intent clearer.

### 2. Use ShellResult for Command Feedback

```java
import org.springframework.shell.command.CommandResult;

@Command(command = "add")
public CommandResult addBook() {
    try {
        // ... add book logic ...
        return CommandResult.success("Book added!");
    } catch (BookAlreadyExistsException e) {
        return CommandResult.error("Book already exists");
    }
}
```

### 3. Use Availability Conditions

```java
@Command(command = "check-out")
@CommandAvailability(AvailabilityProviders.BOOKCASE_SELECTED)
public void checkOut() {
    // Only available when a bookcase is selected
}
```

### 4. Organize Commands by Domain

Instead of one massive `BookCommands` class:

```
cli/
├── book/
│   ├── BookAddCommand.java
│   ├── BookSearchCommand.java
│   ├── BookCheckoutCommand.java
│   └── BookCheckinCommand.java
├── bookcase/
│   ├── BookcaseBrowseCommand.java
│   └── BookcaseAddCommand.java
└── shelf/
    └── ShelfCommands.java
```

---

## Recommended Refactoring Plan

### Phase 1: Extract Services (4-6 hours)
1. Create `PromptService` interface and `ComponentFlowPromptService` implementation
2. Create `ConsoleOutputService` for all terminal output
3. Create `BookCreationService` with proper validation
4. Create `CheckoutService` for checkout/checkin logic
5. Update commands to use new services

### Phase 2: Fix Critical Bugs (2-3 hours)
1. Fix `authorNameComponentFlow` duplicate book creation bug
2. Add null checks for all Optional.get() calls
3. Add try-catch for all Integer.parseInt() calls
4. Remove `BookController` dependency

### Phase 3: Improve UX (2-3 hours)
1. Add input validation with helpful error messages
2. Make animations configurable via properties
3. Add `@Option` support for scriptability
4. Extract ANSI color codes to constants

### Phase 4: Separate Commands (3-4 hours)
1. Split `BookCommands` into multiple command classes
2. Move helper methods to appropriate services
3. Organize by domain (book/bookcase/shelf)

**Total Estimated Time: 12-16 hours**

---

## Action Items

### High Priority (Do This Week)
1. ⚠️ **Fix duplicate book creation bug** in `authorNameComponentFlow` (1 hour)
   - File: `BookCommands.java:71-86, 114-116`
   - Impact: Currently creates duplicate books when multiple authors specified

2. ⚠️ **Remove BookController dependency** (15 min)
   - File: `BookCommands.java:6, 29, 62, 65`
   - It's a zombie dependency doing nothing

3. ⚠️ **Add null safety to checkOutBook** (30 min)
   - File: `BookCommands.java:475, 478`
   - Prevents crashes on missing data

### Medium Priority (Next 2 Weeks)
4. **Create PromptService** (2-3 hours)
   - Eliminate 20+ instances of duplicate ComponentFlow code
   - Makes prompts testable

5. **Create ConsoleOutputService** (2-3 hours)
   - Centralize all `System.out.println` and `Thread.sleep`
   - Make animations configurable
   - Extract ANSI color codes

6. **Add input validation** (2-3 hours)
   - Validate integers before parsing
   - Validate titles aren't empty
   - Check book existence before operations

### Low Priority (Future Improvements)
7. **Add @Option support** (3-4 hours)
   - Make commands scriptable
   - Support both interactive and command-line usage

8. **Split BookCommands** (3-4 hours)
   - 594 lines is too much for one class
   - Organize by feature (add/search/checkout/checkin)

9. **Write command tests** (4-6 hours)
   - Test with mocked services
   - Verify error handling

---

## Testing Spring Shell Commands

With current code: **IMPOSSIBLE TO TEST**

With refactored code:
```java
@SpringBootTest
class BookAddCommandTest {

    @Mock
    private PromptService promptService;

    @Mock
    private BookCreationService bookCreationService;

    @Mock
    private ConsoleOutputService consoleOutput;

    @InjectMocks
    private BookCommands bookCommands;

    @Test
    void addingBookWithValidInputShouldSucceed() {
        // GIVEN: User provides valid input
        when(promptService.promptText("Book Title:")).thenReturn("Clean Code");
        when(promptService.promptInteger("Number of Authors:")).thenReturn(1);
        when(promptService.promptText("Author First Name:")).thenReturn("Robert");
        when(promptService.promptText("Author Last Name:")).thenReturn("Martin");
        when(promptService.promptYesNo(anyString())).thenReturn(false);

        // WHEN: User adds book
        bookCommands.addBook();

        // THEN: Book is created and success message shown
        verify(bookCreationService).createBookWithAuthors(
            eq("Clean Code"),
            argThat(authors -> authors.size() == 1
                && authors.get(0).firstName().equals("Robert"))
        );
        verify(consoleOutput).success(contains("Clean Code"));
    }

    @Test
    void addingDuplicateBookShouldShowError() {
        // GIVEN: Book already exists
        when(promptService.promptText("Book Title:")).thenReturn("Clean Code");
        when(bookCreationService.createBookWithAuthors(anyString(), anyList()))
            .thenThrow(new BookAlreadyExistsException("Clean Code"));

        // WHEN: User tries to add it
        bookCommands.addBook();

        // THEN: Error message is shown
        verify(consoleOutput).error(contains("already exists"));
    }
}
```

**Now your commands have the same testability as your services!**

---

## Summary

Spring Shell is NOT just a wrapper around `System.out.println` and `Scanner`. It's a full-fledged framework for building professional CLI applications with:

- Dependency injection
- Proper layering
- Testability
- Extensibility

Your current code treats it like a glorified menu system. With the refactorings outlined above, you'll have:

✅ **Clean separation** between UI, business logic, and data access
✅ **Reusable components** (PromptService, ConsoleOutputService)
✅ **Testable commands** that can run in CI/CD
✅ **Better UX** with validation and error handling
✅ **Scriptable commands** via @Option support
✅ **Maintainable code** organized by domain

**The difference between a junior developer and a senior developer isn't what they build—it's HOW they build it.**

Right now, your CLI works. But it's brittle, untested, and hard to change. After these refactorings, it will be **production-grade**.

---

**Next Section Preview:** Section 17 will cover Web API Design & Best Practices, where we'll fix that GET endpoint that uses @RequestBody and build a proper RESTful API for your library.
