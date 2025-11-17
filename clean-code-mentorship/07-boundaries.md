# SECTION 7: BOUNDARIES
## Clean Code Chapter 8 Applied to Your Bibby Codebase

---

## Table of Contents
1. [Principles Overview](#principles-overview)
2. [Your Code Analysis](#your-code-analysis)
3. [Learning Tests](#learning-tests)
4. [Adapter Pattern Examples](#adapter-pattern-examples)
5. [Patterns You Should Recognize](#patterns-you-should-recognize)
6. [Action Items](#action-items)
7. [Key Takeaways](#key-takeaways)
8. [Further Study](#further-study)

---

## Principles Overview

### What Are Boundaries?

Boundaries are the places where your code meets code you don't control:
- Third-party libraries and frameworks
- External APIs and services
- Legacy code you can't modify
- Other teams' code
- Operating system interfaces

Managing these boundaries well is critical for:
- Keeping your code flexible
- Protecting yourself from changes in third-party code
- Maintaining clean architecture
- Enabling testing

### The Boundary Problem

You don't control third-party code. When it changes, your code might break. When you don't understand it well, you might use it incorrectly.

**Uncle Bob's advice:**
> "Code at the boundaries needs clear separation and tests that define expectations."

### Using Third-Party Code

**The challenge:** Third-party code is designed to be general-purpose and feature-rich. Your needs are specific. The boundary between what they provide and what you need can be messy.

**Example:**

Java's `Map` interface has many methods:
- `clear()`, `containsKey()`, `containsValue()`, `get()`, `put()`, `remove()`...

If you pass a `Map` around your code, anyone can call any of these methods. You can't control how it's used.

**Better approach - Wrap it:**

```java
public class Sensors {
    private Map<String, Sensor> sensors = new HashMap<>();

    public Sensor getById(String id) {
        return sensors.get(id);
    }

    public void register(String id, Sensor sensor) {
        sensors.put(id, sensor);
    }

    // Only expose what you need, hide the rest
}
```

Now:
- You control the interface
- You can change the implementation without affecting callers
- You can add validation and business logic
- Testing is easier

### Learning Tests

**Uncle Bob's technique:** Write tests to explore and understand third-party APIs.

Benefits:
1. You learn how the API works
2. You have tests that verify your understanding
3. When the third-party library updates, run your learning tests to see what broke
4. Documentation in the form of executable code

**Example:**

```java
@Test
public void learnSpringShellComponentFlow() {
    ComponentFlow.Builder builder = new ComponentFlow.Builder();

    ComponentFlow flow = builder
        .withStringInput("name")
            .name("Enter your name:")
            .defaultValue("User")
        .and()
        .build();

    // Learning: default values work
    // Learning: must call build() before run()
    // Learning: context holds results
}
```

### Using Code That Doesn't Exist Yet

Sometimes you need to use an interface to code that doesn't exist yet (being developed by another team, for example).

**Uncle Bob's technique:** Define the interface you WISH you had. Create an adapter when the real code arrives.

**Example:**

```java
// Define the interface you want
public interface BookRecommendationService {
    List<Book> getRecommendationsFor(User user);
}

// Use it in your code
public class LibraryService {
    private final BookRecommendationService recommendations;

    public List<Book> suggestBooks(User user) {
        return recommendations.getRecommendationsFor(user);
    }
}

// When the real API arrives, create an adapter
public class OpenAIRecommendationAdapter implements BookRecommendationService {
    private final OpenAIClient client;

    public List<Book> getRecommendationsFor(User user) {
        // Translate from your interface to OpenAI's API
        String prompt = buildPrompt(user);
        String response = client.complete(prompt);
        return parseBooks(response);
    }
}
```

---

## Your Code Analysis

Let's examine how you're managing boundaries in your Bibby codebase.

### Your Third-Party Dependencies

From your `pom.xml`, you depend on:

1. **Spring Boot 3.5.7** - Application framework
2. **Spring Shell 3.4.1** - CLI framework
3. **Spring Data JPA** - Database abstraction
4. **PostgreSQL** - Database driver
5. **Spring Web** - REST framework (not yet used)

These are significant dependencies. Let's see how you're managing them.

---

### Boundary #1: Spring Shell (ComponentFlow)

**Location:** Throughout `BookCommands.java` and `BookcaseCommands.java`

**Current Usage:**

```java
ComponentFlow flow = componentFlowBuilder.clone()
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

**Issue #1: Spring Shell is everywhere**

Your command classes are tightly coupled to Spring Shell's `ComponentFlow` API. This appears 20+ times across your command classes.

**Why This Hurts:**

1. **Can't test without Spring Shell:** Your methods require ComponentFlow.Builder
2. **Can't switch UI frameworks:** Locked into Spring Shell
3. **Business logic mixed with UI framework:** Hard to reuse logic

**How to Fix - Create an Abstraction:**

```java
// Your interface (not Spring Shell's)
public interface UserInputService {
    String promptForText(String prompt, String defaultValue);
    int promptForNumber(String prompt);
    <T> T promptForSelection(String prompt, Map<String, T> options);
}

// Spring Shell implementation
@Component
public class SpringShellInputService implements UserInputService {
    private final ComponentFlow.Builder flowBuilder;

    @Override
    public String promptForText(String prompt, String defaultValue) {
        ComponentFlow flow = flowBuilder.clone()
            .withStringInput("input")
                .name(prompt)
                .defaultValue(defaultValue)
            .and()
            .build();

        return flow.run().getContext().get("input", String.class);
    }

    @Override
    public int promptForNumber(String prompt) {
        String input = promptForText(prompt, "0");
        return Integer.parseInt(input);
    }

    @Override
    public <T> T promptForSelection(String prompt, Map<String, T> options) {
        // Implementation using SingleItemSelector
    }
}

// Commands now use your interface
@Component
@Command(command = "book", group = "Book Commands")
public class BookCommands {
    private final UserInputService input;  // NOT ComponentFlow.Builder!
    private final BookService bookService;

    @Command(command = "add")
    public void addBook() {
        String title = input.promptForText("Book Title:", "");
        int authorCount = input.promptForNumber("Number of Authors:");

        // Rest of logic...
    }
}
```

**Benefits:**
- Command classes don't know about Spring Shell
- Can test with a mock `UserInputService`
- Can switch to different UI framework
- Cleaner, more focused command methods

---

### Boundary #2: JPA/Hibernate Annotations

**Location:** All `*Entity.java` files

**Current Usage:**

```java
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    @ManyToMany
    @JoinTable(name = "book_authors")
    private Set<AuthorEntity> authors = new HashSet<>();
}
```

**Status: Actually Good!**

You're using JPA annotations directly on your entities. This is a pragmatic choice for your application size.

**Why It's Acceptable:**

1. **Spring Data JPA is your persistence strategy** - not likely to change
2. **Small application** - the cost of separating persistence from domain isn't worth it
3. **Annotations are non-invasive** - don't fundamentally change your class design

**When you'd need to change:**

If your domain logic becomes complex and you need to:
- Test domain logic without a database
- Support multiple persistence mechanisms
- Have truly persistence-ignorant domain models

Then you'd separate into:
- **Domain models** (no annotations, pure business logic)
- **Persistence models** (JPA entities)
- **Mappers** between them

For Bibby's current scope, what you have is fine.

---

### Boundary #3: Spring Data JPA Repositories

**Location:** `*Repository.java` interfaces

**Current Usage:**

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    BookEntity findByTitle(String title);
    BookEntity findByTitleIgnoreCase(String title);
    List<BookEntity> findByTitleContaining(String title);
    List<BookEntity> findByShelfId(Long id);

    @Query(value = """
        SELECT b.book_id, b.title, ...
        FROM books b
        JOIN book_authors ba ON ...
        """, nativeQuery = true)
    BookDetailView getBookDetailView(Long bookId);
}
```

**Issue: No abstraction layer**

Your services depend directly on Spring Data repositories. This is common, but has trade-offs.

**Current (tightly coupled):**
```java
@Service
public class BookService {
    private final BookRepository bookRepository;  // Spring Data interface

    public Optional<BookEntity> findById(Long id) {
        return bookRepository.findById(id);  // Directly using Spring Data
    }
}
```

**Alternative (with abstraction):**
```java
// Your interface
public interface BookDataAccess {
    Optional<Book> findById(Long id);
    List<Book> findByTitle(String title);
    void save(Book book);
}

// Spring Data implementation
@Component
public class SpringDataBookAccess implements BookDataAccess {
    private final BookRepository repository;

    @Override
    public Optional<Book> findById(Long id) {
        return repository.findById(id)
            .map(this::toDomain);
    }
}

// Service uses your interface
@Service
public class BookService {
    private final BookDataAccess bookData;  // Your interface, not Spring's
}
```

**My Recommendation:**

For Bibby, stick with Spring Data repositories directly. The abstraction layer adds complexity without enough benefit for your application size.

**When you'd need abstraction:**
- Supporting multiple databases
- Switching persistence frameworks
- Complex domain logic that needs testing without database

---

### Boundary #4: PostgreSQL Driver

**Location:** Implicitly used through Spring Data JPA

**Current Status:** Well isolated

You never directly use PostgreSQL-specific code. Spring Data JPA abstracts it completely. This is exactly what you want.

**Evidence:**
- No `import org.postgresql` in your code
- Configuration in `application.properties`, not code
- Can switch to H2, MySQL, etc. with just config change

**This is properly managed!**

---

### Missing Boundary: No Learning Tests

**Discovery:** You have no tests that explore how Spring Shell or Spring Data JPA work.

**Why This Hurts:**

When Spring Shell updates from 3.4.1 to 3.5.0, how do you know what broke? When Spring Data JPA changes behavior, how do you verify your assumptions?

**What You Should Have:**

```java
// Learning test for Spring Shell ComponentFlow
public class SpringShellLearningTest {

    @Test
    public void learnComponentFlowStringInput() {
        ComponentFlow.Builder builder = new ComponentFlow.Builder();

        ComponentFlow flow = builder
            .withStringInput("name")
                .name("Enter name:")
                .defaultValue("Test")
            .and()
            .build();

        // This test documents how ComponentFlow works
        // When Spring Shell updates, this test verifies behavior
    }

    @Test
    public void learnSingleItemSelector() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Option 1", "value1");
        options.put("Option 2", "value2");

        ComponentFlow flow = builder
            .withSingleItemSelector("choice")
                .selectItems(options)
            .and()
            .build();

        // Documents how selectors work
    }
}
```

```java
// Learning test for Spring Data JPA
public class SpringDataJpaLearningTest {

    @Autowired
    private BookRepository repository;

    @Test
    public void learnDerivedQueryMethods() {
        // Save test data
        BookEntity book = new BookEntity();
        book.setTitle("Clean Code");
        repository.save(book);

        // Test findByTitleIgnoreCase
        BookEntity found = repository.findByTitleIgnoreCase("clean code");
        assertNotNull(found);
        assertEquals("Clean Code", found.getTitle());

        // Documents Spring Data query method behavior
    }

    @Test
    public void learnNativeQueryWithProjection() {
        // Test your native query
        BookDetailView view = repository.getBookDetailView(1L);

        // Documents how projection works
        // Verifies field mapping
    }
}
```

---

## Learning Tests

Let me show you how to write learning tests for your dependencies.

### Learning Test #1: Spring Shell ComponentFlow

**Purpose:** Understand how ComponentFlow handles user input

```java
package com.penrose.bibby.learning;

import org.junit.jupiter.api.Test;
import org.springframework.shell.component.flow.ComponentFlow;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Learning tests for Spring Shell ComponentFlow.
 * These tests document our understanding of how ComponentFlow works.
 * Run these when updating Spring Shell to verify behavior hasn't changed.
 */
public class ComponentFlowLearningTest {

    @Test
    public void stringInputWithDefault() {
        // GIVEN a flow with string input and default
        ComponentFlow.Builder builder = new ComponentFlow.Builder();
        ComponentFlow flow = builder
            .withStringInput("name")
                .name("Enter name:")
                .defaultValue("DefaultName")
            .and()
            .build();

        // WHEN we get context (simulating Enter key)
        // ComponentFlow.ComponentFlowResult result = flow.run();

        // THEN context should have the value
        // (Can't actually run without terminal, but documents API)

        // Learning: ComponentFlow requires terminal interaction
        // Learning: Can't easily unit test without mocking
        // Learning: This is why we need UserInputService abstraction!
    }

    @Test
    public void multipleInputsInSequence() {
        ComponentFlow.Builder builder = new ComponentFlow.Builder();

        ComponentFlow flow = builder
            .withStringInput("first")
                .name("First:")
            .and()
            .withStringInput("second")
                .name("Second:")
            .and()
            .build();

        // Learning: Inputs are collected sequentially
        // Learning: Context accumulates all inputs
        // Learning: Can reference previous inputs in later prompts
    }

    @Test
    public void singleItemSelectorWithMap() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Display 1", "value1");
        options.put("Display 2", "value2");

        // Learning: LinkedHashMap preserves insertion order
        // Learning: Key is displayed, value is returned
        // Learning: Useful for menu systems
    }
}
```

### Learning Test #2: Spring Data JPA Queries

```java
package com.penrose.bibby.learning;

import com.penrose.bibby.library.book.BookEntity;
import com.penrose.bibby.library.book.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Learning tests for Spring Data JPA query methods.
 * Documents how derived query methods work.
 */
@DataJpaTest
public class SpringDataJpaLearningTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository repository;

    @Test
    public void findByTitleIgnoreCaseWorks() {
        // GIVEN a book with title "Clean Code"
        BookEntity book = new BookEntity();
        book.setTitle("Clean Code");
        entityManager.persist(book);
        entityManager.flush();

        // WHEN searching with different case
        BookEntity found = repository.findByTitleIgnoreCase("clean code");

        // THEN it finds the book
        assertNotNull(found);
        assertEquals("Clean Code", found.getTitle());

        // Learning: IgnoreCase works as expected
        // Learning: Returns single entity, not Optional
    }

    @Test
    public void findByTitleContainingReturnsMultiple() {
        // GIVEN multiple books with "Code" in title
        BookEntity book1 = new BookEntity();
        book1.setTitle("Clean Code");
        BookEntity book2 = new BookEntity();
        book2.setTitle("Code Complete");

        entityManager.persist(book1);
        entityManager.persist(book2);
        entityManager.flush();

        // WHEN searching for partial match
        List<BookEntity> found = repository.findByTitleContaining("Code");

        // THEN both are found
        assertEquals(2, found.size());

        // Learning: Containing does substring match
        // Learning: Returns List, not single entity
        // Learning: Case-sensitive by default
    }

    @Test
    public void manyToManyRelationshipPersists() {
        // Test your @ManyToMany relationship
        // Document how it behaves with save, fetch, etc.
    }
}
```

---

## Adapter Pattern Examples

### Example 1: Wrapping ComponentFlow

**Problem:** ComponentFlow is spread throughout your command classes.

**Solution:** Create an adapter that hides Spring Shell details.

```java
// Your interface (clean boundary)
public interface PromptService {
    String promptText(String message);
    String promptText(String message, String defaultValue);
    int promptInteger(String message);
    <T> T promptSelection(String message, Map<String, T> choices);
    boolean promptConfirmation(String message);
}

// Spring Shell adapter (manages the boundary)
@Component
public class SpringShellPromptService implements PromptService {

    private final ComponentFlow.Builder flowBuilder;

    public SpringShellPromptService(ComponentFlow.Builder flowBuilder) {
        this.flowBuilder = flowBuilder;
    }

    @Override
    public String promptText(String message) {
        return promptText(message, "");
    }

    @Override
    public String promptText(String message, String defaultValue) {
        ComponentFlow flow = flowBuilder.clone()
            .withStringInput("value")
                .name(message)
                .defaultValue(defaultValue)
            .and()
            .build();

        return flow.run().getContext().get("value", String.class);
    }

    @Override
    public int promptInteger(String message) {
        String input = promptText(message);
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + input);
        }
    }

    @Override
    public <T> T promptSelection(String message, Map<String, T> choices) {
        ComponentFlow flow = flowBuilder.clone()
            .withSingleItemSelector("selection")
                .name(message)
                .selectItems(choices)
            .and()
            .build();

        return flow.run().getContext().get("selection");
    }

    @Override
    public boolean promptConfirmation(String message) {
        Map<String, Boolean> options = new LinkedHashMap<>();
        options.put("Yes", true);
        options.put("No", false);

        return promptSelection(message, options);
    }
}

// Mock for testing (no Spring Shell needed!)
public class MockPromptService implements PromptService {
    private Queue<String> responses = new LinkedList<>();

    public void addResponse(String response) {
        responses.add(response);
    }

    @Override
    public String promptText(String message) {
        return responses.poll();
    }

    // ... other methods return pre-configured responses
}
```

**Usage in commands:**

```java
@Component
@Command(command = "book")
public class BookCommands {

    private final PromptService prompt;  // Your interface!
    private final BookService bookService;

    @Command(command = "add")
    public void addBook() {
        String title = prompt.promptText("Book Title:");
        int authorCount = prompt.promptInteger("Number of Authors:");

        for (int i = 0; i < authorCount; i++) {
            String firstName = prompt.promptText("Author First Name:");
            String lastName = prompt.promptText("Author Last Name:");
            bookService.addAuthor(title, firstName, lastName);
        }

        if (prompt.promptConfirmation("Assign to shelf?")) {
            assignToShelf(title);
        }
    }

    // Now testable!
    // No ComponentFlow needed in tests
}
```

---

### Example 2: Future-Proofing for AI Recommendations

**Scenario:** You want to add AI book recommendations, but the API doesn't exist yet.

**Solution:** Define the interface you want, code against it, add real implementation later.

```java
// Define the interface you WISH you had
public interface BookRecommendationService {
    List<BookRecommendation> recommendFor(User user);
    List<BookRecommendation> recommendSimilarTo(Book book);
}

public record BookRecommendation(
    String title,
    String author,
    String reason,
    double confidenceScore
) {}

// Fake implementation for now
@Component
@Primary  // Use this until real AI is ready
public class SimpleRecommendationService implements BookRecommendationService {

    @Override
    public List<BookRecommendation> recommendFor(User user) {
        // Simple rule-based recommendations
        return List.of(
            new BookRecommendation(
                "The Pragmatic Programmer",
                "Andrew Hunt, David Thomas",
                "Popular among software engineers",
                0.8
            )
        );
    }

    @Override
    public List<BookRecommendation> recommendSimilarTo(Book book) {
        // Simple genre-based matching
        return Collections.emptyList();
    }
}

// When OpenAI API is ready, create adapter
@Component
public class OpenAIRecommendationService implements BookRecommendationService {

    private final OpenAIClient aiClient;

    @Override
    public List<BookRecommendation> recommendFor(User user) {
        String prompt = buildPromptFromUserHistory(user);
        String response = aiClient.complete(prompt);
        return parseRecommendations(response);
    }
}

// Your code uses the interface
@Service
public class LibraryService {
    private final BookRecommendationService recommendations;

    public void suggestBooksForUser(User user) {
        List<BookRecommendation> suggestions = recommendations.recommendFor(user);
        // Use suggestions
    }
}
```

---

## Patterns You Should Recognize

### Pattern #1: Framework Sprawl

**Your code has this:**
- ComponentFlow usage in 20+ places
- Direct Spring annotations everywhere
- Framework-specific types in method signatures

**Risk:** Locked into Spring Shell. Can't test without it. Can't switch frameworks.

### Pattern #2: The One-Way Door

Every time you use a framework API directly in business logic, you create a "one-way door" - easy to go through, hard to come back.

**Questions to ask:**
- Can I test this without the framework?
- Can I switch frameworks if needed?
- Is this business logic or UI framework logic?

### Pattern #3: The Missing Seam

A "seam" is a place where you can insert different behavior without editing code. Interfaces are seams.

**Your code lacks seams at:**
- User input (ComponentFlow everywhere)
- Display logic (System.out.println everywhere)
- Future extensibility (no interfaces for strategy pattern)

---

## Action Items

### 🔴 High Priority (Next 2 Weeks)

#### 1. **Create PromptService Interface**
**New interface:** `com.penrose.bibby.cli.PromptService`
- [ ] Define clean interface for user input
- [ ] Implement SpringShellPromptService adapter
- [ ] Replace ComponentFlow usage in one command as proof-of-concept
- **Estimated time:** 2 hours
- **Impact:** Testability, flexibility

#### 2. **Write Learning Tests for Spring Shell**
**New test class:** `src/test/java/learning/ComponentFlowLearningTest.java`
- [ ] Test string input behavior
- [ ] Test single item selector
- [ ] Test number input
- [ ] Document findings
- **Estimated time:** 1 hour
- **Impact:** Understanding, upgrade safety

#### 3. **Write Learning Tests for Spring Data JPA**
**New test class:** `src/test/java/learning/SpringDataJpaLearningTest.java`
- [ ] Test findByTitleIgnoreCase
- [ ] Test findByTitleContaining
- [ ] Test @ManyToMany relationship
- [ ] Test native query projection
- **Estimated time:** 1.5 hours
- **Impact:** Understanding, upgrade safety

---

### 🟡 Medium Priority (Next Month)

#### 4. **Refactor All Commands to Use PromptService**
**Files:** `BookCommands.java`, `BookcaseCommands.java`
- [ ] Replace all ComponentFlow usage
- [ ] Use PromptService interface
- [ ] Write unit tests with mock
- **Estimated time:** 4 hours
- **Impact:** Fully testable commands

#### 5. **Create DisplayService Interface**
**New interface:** `com.penrose.bibby.cli.DisplayService`
- [ ] Abstract away System.out.println
- [ ] Create ConsoleDisplayService implementation
- [ ] Enable testing without console output
- **Estimated time:** 2 hours
- **Impact:** Testability

---

### 🟢 Low Priority (Future)

#### 6. **Document Third-Party Dependencies**
- [ ] Create DEPENDENCIES.md
- [ ] List all third-party libs
- [ ] Document why each is used
- [ ] Note version constraints
- **Estimated time:** 30 minutes
- **Impact:** Maintenance clarity

---

## Key Takeaways

### What You're Doing Right

1. **PostgreSQL is properly isolated** - No direct driver usage
2. **JPA annotations are pragmatic** - Right level of coupling for your app size
3. **Spring Data repos are clean** - Using framework as intended

### What Needs Work

1. **ComponentFlow everywhere** - Spread throughout command classes
2. **No learning tests** - Don't know if framework updates break you
3. **No abstractions for UI** - Can't test commands easily
4. **Framework in business logic** - UI concerns mixed with domain

### The Big Lesson

**Good boundaries are about managing coupling, not eliminating it.**

You WILL depend on frameworks. The question is: **where and how deeply**?

**Good coupling:**
- Configuration (Spring annotations on components)
- Infrastructure (JPA on persistence layer)
- Framework used as designed (Spring Data repos)

**Bad coupling:**
- Business logic using framework APIs directly
- UI framework spread throughout domain logic
- Can't test without framework
- Can't switch implementations

**The Test:**

Can you test your business logic without:
- Spring Boot running?
- ComponentFlow available?
- A database connection?

Right now, the answer is largely "no." That's what needs to change.

### Uncle Bob's Wisdom

> "We let third-party code talk to us through interfaces defined in our code."

> "We might use some of the learning tests to explore our understanding of third-party code."

> "Code at the boundaries needs clear separation and tests that define expectations."

---

## Further Study

### From Clean Code (Chapter 8)
- Re-read pages 113-122
- Focus on "Learning Tests Are Better Than Free"
- Study the example of wrapping `Map`

### Hexagonal Architecture (Ports and Adapters)
- Alistair Cockburn's Hexagonal Architecture
- Primary ports (drive your app) vs Secondary ports (your app drives)
- How to structure applications around business logic, not frameworks

### Dependency Inversion Principle
- High-level modules don't depend on low-level modules
- Both depend on abstractions
- Robert C. Martin's SOLID principles

### Testing Without Frameworks
- Growing Object-Oriented Software, Guided by Tests (Freeman & Pryce)
- Testing strategies for framework-heavy code

---

## Final Thoughts from Your Mentor

Boundaries are subtle. You don't feel the pain immediately when you tightly couple to a framework. It feels productive—you're using powerful tools to get things done quickly.

The pain comes later:
- When you want to test without spinning up the whole framework
- When the framework updates and breaks your code
- When you want to switch frameworks or add alternative interfaces

Your biggest boundary issue is **ComponentFlow sprawl**. It's everywhere in your command classes, making them impossible to test without Spring Shell.

The fix is straightforward: create a `PromptService` interface. It's a few hours of work that will pay dividends immediately in testing and long-term in flexibility.

Start there. Write the learning tests. You'll be surprised how much you learn about Spring Shell by documenting your assumptions as tests.

And remember: **the goal isn't to avoid frameworks**. Spring Boot and Spring Shell are excellent tools. The goal is to **control where and how you use them**, so they don't control you.

---

**End of Section 7: Boundaries**
