# Section 14: Smells and Heuristics
**Clean Code Principle:** *"A comprehensive catalog of code smells helps you recognize and eliminate quality issues systematically."*

---

## 📚 Principles Overview

### What Are Code Smells?

**Code smells** are surface indicators of deeper problems in your code. They're not bugs—the code works—but they suggest design issues that make code harder to maintain.

Uncle Bob's Chapter 17 in Clean Code provides a comprehensive catalog of smells and heuristics. This section applies that catalog to **your actual Bibby codebase**.

**Key insight:** Code smells are like medical symptoms. A cough doesn't tell you what's wrong, but it tells you something IS wrong. Similarly, long methods don't tell you the exact problem, but they indicate poor design.

### The Categories

Uncle Bob organizes smells into categories:

1. **Comments** - When comments become necessary evils
2. **Environment** - Build, test, and deployment issues
3. **Functions** - Method-level smells
4. **General** - Cross-cutting design issues
5. **Names** - Naming problems
6. **Tests** - Testing anti-patterns

---

## 🔍 Your Code Smells Catalog

This section consolidates **every issue** we've identified across Sections 1-13, organized by category for easy reference.

---

## 💬 COMMENTS

### C1: Inappropriate Information

**Smell:** Comments containing metadata that belongs elsewhere.

**Your Examples:**
```java
// TODO: Add data seeding logic here if needed
// StartupRunner.java:16
```

**Problem:** TODOs are task tracking, not code documentation. Use GitHub Issues instead.

**Fix:** Create GitHub issue, remove TODO comment.

---

### C2: Redundant Comments

**Smell:** Comments that describe what the code obviously does.

**Your Examples:**
```java
// LinkedHashMap keeps insertion order so the menu shows in the order you add them
Map<String, String> options = new LinkedHashMap<>();
// This comment appears in 3+ locations!
```

**Problem:**
- If code needs this comment, the code should be clearer
- Duplicated 3+ times = missing abstraction

**Fix:** Extract to method `createOrderedOptions()` with clear name.

---

### C3: Commented-Out Code

**Smell:** Dead code left in comments.

**Your Examples:**
```java
//#spring.jpa.hibernate.ddl-auto=create-drop
// application.properties:6
```

**Problem:** Clutters codebase, confuses developers.

**Fix:** Delete it. Git preserves history.

---

## 🌍 ENVIRONMENT

### E1: Build Requires More Than One Step

**Status:** ✅ Good - Single Maven command builds everything

**Your build:**
```bash
mvn clean install
```

**This is correct.** No issues here.

---

### E2: Tests Require More Than One Step

**Status:** ⚠️ No real tests to run

**Current state:** Only empty test skeleton and context loading test.

**Fix:** After adding tests (Section 8 action items):
```bash
mvn test  # Should be this simple
```

---

### E3: Security - Hardcoded Credentials

**Smell:** Credentials in version control.

**Your Example:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5332/amigos
spring.datasource.username=amigoscode  # ❌ IN GIT!
spring.datasource.password=password    # ❌ IN GIT!
```

**Problem:** Security breach if pushed to GitHub.

**Fix:** Environment variables (covered in Section 10).

---

## 🔧 FUNCTIONS

### F1: Too Many Arguments

**Smell:** Methods with 3+ parameters.

**Your Examples:**
```java
public BookCommands(ComponentFlow.Builder componentFlowBuilder,
                    BookService bookService,
                    BookController bookController,
                    BookcaseService bookcaseService,
                    ShelfService shelfService,
                    AuthorService authorService) {
    // 6 parameters!
}
```

**Problem:** Hard to understand, hard to call, fragile.

**Fix:** This is constructor injection (good), but consider if all dependencies are needed.

---

### F2: Output Arguments

**Smell:** Methods that modify their arguments.

**Your Example:**
```java
public void setAuthors(AuthorEntity authors) {
    this.authors.add(authors);  // Misleading name - it ADDS!
}
```

**Problem:** Name says "set" but it modifies existing collection.

**Fix:** Rename to `addAuthor()` or return new collection (immutable).

---

### F3: Flag Arguments

**Smell:** Boolean parameters that control behavior.

**Status:** ✅ Good - No flag arguments found

**Good example of what to avoid:**
```java
// ❌ BAD
public void render(boolean isSuite) {
    if (isSuite) {
        // render suite
    } else {
        // render single test
    }
}

