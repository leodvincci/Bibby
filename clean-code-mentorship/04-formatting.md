# SECTION 4: FORMATTING
## Clean Code Chapter 5 Applied to Your Bibby Codebase

---

## Table of Contents
1. [Principles Overview](#principles-overview)
2. [Your Code Analysis](#your-code-analysis)
3. [Standards to Adopt](#standards-to-adopt)
4. [Practical Examples](#practical-examples)
5. [Patterns You Should Recognize](#patterns-you-should-recognize)
6. [Action Items](#action-items)
7. [Key Takeaways](#key-takeaways)
8. [Further Study](#further-study)

---

## Principles Overview

### Why Formatting Matters

Here's a hard truth: **Code formatting is not about personal preference. It's about communication.**

When you write code, you're not just instructing a computer. You're communicating with other humans (including future you). Consistent, thoughtful formatting makes that communication clearer.

Uncle Bob says:
> "Code formatting is important. It is too important to ignore and it is too important to treat religiously. Code formatting is about communication, and communication is the professional developer's first order of business."

Formatting affects:
- **Readability:** Can you quickly scan and understand the code?
- **Maintainability:** Can you find what you're looking for?
- **Professionalism:** Does your code look like it was written by a professional?

### The Newspaper Metaphor

Think about how a newspaper is structured:
- **Headline** (class name): Tells you what it's about
- **Synopsis** (public methods): High-level overview in first paragraph
- **Details** (private methods): Increasing detail as you read down
- **Short articles:** Each story is concise and focused

Your code should work the same way. Someone reading your class should get:
1. The big picture from the class name and public interface
2. Increasing levels of detail as they read down
3. Related concepts grouped together

---

## The Two Axes of Formatting

### Vertical Formatting: Top to Bottom

**File Length:**
- Uncle Bob: "Files should be small, typically 200-500 lines"
- **Your worst offender:** `BookCommands.java` at **594 lines** 🚨

**Vertical Openness:**
- Blank lines separate concepts
- Related concepts should be vertically close
- Don't over-separate (one blank line is usually enough)

**Vertical Density:**
- Lines of code that are tightly related should appear vertically dense
- Don't put blank lines between every line

**Vertical Distance:**
- Variable declarations should be close to their usage
- Instance variables should be at the top of the class
- Dependent functions should be vertically close

**Vertical Ordering:**
- High-level functions at the top
- Low-level helper functions below
- Functions that call other functions should be above those they call

### Horizontal Formatting: Left to Right

**Line Length:**
- Uncle Bob: "Limit lines to 100-120 characters"
- **Your issues:** Multiple lines exceed 120 characters

**Horizontal Openness and Density:**
- Use spaces to associate related things and disassociate weakly related
- Don't align variable declarations (makes them hard to read)

**Indentation:**
- Consistent indentation shows structure
- Never skip indentation for short statements

---

## Your Code Analysis

Let's examine specific formatting issues in your Bibby codebase.

### Issue #1: File Way Too Long

**Location:** `BookCommands.java`

**Current Stats:**
- **594 lines** (3x the recommended maximum)
- **17 public methods** (way too many responsibilities)
- **Uses section marker comments** to organize (symptom of the problem)

**Why This Hurts:**

When a file is this long:
1. You can't grasp the whole structure at a glance
2. Finding specific functionality requires scrolling and searching
3. The class is definitely violating Single Responsibility Principle
4. Merge conflicts become more likely
5. It's harder to test

**How to Fix - Split Into Multiple Classes:**

```
Current:
BookCommands.java (594 lines)

Proposed:
BookCreationCommands.java      (~100 lines)
  ├── addBook()
  ├── collectBookTitle()
  ├── collectAuthorCount()
  └── addAuthorsToBook()

BookSearchCommands.java         (~150 lines)
  ├── searchBook()
  ├── searchByAuthor()
  ├── searchByTitle()
  └── displaySearchResults()

BookCheckoutCommands.java       (~100 lines)
  ├── checkOutBook()
  ├── checkInBook()
  └── displayCheckoutConfirmation()

BookShelfCommands.java          (~80 lines)
  ├── assignBookToShelf()
  ├── suggestShelf()
  └── displayShelfOptions()

MenuOptionBuilders.java         (~60 lines)
  ├── buildSearchOptions()
  ├── bookCaseOptions()
  ├── bookShelfOptions()
  └── yesNoOptions()
```

Each file is now:
- Under 200 lines ✅
- Focused on one responsibility ✅
- Easy to understand at a glance ✅
- Easy to test ✅

---

### Issue #2: No Vertical Spacing Between Methods

**Location:** `ShelfEntity.java`

**Current Code:**
```java
public ShelfEntity() {

}
public Long getBookcaseId() {
    return bookcaseId;
}
public void setBookcaseId(Long bookCaseLabel) {
    this.bookcaseId = bookCaseLabel;
}
public String getShelfLabel() {
    return shelfLabel;
}
public void setShelfLabel(String label) {
    this.shelfLabel = label;
}
// ... continues without spacing
```

**Why This Hurts:**

All the methods run together. Your eye can't quickly distinguish where one ends and another begins. It's like reading a paragraph with no sentence breaks.

**How to Fix - Add Vertical Openness:**

```java
public ShelfEntity() {
}

public Long getBookcaseId() {
    return bookcaseId;
}

public void setBookcaseId(Long bookCaseLabel) {
    this.bookcaseId = bookCaseLabel;
}

public String getShelfLabel() {
    return shelfLabel;
}

public void setShelfLabel(String label) {
    this.shelfLabel = label;
}

public Long getShelfId() {
    return shelfId;
}

public void setShelfId(Long id) {
    this.shelfId = id;
}

public int getShelfPosition() {
    return shelfPosition;
}

public void setShelfPosition(int shelfPosition) {
    this.shelfPosition = shelfPosition;
}
```

**Notice:** One blank line between methods. Not two, not zero. One.

---

### Issue #3: Long Lines with ANSI Codes

**Location:** `BookCommands.java:62, 124, 157, etc.`

**Current Code:**
```java
public BookCommands(ComponentFlow.Builder componentFlowBuilder, BookService bookService, BookController bookController, BookcaseService bookcaseService, ShelfService shelfService, AuthorService authorService) {
```

**Line Length:** 200+ characters

**Why This Hurts:**
- Forces horizontal scrolling
- Hard to read on code review tools
- Violates 120-character limit

**How to Fix - Break Into Multiple Lines:**

```java
public BookCommands(
        ComponentFlow.Builder componentFlowBuilder,
        BookService bookService,
        BookController bookController,
        BookcaseService bookcaseService,
        ShelfService shelfService,
        AuthorService authorService) {
    this.componentFlowBuilder = componentFlowBuilder;
    this.bookService = bookService;
    this.bookController = bookController;
    this.bookcaseService = bookcaseService;
    this.shelfService = shelfService;
    this.authorService = authorService;
}
```

Even better, recognize that 6 dependencies is too many (code smell), and refactor:

```java
public BookCommands(
        ComponentFlow.Builder componentFlowBuilder,
        BookService bookService,
        BookcaseService bookcaseService,
        ShelfService shelfService) {
    this.componentFlowBuilder = componentFlowBuilder;
    this.bookService = bookService;
    this.bookcaseService = bookcaseService;
    this.shelfService = shelfService;
}
```

(Remove `BookController` and `AuthorService` if they're not actually used in meaningful ways)

---

### Issue #4: ANSI Escape Sequences Make Lines Unreadable

**Location:** `BookCommands.java:41, 124, 283, etc.`

**Current Code:**
```java
System.out.println(" %-12s \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mShelves    \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mBooks ");
```

**Why This Hurts:**
- ANSI codes are hard to read
- Line is extremely long
- Can't tell what's being displayed

**How to Fix - Extract to Constants and Helper Methods:**

```java
// Constants at top of class
private static final String ANSI_BOLD = "\u001B[1m";
private static final String ANSI_RESET = "\u001B[0m";
private static final String ANSI_CYAN = "\u001B[36m";
private static final String ANSI_YELLOW = "\u001B[33m";
private static final String ANSI_RED = "\u001B[31m";

// Better yet, create a Console utility class
public class ConsoleColors {
    public static final String BOLD = "\u001B[1m";
    public static final String RESET = "\u001B[0m";
    public static final String CYAN = "\u001B[36m";
    public static final String YELLOW = "\u001B[33m";

    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    public static String cyan(String text) {
        return CYAN + text + RESET;
    }

    public static String yellow(String text) {
        return YELLOW + text + RESET;
    }
}

// Usage
System.out.println(
    ConsoleColors.cyan("</>") + ": " +
    "Recommended Shelf → " +
    ConsoleColors.yellow("D-48") + ": Programming & Engineering."
);
```

Much more readable!

---

### Issue #5: Inconsistent Blank Line Usage

**Location:** Various files

**Examples:**

**Too many blank lines:**
```java
public void authorNameComponentFlow(String title){
    ComponentFlow flow2;


    flow2 = componentFlowBuilder.clone()
```

**Not enough blank lines:**
```java
    String title;
    String author;
    flow = componentFlowBuilder.clone()
```

**Why This Hurts:**

Inconsistent spacing makes the code look amateurish and harder to read.

**The Rule:**

- **One blank line** between methods
- **One blank line** between logical sections within a method
- **No blank lines** between consecutive related statements
- **No blank lines** at the beginning or end of a method

**How to Fix:**

```java
public void authorNameComponentFlow(String title){
    ComponentFlow flow = componentFlowBuilder.clone()
            .withStringInput("authorFirstName")
            .name("Author's First Name:_")
            .and()
            .withStringInput("authorLastName")
            .name("Author's Last Name:_")
            .and()
            .build();

    ComponentFlow.ComponentFlowResult result = flow.run();

    String firstName = result.getContext().get("authorFirstName", String.class);
    String lastName = result.getContext().get("authorLastName", String.class);

    BookRequestDTO bookRequestDTO = new BookRequestDTO(title, firstName, lastName);
    bookService.createNewBook(bookRequestDTO);
}
```

**Notice:**
- Blank line after building the flow (separates setup from execution)
- Blank line after getting results (separates extraction from usage)
- No blank lines between related variable declarations

---

### Issue #6: Vertical Ordering Violation

**Location:** `BookCommands.java:172-174, 256-259`

**Current Code:**

Methods appear in this order:
1. `addBook()` (line 89) - public
2. `showLoading()` (line 172) - public
3. `searchBook()` (line 189) - public
4. `addToShelf()` (line 214) - public
5. `searchByAuthorVoice()` (line 256) - public (empty/dead)
6. `searchByAuthor()` (line 261) - public
7. ... private helpers scattered throughout ...

**Why This Hurts:**

Related functions aren't near each other. `searchByAuthor()` is 70 lines away from `searchBook()`, even though they're related.

**How to Fix - Organize by Feature:**

```java
// ============================================
// Public Commands (in logical groupings)
// ============================================

// Book Creation
public void addBook() { ... }

// Book Search
public void searchBook() { ... }
public void searchByAuthor() { ... }
public void searchByTitle() { ... }

// Book Checkout
public void checkOutBook() { ... }
public void checkInBook() { ... }

// Book Shelf Management
public void assignBookToShelf() { ... }
public void suggestShelf() { ... }

// ============================================
// Private Helper Methods (below their callers)
// ============================================

private String collectBookTitle() { ... }
private int collectAuthorCount() { ... }
// etc.
```

Or better yet (as mentioned in Issue #1), split into separate classes.

---

### Issue #7: Empty Constructor with Blank Line

**Location:** `ShelfEntity.java:16-18`, `BookcaseEntity.java:26-28`

**Current Code:**
```java
public ShelfEntity() {

}
```

**Why This Hurts:**

Empty constructors don't need a blank line inside. It makes them look heavier than they are.

**How to Fix:**

```java
public ShelfEntity() {
}
```

Or use Lombok to eliminate boilerplate entirely:

```java
@Entity
@Table(name = "shelves")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShelfEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;
    private String shelfLabel;
    private Long bookcaseId;
    private int shelfPosition;
}
```

---

### Issue #8: Inconsistent Indentation in Fluent Chains

**Location:** `BookCommands.java:98-105, 130-133`

**Current Code (inconsistent):**
```java
flow = componentFlowBuilder.clone()
        .withStringInput("title")
        .name("Book Title:_")
        .and()
        .withStringInput("author_count")
        .name("Number of Authors:_")
        .and()
        .build();

flow = componentFlowBuilder.clone()
                .withSingleItemSelector("recommendShelf")
                .selectItems(yesNoOptions())
                .and().build();
```

**Why This Hurts:**

Inconsistent indentation (sometimes 8 spaces, sometimes 16) makes the code look sloppy.

**How to Fix - Be Consistent:**

```java
// Option 1: Align with opening parenthesis (preferred)
flow = componentFlowBuilder.clone()
    .withStringInput("title")
    .name("Book Title:_")
    .and()
    .withStringInput("author_count")
    .name("Number of Authors:_")
    .and()
    .build();

flow = componentFlowBuilder.clone()
    .withSingleItemSelector("recommendShelf")
    .selectItems(yesNoOptions())
    .and()
    .build();

// Option 2: Double indent
flow = componentFlowBuilder.clone()
        .withStringInput("title")
        .name("Book Title:_")
        .and()
        .build();
```

Pick one style and use it everywhere.

---

## Standards to Adopt

### Recommended Formatting Rules for Bibby

Based on industry standards and your Spring Boot project, here are the formatting rules you should adopt:

#### File Organization

```java
package declaration
    ↓
import statements (grouped: java.*, javax.*, then third-party, then project)
    ↓
class Javadoc
    ↓
class declaration
    ↓
static constants
    ↓
instance variables (private)
    ↓
constructors
    ↓
public methods (grouped by feature)
    ↓
private methods (near their callers)
```

#### Vertical Spacing

- **One blank line** between methods
- **One blank line** between logical sections in a method
- **No blank lines** between tightly related statements
- **No blank lines** inside empty methods or blocks

#### Horizontal Spacing

- **Maximum line length:** 120 characters
- **Indentation:** 4 spaces (not tabs)
- **Spaces around operators:** `a + b`, not `a+b`
- **No spaces inside parentheses:** `method(arg)`, not `method( arg )`
- **Space after keywords:** `if (condition)`, not `if(condition)`

#### Method Ordering

1. Public methods first (in logical groups)
2. Private methods below their callers
3. Getters and setters at the end (or use Lombok to eliminate them)

---

### IDE Configuration (IntelliJ IDEA)

Configure your IDE to enforce these rules automatically:

**Settings → Editor → Code Style → Java:**

1. **Tabs and Indents:**
   - Tab size: 4
   - Indent: 4
   - Use spaces, not tabs

2. **Wrapping and Braces:**
   - Hard wrap at: 120 columns
   - Method parameters: Wrap if long, align when multiline

3. **Blank Lines:**
   - Around class: 1
   - Around method: 1
   - After class header: 0

4. **Imports:**
   - Use single class imports (no wildcards except for static imports with 5+ classes)
   - Order: java, javax, third-party, project imports

**Enable on save:**
- Settings → Tools → Actions on Save
  - ✅ Reformat code
  - ✅ Optimize imports
  - ✅ Rearrange code (optional)

---

## Practical Examples

### Example 1: Before and After - Entity Class

**Before:**
```java
@Entity
@Table(name = "shelves")
public class ShelfEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;
    private String shelfLabel;
    private Long bookcaseId;
    private int shelfPosition;

    public ShelfEntity() {

    }
    public Long getBookcaseId() {
        return bookcaseId;
    }
    public void setBookcaseId(Long bookCaseLabel) {
        this.bookcaseId = bookCaseLabel;
    }
    public String getShelfLabel() {
        return shelfLabel;
    }
    // ... more getters/setters with no spacing
}
```

**After (without Lombok):**
```java
@Entity
@Table(name = "shelves")
public class ShelfEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;

    private String shelfLabel;
    private Long bookcaseId;
    private int shelfPosition;

    public ShelfEntity() {
    }

    public ShelfEntity(String shelfLabel, Long bookcaseId, int shelfPosition) {
        this.shelfLabel = shelfLabel;
        this.bookcaseId = bookcaseId;
        this.shelfPosition = shelfPosition;
    }

    public Long getShelfId() {
        return shelfId;
    }

    public void setShelfId(Long shelfId) {
        this.shelfId = shelfId;
    }

    public String getShelfLabel() {
        return shelfLabel;
    }

    public void setShelfLabel(String shelfLabel) {
        this.shelfLabel = shelfLabel;
    }

    public Long getBookcaseId() {
        return bookcaseId;
    }

    public void setBookcaseId(Long bookcaseId) {
        this.bookcaseId = bookcaseId;
    }

    public int getShelfPosition() {
        return shelfPosition;
    }

    public void setShelfPosition(int shelfPosition) {
        this.shelfPosition = shelfPosition;
    }
}
```

**After (with Lombok - RECOMMENDED):**
```java
@Entity
@Table(name = "shelves")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShelfEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;

    private String shelfLabel;
    private Long bookcaseId;
    private int shelfPosition;
}
```

**Improvement:**
- Vertical spacing between fields
- Consistent blank lines between methods
- No blank line inside empty constructor
- OR eliminate boilerplate with Lombok

---

### Example 2: Breaking Long Lines

**Before:**
```java
System.out.println(String.format("""
        \n\u001B[32mConfirm Checkout\n\u001B[0m
                \033[31mTitle\u001B[0m %s
                \033[31mAuthor/s\u001B[0m %s
                \033[31mStatus %s
                \033[31mBookcase\u001B[0m %s
                \033[31mShelf\u001B[0m %s
        """,bookEntity.getTitle(), authors, bookEntity.getBookStatus(), bookcaseLabel ,bookshelfLabel));
```

**After:**
```java
String confirmationMessage = formatCheckoutConfirmation(
    bookEntity.getTitle(),
    authors,
    bookEntity.getBookStatus(),
    bookcaseLabel,
    bookshelfLabel
);
System.out.println(confirmationMessage);

// Helper method
private String formatCheckoutConfirmation(
        String title,
        List<AuthorEntity> authors,
        String status,
        String bookcaseLabel,
        String shelfLabel) {

    return String.format("""
        %sConfirm Checkout%s

        %sTitle:%s %s
        %sAuthor/s:%s %s
        %sStatus:%s %s
        %sBookcase:%s %s
        %sShelf:%s %s
        """,
        ConsoleColors.GREEN, ConsoleColors.RESET,
        ConsoleColors.RED, ConsoleColors.RESET, title,
        ConsoleColors.RED, ConsoleColors.RESET, formatAuthors(authors),
        ConsoleColors.RED, ConsoleColors.RESET, status,
        ConsoleColors.RED, ConsoleColors.RESET, bookcaseLabel,
        ConsoleColors.RED, ConsoleColors.RESET, shelfLabel
    );
}
```

---

## Patterns You Should Recognize

### Pattern #1: The Growing File

You start with a small class. Then you add "just one more method." Then another. Before you know it, you have 594 lines.

**Prevention:**
- Set a line limit (200 lines)
- When you hit it, refactor before adding more
- Ask: "Does this method belong in this class?"

### Pattern #2: Inconsistent Spacing

You copy-paste code from different sources, each with different spacing conventions.

**Prevention:**
- Use IDE auto-formatting (Ctrl+Alt+L in IntelliJ)
- Set up "format on save"
- Run formatting check in CI/CD

### Pattern #3: The Horizontal Scroll

Lines get longer as you nest deeper or add more parameters.

**Prevention:**
- Break long parameter lists into multiple lines
- Extract constants for long strings
- Refactor to reduce nesting

---

## Action Items

### 🔴 High Priority (This Week)

#### 1. **Split BookCommands.java**
**File:** `BookCommands.java` (594 lines)
- [ ] Extract BookCreationCommands
- [ ] Extract BookSearchCommands
- [ ] Extract BookCheckoutCommands
- [ ] Extract BookShelfCommands
- [ ] Create shared MenuOptionsBuilder utility
- **Estimated time:** 4-6 hours
- **Impact:** MASSIVE improvement in organization and readability

#### 2. **Add Vertical Spacing to Entity Classes**
**Files:** All `*Entity.java` files
- [ ] Add blank line between each method
- [ ] Remove blank lines inside empty constructors
- [ ] Separate field declarations with blank lines
- **Estimated time:** 30 minutes
- **Impact:** Immediate readability improvement

#### 3. **Fix Constructor Parameter Formatting**
**File:** `BookCommands.java:62`
- [ ] Break long constructor into multiple lines
- [ ] One parameter per line
- **Estimated time:** 5 minutes
- **Impact:** Readability, no horizontal scrolling

#### 4. **Extract Console Color Constants**
**Files:** `BookCommands.java`, `BookcaseCommands.java`
- [ ] Create `ConsoleColors` utility class
- [ ] Define ANSI codes as constants
- [ ] Create helper methods (bold(), cyan(), etc.)
- [ ] Replace all inline ANSI codes with utility calls
- **Estimated time:** 1.5 hours
- **Impact:** Massive readability improvement for display code

---

### 🟡 Medium Priority (Next 2 Weeks)

#### 5. **Configure IDE Auto-Formatting**
- [ ] Set up IntelliJ code style (120 char limit, 4-space indent)
- [ ] Enable "Reformat code" on save
- [ ] Enable "Optimize imports" on save
- **Estimated time:** 15 minutes
- **Impact:** Prevents future formatting issues

#### 6. **Add Lombok to Eliminate Boilerplate**
**Files:** All entity classes
- [ ] Add Lombok dependency to pom.xml
- [ ] Replace getters/setters with @Getter/@Setter
- [ ] Replace constructors with @NoArgsConstructor/@AllArgsConstructor
- [ ] Install Lombok plugin in IDE
- **Estimated time:** 1 hour
- **Impact:** Reduces entity classes from 50+ lines to 10-15 lines

#### 7. **Standardize Fluent Chain Indentation**
**Files:** All command classes
- [ ] Pick one indentation style for builder patterns
- [ ] Apply consistently throughout
- **Estimated time:** 30 minutes
- **Impact:** Visual consistency

---

### 🟢 Low Priority (Nice to Have)

#### 8. **Create Team Formatting Guide**
- [ ] Document chosen formatting standards in CONTRIBUTING.md
- [ ] Export IDE code style as XML
- [ ] Share with team (or for future reference)
- **Estimated time:** 30 minutes
- **Impact:** Consistency for future development

#### 9. **Set Up Pre-Commit Hook for Formatting**
- [ ] Add checkstyle or spotless to Maven build
- [ ] Configure to enforce formatting rules
- [ ] Fail build on formatting violations
- **Estimated time:** 1 hour
- **Impact:** Enforces standards automatically

---

## Formatting Checklist

Before committing code, check:

### File Level
- [ ] File is under 200 lines (split if longer)
- [ ] One blank line between methods
- [ ] Imports are organized (java, javax, third-party, project)
- [ ] No wildcard imports (except for common utilities)

### Method Level
- [ ] Method is under 20 lines
- [ ] Logical sections separated by one blank line
- [ ] No blank lines at start/end of method
- [ ] Related statements are vertically dense (no unnecessary blank lines)

### Line Level
- [ ] Lines are under 120 characters
- [ ] Indentation is consistent (4 spaces)
- [ ] Spaces around operators (`a + b`, not `a+b`)
- [ ] No trailing whitespace

### Readability
- [ ] Code reads top-to-bottom (stepdown rule)
- [ ] Public methods before private
- [ ] Helper methods near their callers
- [ ] Related concepts are vertically close

---

## Key Takeaways

### What You're Doing Right

1. **Consistent indentation:** Your code uses consistent spacing (mostly)
2. **Logical method grouping:** Related methods are generally near each other
3. **No crazy alignment tricks:** You're not doing weird whitespace alignment

### What Needs Work

1. **File length:** BookCommands.java is 3x too long
2. **Vertical spacing:** Entity classes have no spacing between methods
3. **Long lines:** ANSI codes and long parameter lists exceed 120 chars
4. **Boilerplate:** Entity classes are full of getters/setters (use Lombok)

### The Big Lesson

**Formatting is not about aesthetics. It's about communication.**

Good formatting makes code:
- **Scannable:** You can quickly find what you're looking for
- **Understandable:** Structure reveals intent
- **Maintainable:** Consistent style reduces cognitive load

Bad formatting makes code:
- **Confusing:** Hard to tell where things begin and end
- **Intimidating:** Large files scare people away
- **Error-prone:** Hard to spot bugs in poorly formatted code

### Uncle Bob's Golden Rule

> "Follow the formatting conventions your team has agreed upon. If you're on a team of one, be consistent."

---

## Further Study

### From Clean Code (Chapter 5)
- Re-read pages 75-97
- Study the example refactorings showing before/after formatting
- Pay attention to the "Team Rules" section

### Google Java Style Guide
- https://google.github.io/styleguide/javaguide.html
- Definitive reference for Java formatting
- Used by thousands of companies

### Effective Java (Joshua Bloch)
- Item 4: Enforce noninstantiability with a private constructor
- Item 16: In public classes, use accessor methods, not public fields
- Item 17: Minimize mutability

### Project Lombok
- https://projectlombok.org/
- Learn @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor
- Understand @Data, @Builder, @Value

---

## Final Thoughts from Your Mentor

Your code shows good instincts. You're not doing crazy things with formatting. You have consistent indentation, and your methods are generally organized well.

The biggest issue is **file length**. BookCommands.java at 594 lines is your #1 priority to fix. This isn't just a formatting issue—it's a design issue. That class is doing too much.

Split it. You'll immediately notice:
- Each new class is easier to understand
- Testing becomes easier
- Adding features becomes easier
- Your code looks more professional

The entity class formatting (no spacing between methods) is a quick win. Add one blank line between each method. Takes 5 minutes, big readability improvement.

And seriously consider Lombok. It will cut your entity classes from 50+ lines to 10-15 lines. That's not just formatting—that's eliminating noise so the important parts (your actual fields and business logic) stand out.

**Remember:** Code formatting is a sign of professionalism. When someone opens your code and sees consistent, clean formatting, they immediately know you care about your craft.

And when they see a 594-line file with methods running together... well, they know you're still learning.

Let's fix that.

---

**End of Section 4: Formatting**