// ✅ GOOD
public void renderSuite() { ... }
public void renderSingleTest() { ... }
```

---

### F4: Dead Function

**Smell:** Unused methods.

**Your Examples:**
```java
// BookCommands.java:256
public void searchByAuthorVoice(){
    List<String> searchResponses = new ArrayList<>();
    // Empty method - never called!
}
```

**Problem:** Clutters codebase, confuses developers.

**Fix:** Delete it.

---

## 🎯 GENERAL

### G1: Multiple Languages in One Source File

**Smell:** Mixing languages (Java + SQL + HTML) in one file.

**Your Example:**
```java
System.out.println("""
    \u001B[38;5;63m  .---.
    \u001B[38;5;63m (* @ *)  \u001B[36m "Try the Mystery Section."
    \u001B[38;5;63m  \\|=|/
    """);
// Java file contains ASCII art + ANSI codes
```

**Problem:** Hard to maintain, test, or change.

**Fix:** Extract to resource file or dedicated formatter class.

---

### G2: Obvious Behavior Is Unimplemented

**Smell:** Methods don't do what their name promises.

**Your Example:**
```java
public void searchByAuthor() throws InterruptedException {
    // User enters "George Orwell"
    // ❌ Returns hardcoded "Leo" books instead!
    System.out.println("[12] My Life Decoded: The Story of Leo");
}
```

**Problem:** Method name promises search, but returns fake data.

**Fix:** Implement actual search or rename to `mockSearchByAuthor()`.

---

### G3: Incorrect Behavior at the Boundaries

**Smell:** Edge cases not handled.

**Your Examples:**
```java
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);  // Can return null!
    bookEntity.setBookStatus(...);  // ❌ CRASHES if null!
}
```

**Problem:** No null check = crash on invalid input.

**Fix:** Use Optional or throw custom exception.

---

### G4: Duplication (DRY Violation)

**Smell:** Copy-pasted code.

**Your Examples:**

**1. ComponentFlow pattern (20+ times):**
```java
ComponentFlow flow = componentFlowBuilder.clone()
    .withStringInput("input")
    .name("Enter something:_")
    .and()
    .build();
ComponentFlow.ComponentFlowResult result = flow.run();
String value = result.getContext().get("input");
// Repeated 20+ times across codebase!
```

**2. Thread.sleep() (25 times):**
```java
Thread.sleep(1000);  // Hardcoded 25+ times
```

**3. LinkedHashMap comment (3+ times):**
```java
// LinkedHashMap keeps insertion order so the menu shows in the order you add them
Map<String, String> options = new LinkedHashMap<>();
// Same comment in 3+ locations
```

**Fix:** Extract to PromptService, DelayService, and helper methods.

---

### G5: Code at Wrong Level of Abstraction

**Smell:** High-level and low-level code mixed.

**Your Example:**
```java
@Command(command = "add")
public void addBook() {
    // HIGH-LEVEL: User interaction
    ComponentFlow flow = componentFlowBuilder.clone()...

    // LOW-LEVEL: ComponentFlow API details
    .withStringInput("bookTitle")
    .name("Book Title:_")
    .and().build();

    // HIGH-LEVEL: Business logic
    bookService.createNewBook(...);

    // LOW-LEVEL: Thread management
    Thread.sleep(1000);
}
```

**Problem:** Mixing abstraction levels makes code hard to understand.

**Fix:** Commands should be high-level only. Delegate to services.

---

### G6: Base Classes Depending on Derivatives

**Status:** ✅ Good - Minimal inheritance, no violations

---

### G7: Too Much Information

**Smell:** Exposing too many methods/fields publicly.

**Your Example:**
```java
@Entity
public class BookEntity {
    // ALL getters/setters public
    public void setShelfId(Long shelfId) { ... }
    public void setBookStatus(String status) { ... }
    // Anyone can modify internal state!
}
```

**Problem:** No encapsulation. Business rules not enforced.

**Fix:** Add business methods, make setters package-private or remove them.

---

### G8: Dead Code

**Smell:** Unused variables, methods, classes.

**Your Examples:**
```java
// Empty class - never used
public class CatalogEntity {
}

// Variable created but never used (BookCommandsTest.java:12)
BookEntity bookEntity = null;

// Empty method (BookCommands.java:256)
public void searchByAuthorVoice() {
    List<String> searchResponses = new ArrayList<>();
}
```

**Fix:** Delete all of it.

---

### G9: Vertical Separation

**Smell:** Related code separated by unrelated code.

**Your Example:**
```java
public class BookCommands {
    // Field declared at top
    private final BookService bookService;

    // ... 400 lines of other code ...

    // Method using bookService way down here
    public void checkOutBook() {
        bookService.findBookByTitle(...);
    }
}
```

**Problem:** Hard to see relationship between field and usage.

**Fix:** Organize by feature, not by type. Or split large classes.

---

### G10: Inconsistency

**Smell:** Similar things done differently.

**Your Examples:**

**1. Naming inconsistency:**
```java
// Sometimes camelCase:
Long bookcaseId;

// Sometimes PascalCase:
Long BookCaseLabel;

// Sometimes mixed:
findByBookcaseId()
findBookCaseById()
```

**2. Method duplication:**
```java
// ShelfService.java
public List<ShelfEntity> getAllShelves(Long bookCaseId) { ... }
public List<ShelfEntity> findByBookcaseId(Long bookcaseId) { ... }
// Same method, different names!
```

**Fix:** Standardize on ONE convention.

---

### G11: Clutter

**Smell:** Unused imports, variables, methods.

**Your Examples:**
```java
// ShelfService.java - unused imports
import com.penrose.bibby.library.book.Book;
import com.penrose.bibby.library.bookcase.BookcaseEntity;
// These classes never used in file!
```

**Fix:** IDE cleanup (Ctrl+Shift+O in IntelliJ).

---

### G12: Artificial Coupling

**Smell:** Unrelated things coupled together.

**Your Example:**
```java
@Service
public class BookcaseService {
    private final ShelfRepository shelfRepository;  // ❌ Wrong layer!

    public String createNewBookCase(...) {
        // Creates shelves directly instead of calling ShelfService
        shelfRepository.save(shelfEntity);
    }
}
```

**Problem:** BookcaseService knows about shelves (should use ShelfService).

**Fix:** BookcaseService → ShelfService → ShelfRepository

---

### G13: Feature Envy

**Smell:** Method more interested in another class than its own.

**Your Example:**
```java
// BookCommands.java
private Map<String, String> bookCaseOptions() {
    for (BookcaseEntity b : bookcaseEntities) {
        int shelfBookCount = 0;
        for(ShelfEntity s : shelves){  // Digging into shelves
            List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());
            shelfBookCount += bookList.size();  // Counting books
        }
        // This logic belongs in BookcaseService!
    }
}
```

**Problem:** Command class doing BookcaseService's job.

**Fix:** Move to `BookcaseService.getTotalBookCount()`.

---

### G14: Selector Arguments

**Smell:** Arguments that select behavior (like flag arguments).

**Status:** ✅ Good - No selector arguments found

---

### G15: Obscured Intent

**Smell:** Code intent is unclear.

**Your Examples:**
```java
// What does this do? (BookCommands.java:227)
System.out.println("BOOK CASE ID: " + bookCaseId);
// Debug output? Production code? Logging?

// What are these numbers? (BookCommands.java)
Thread.sleep(2350);
Thread.sleep(3800);
// Why 2350ms? Magic numbers!
```

**Fix:** Extract to named constants or methods.

---

### G16: Misplaced Responsibility

**Smell:** Code in wrong class/layer.

**Your Examples:**

**1. Business logic in commands:**
```java
// BookCommands.java - counting books (business logic!)
for(ShelfEntity s : shelves){
    List<BookEntity> bookList = bookService.findBooksByShelf(s.getShelfId());
    shelfBookCount += bookList.size();
}
```

**2. Transaction in wrong layer:**
```java
// BookCommands.java - modifying entities directly
BookEntity bookEnt = bookService.findBookByTitle(title);
bookEnt.setShelfId(shelfId);  // Should be in service with @Transactional!
bookService.updateBook(bookEnt);
```

**Fix:** Move all business logic to service layer.

---

### G17: Inappropriate Static

**Smell:** Methods that should be instance methods made static.

**Status:** ✅ Mostly good - Static usage is appropriate

**Good example from your code:**
```java
public static void showProgressBar(...)  // Utility method - correctly static
```

---

### G18: Use Explanatory Variables

**Smell:** Complex expressions without intermediate variables.

**Your Example:**
```java
// Hard to read:
bookcaseEntity.get().getBookcaseLabel();

// Better:
BookcaseEntity bookcase = bookcaseEntity.get();
String label = bookcase.getBookcaseLabel();
```

**Fix:** Break complex expressions into named variables.

---

### G19: Function Names Should Say What They Do

**Smell:** Misleading or vague method names.

**Your Examples:**
```java
// Misleading (adds, doesn't set!)
public void setAuthors(AuthorEntity authors) {
    this.authors.add(authors);
}

// Vague (does what?)
public void updateBook(BookEntity bookEntity) {
    bookRepository.save(bookEntity);
}

// Unclear (options for what?)
private Map<String, String> bookCaseOptions() {
    // Builds menu options with book counts...
}
```

**Fix:**
```java
public void addAuthor(AuthorEntity author) { ... }
public void saveBook(BookEntity book) { ... }
public Map<String, String> buildBookcaseMenuOptionsWithBookCounts() { ... }
```

---

### G20: Understand the Algorithm

**Smell:** Code that works but author doesn't understand why.

**Your Example:**
```java
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(
        bookRepository.findByTitleIgnoreCase(title)
    );
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    for(BookEntity b : bookEntities){
        System.out.println(b.getTitle());  // Why print here?
    }

    if(bookEntity.isEmpty()){
        return null;  // Why query twice?
    }
    return bookEntity.get();
}
```

**Problem:** Queries database twice, prints titles (why?), returns null instead of Optional.

**Fix:** Understand what you're trying to achieve, then implement cleanly.

---

### G21: Make Logical Dependencies Physical

**Smell:** Code depends on something implicitly.

**Your Example:**
```java
// Depends on ShelfService being injected, but no explicit check
@Autowired
ShelfService shelfService;  // Field injection - dependency implicit

@Override
public void run(String... args) throws Exception {
    shelfService.getAllShelves(1L);  // Assumes shelfService != null
}
```

**Fix:** Use constructor injection (makes dependency explicit).

---

### G22: Prefer Polymorphism to If/Else or Switch/Case

**Your Example:**
```java
if (searchType.equalsIgnoreCase("author")){
    searchByAuthor();
} else if(searchType.equalsIgnoreCase("title")){
    searchByTitle();
}
```

**Better (with polymorphism):**
```java
interface SearchStrategy {
    void search();
}

class AuthorSearchStrategy implements SearchStrategy { ... }
class TitleSearchStrategy implements SearchStrategy { ... }

Map<String, SearchStrategy> strategies = Map.of(
    "author", new AuthorSearchStrategy(),
    "title", new TitleSearchStrategy()
);

strategies.get(searchType).search();
```

**Note:** For 2 cases, if/else is fine. Use polymorphism when you have 3+ cases.

---

### G23: Follow Standard Conventions

**Status:** ⚠️ Mixed

**Good:**
- Java naming conventions (mostly)
- Spring Boot conventions
- Maven standard directory structure

**Bad:**
- Inconsistent field naming (`bookcaseId` vs `bookCaseLabel`)
- Mixed transaction boundaries
- No package-private modifiers

**Fix:** Adopt checkstyle/PMD rules, enforce with CI.

---

### G24: Replace Magic Numbers with Named Constants

**Smell:** Hardcoded numbers without explanation.

**Your Examples:**
```java
Thread.sleep(1000);   // What's 1000?
Thread.sleep(2350);   // Why 2350?
Thread.sleep(3800);   // Why 3800?

// Better:
private static final int SHORT_DELAY_MS = 1000;
private static final int MEDIUM_DELAY_MS = 2350;
private static final int LONG_DELAY_MS = 3800;
```

---

### G25: Be Precise

**Smell:** Vague decisions, assumptions not validated.

**Your Examples:**
```java
// Assumes Optional contains value (not precise!)
bookcaseEntity.get().getBookcaseLabel();

// Better:
bookcaseEntity.orElseThrow(() -> new BookcaseNotFoundException())
    .getBookcaseLabel();
```

---

### G26: Structure Over Convention

**Smell:** Relying on naming conventions instead of structure.

**Your Example:**
```java
// All entities end in "Entity" - convention, not enforced
public class BookEntity { ... }
public class AuthorEntity { ... }

// Better: Use inheritance
public abstract class BaseEntity { ... }
public class Book extends BaseEntity { ... }
```

**Note:** Your current approach is actually fine for JPA entities.

---

### G27: Encapsulate Conditionals

**Smell:** Complex boolean expressions inline.

**Your Example:**
```java
if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
    // Hard to read!
}

// Better:
private boolean isAvailable(BookEntity book) {
    return !book.getBookStatus().equals(BookStatus.CHECKED_OUT.toString());
}

if (isAvailable(bookEntity)) {
    // Much clearer!
}
```

---

### G28: Avoid Negative Conditionals

**Smell:** Using `!` makes logic harder to follow.

**Your Example:**
```java
if (!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
    // Double negative hard to read
}

// Better:
if (bookEntity.getBookStatus().equals(BookStatus.AVAILABLE.toString())) {
    // Positive condition clearer
}
```

---

### G29: Functions Should Do One Thing

**Smell:** Methods with multiple responsibilities.

**Your Example:**
```java
@Command(command = "add")
public void addBook() throws InterruptedException {
    // 1. Collect input
    // 2. Validate input
    // 3. Create book
    // 4. Display confirmation
    // 5. Offer recommendation
    // 81 lines doing 5 different things!
}
```

**Fix:** Extract each responsibility to separate method (covered in Section 13).

---

### G30: Hidden Temporal Couplings

**Smell:** Methods must be called in specific order, but order isn't enforced.

**Your Example:**
```java
// These MUST be called in order, but nothing enforces it:
BookEntity book = bookService.findBookByTitle(title);  // 1. Find
book.setShelfId(shelfId);                              // 2. Modify
bookService.updateBook(book);                          // 3. Save
```

**Better:**
```java
// Enforce order with single method
@Transactional
public void moveBookToShelf(String title, Long shelfId) {
    BookEntity book = findBookByTitle(title);
    book.setShelfId(shelfId);
    saveBook(book);
}
```

---

### G31: Don't Be Arbitrary

**Smell:** Inconsistent choices without reason.

**Your Examples:**
- Sometimes `bookcaseId`, sometimes `bookCaseLabel`
- Sometimes returning `Optional`, sometimes returning `null`
- Sometimes `findById`, sometimes `findBookById`

**Fix:** Pick ONE convention and stick to it.

---

### G32: Encapsulate Boundary Conditions

**Smell:** `+1`, `-1` scattered throughout code.

**Status:** ✅ Good - No boundary condition issues found

---

### G33: Functions Should Descend Only One Level of Abstraction

**Already covered in G5.**

---

### G34: Keep Configurable Data at High Levels

**Smell:** Configuration scattered throughout code.

**Your Examples:**
```java
// Hardcoded delays throughout
Thread.sleep(1000);
Thread.sleep(2000);
Thread.sleep(3000);

// Better: Configurable at high level
@Value("${app.delay.short}")
private int shortDelayMs;
```

---

### G35: Avoid Transitive Navigation (Law of Demeter)

**Smell:** Chain of calls (train wrecks).

**Your Examples:**
```java
// Calling through multiple objects
bookcaseEntity.get().getBookcaseLabel();

shelfEntity.get().getBookcaseId();

// Better:
String label = bookcase.getLabel();
Long bookcaseId = shelf.getBookcaseId();
```

---

## 📝 NAMES

### N1: Choose Descriptive Names

**Your Examples:**
```java
// Vague
ComponentFlow flow;
ComponentFlow.ComponentFlowResult res;

// Better
ComponentFlow userInputFlow;
ComponentFlow.ComponentFlowResult userInputResult;
```

---

### N2: Choose Names at Appropriate Level of Abstraction

**Your Example:**
```java
// Too implementation-specific
private Map<String, String> bookCaseOptions()

// Better (describes what, not how)
private Map<String, String> getBookcaseMenuChoices()
```

---

### N3: Use Standard Nomenclature Where Possible

**Status:** ✅ Good - Following Java conventions mostly

---

### N4: Unambiguous Names

**Your Example:**
```java
// Ambiguous (updates what? how?)
public void updateBook(BookEntity bookEntity)

// Clearer
public void saveBookChanges(BookEntity bookEntity)
```

---

### N5: Use Long Names for Long Scopes

**Your Examples:**
```java
// Short scope (loop) - short name OK
for(int i = 0; i < numAuthors; i++)

// Long scope (field) - should be longer
private final ComponentFlow.Builder componentFlowBuilder;  // Good!
```

---

### N6: Avoid Encodings

**Your Example:**
```java
// "Entity" suffix is a form of encoding
BookEntity
AuthorEntity

// But this is acceptable for JPA (standard convention)
```

---

### N7: Names Should Describe Side Effects

**Your Example:**
```java
// Misleading (doesn't just set, also ADDS)
public void setAuthors(AuthorEntity authors) {
    this.authors.add(authors);
}

// Honest
public void addAuthor(AuthorEntity author) {
    this.authors.add(author);
}
```

---

## ✅ TESTS

### T1: Insufficient Tests

**Status:** 🔴 CRITICAL

**Current:** 0 meaningful tests (0% coverage)
**Target:** 70-80% coverage

**Missing tests for:**
- All service methods
- All repository queries
- All command flows
- Integration tests

---

### T2: Use a Coverage Tool

**Status:** ❌ Not set up

**Fix:** Add JaCoCo to pom.xml (covered in Section 8).

---

### T3: Don't Skip Trivial Tests

**Your Example:**
```java
// Empty test - worse than no test!
@Test
public void searchByTitleTest(){
    BookEntity bookEntity = null;
}
```

**Fix:** Write real test or delete it.

---

### T4: An Ignored Test Is a Question About an Ambiguity

**Status:** N/A - No @Ignored tests

---

### T5: Test Boundary Conditions

**Missing tests:**
- What happens when title is null?
- What happens when title is empty string?
- What happens when book doesn't exist?
- What happens when multiple books match?

---

### T6: Exhaustively Test Near Bugs

**Once you find a bug:** Write tests for similar scenarios.

Example: If `checkInBook()` crashes on null, test ALL methods that could receive null.

---

### T7: Patterns of Failure Are Revealing

**When tests fail:** Look for patterns. If all "null input" tests fail, you have a systemic input validation problem.

---

### T8: Test Coverage Patterns Can Be Revealing

**Example:** If all error-handling tests fail, you have no error handling (which is true for your codebase!).

---

### T9: Tests Should Be Fast

**Your issue:** Thread.sleep() 25 times = slow tests.

**Fix:** DelayService with configurable multiplier (0 for tests).

---

## 📊 Your Code Smells Summary

### Critical (Fix Immediately)

| Smell | Count | Impact | Priority |
|-------|-------|--------|----------|
| T1: No tests | 0% coverage | Can't refactor safely | 🔴 CRITICAL |
| G4: ComponentFlow duplication | 20+ times | 100+ lines duplicated | 🔴 CRITICAL |
| G3: Null pointer risks | 10+ locations | Production crashes | 🔴 CRITICAL |
| E3: Hardcoded credentials | 1 file | Security breach | 🔴 CRITICAL |

### High Priority (Fix This Month)

| Smell | Count | Impact | Priority |
|-------|-------|--------|----------|
| G4: Thread.sleep duplication | 25 times | Slow, untestable | 🔴 HIGH |
| G4: System.out.println | 47 times | No logging | 🔴 HIGH |
| G13: Feature envy | 5+ locations | Wrong layer | 🔴 HIGH |
| G29: Long methods | 3 methods | Hard to maintain | 🔴 HIGH |

### Medium Priority (Fix This Quarter)

| Smell | Count | Impact | Priority |
|-------|-------|--------|----------|
| G8: Dead code | 5+ items | Clutter | 🟡 MEDIUM |
| N7: Misleading names | 10+ methods | Confusion | 🟡 MEDIUM |
| G10: Inconsistency | Throughout | Cognitive load | 🟡 MEDIUM |
| G7: Too much information | All entities | No encapsulation | 🟡 MEDIUM |

---

## 🎯 Quick Reference Checklist

Use this checklist when reviewing code (yours or others'):

### Comments
- [ ] No redundant comments
- [ ] No commented-out code
- [ ] No TODOs (use issue tracker)

### Functions
- [ ] < 20 lines per method
- [ ] One level of abstraction
- [ ] Do one thing
- [ ] Descriptive names
- [ ] < 3 parameters

### Names
- [ ] Intention-revealing
- [ ] Pronounceable
- [ ] Searchable
- [ ] No encoding
- [ ] Describe side effects

### General
- [ ] No duplication
- [ ] Code at right level
- [ ] Error handling present
- [ ] Boundaries clearly defined
- [ ] Dependencies injected

### Tests
- [ ] Tests exist!
- [ ] Tests are fast
- [ ] One assert per concept
- [ ] Given-When-Then structure
- [ ] Test edge cases

---

## 🛠️ How to Use This Document

### Daily Code Reviews

Before committing code, scan for these top 5 smells:
1. **Duplication** - Is this code copy-pasted?
2. **Long methods** - Is this method > 20 lines?
3. **Poor names** - Does the name reveal intent?
4. **Missing tests** - Did I write tests?
5. **Wrong layer** - Is business logic in commands?

### Weekly Cleanup Sessions

Pick ONE category, scan codebase:
- **Week 1:** Functions (extract long methods)
- **Week 2:** Names (rename unclear variables)
- **Week 3:** Duplication (extract common code)
- **Week 4:** Tests (add missing tests)

### Before Pull Requests

Run through the checklist above. Fix any 🔴 CRITICAL smells.

---

## 📚 Further Study

### Books
- **"Refactoring"** - Martin Fowler
  *Comprehensive catalog of refactoring patterns.*

- **"Clean Code"** - Robert C. Martin (Chapter 17)
  *The source of this smells catalog.*

### Tools
- **Checkstyle** - Enforces coding standards
- **PMD** - Detects code smells automatically
- **SonarQube** - Comprehensive code quality platform
- **JaCoCo** - Test coverage tool

### Online Resources
- **Refactoring Guru:** https://refactoring.guru/refactoring/smells
- **Code Smell Catalog:** https://martinfowler.com/bliki/CodeSmell.html

---

## 💭 Mentor's Final Thoughts

Leo, this section is your **comprehensive reference guide** to code quality. I've cataloged **every single issue** we've found across 13 sections of analysis.

The numbers are sobering:
- **0% test coverage** (T1 - Critical)
- **100+ lines of ComponentFlow duplication** (G4 - Critical)
- **25 Thread.sleep() calls** (G4 - High)
- **47 System.out.println calls** (High)
- **10+ null pointer risks** (G3 - Critical)

But here's the good news: **We know exactly what to fix.** This isn't a mystery. The roadmap is clear:

**Week 1-2:** Create abstractions (PromptService, DelayService, ConsoleOutput)
**Week 3-4:** Refactor commands to use new services
**Week 5-6:** Move business logic to services, add transactions
**Week 7-8:** Write comprehensive tests

After 8 weeks of focused work, your codebase will be **unrecognizable**. From code with critical smells to clean, professional code.

**This section is your North Star.** Bookmark it. Reference it daily. Use the checklist before every commit.

**Congratulations!** You've completed all **14 Clean Code Fundamentals** sections! That's 46% of the entire mentorship.

Next up: **Spring Framework Mastery** (Sections 15-24), where you'll learn:
- Spring Boot best practices
- Spring Shell mastery
- REST API design
- Spring Data JPA optimization
- Production-ready patterns

You've built a solid foundation. Now let's build something amazing on top of it.

**Welcome to Spring Framework Mastery.**

— Your Mentor

---

**Next:** Section 15 - Spring Boot Fundamentals & Best Practices
**Previous:** [Section 13 - Successive Refinement](./13-successive-refinement.md)
**Home:** [Master Index](./00-master-index.md)
