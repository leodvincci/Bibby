# SECTION 19: THE DRY PRINCIPLE

## Introduction: Don't Repeat Yourself

**"Every piece of knowledge must have a single, unambiguous, authoritative representation within a system."**

This is the DRY principle from *The Pragmatic Programmer*. It's one of the most misunderstood principles in software development.

DRY is **NOT** just about code duplication. It's about **knowledge duplication**. Two pieces of code can be identical but not violate DRY. Conversely, two different-looking pieces of code can violate DRY if they represent the same knowledge.

In this section, we'll explore:
- The true meaning of DRY (knowledge vs. code)
- Four types of duplication and how to recognize them
- Real DRY violations in the Bibby codebase
- When duplication is actually acceptable
- Refactoring techniques to eliminate duplication

**Learning Objectives**:
- Distinguish knowledge duplication from code duplication
- Identify the four types of duplication
- Apply the Rule of Three for refactoring
- Recognize false DRY violations
- Refactor Bibby code to eliminate true DRY violations

**Time Investment**: 3-4 hours

---

## Understanding DRY: Knowledge vs. Code

### What is "Knowledge"?

In software, "knowledge" means:
- Business rules ("checkout period is 14 days")
- Algorithms ("calculate late fee as $0.50 per day")
- Data structures ("book has title, ISBN, authors")
- External interfaces ("ISBN validation rules")
- Design decisions ("status is an enum, not a string")

### The DRY Violation: Same Knowledge, Multiple Places

**DRY Violation Example from Bibby**:

From `AuthorEntity.java:15-27`:
```java
private String firstName;
private String lastName;
private String fullName;  // ❌ Duplication of knowledge!

public AuthorEntity(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.fullName = String.format("%s %s", firstName, lastName);  // Knowledge #1
}
```

**Problem**: The knowledge "full name = first name + space + last name" exists in TWO places:
1. As a stored field: `private String fullName`
2. In the constructor: `this.fullName = String.format("%s %s", firstName, lastName)`

**Consequence**: When someone calls `setFirstName()` or `setLastName()`, the `fullName` field becomes incorrect! The knowledge is duplicated, and they fall out of sync.

**DRY Solution**:
```java
private String firstName;
private String lastName;
// No fullName field!

public AuthorEntity(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
}

public String getFullName() {
    // Single source of truth - derived from firstName + lastName
    return String.format("%s %s", firstName, lastName);
}
```

**Knowledge now exists in exactly ONE place**: the `getFullName()` method. Impossible to be inconsistent!

---

## The Four Types of Duplication

Hunt and Thomas identify four types of duplication. Each has different causes and solutions.

### Type 1: Imposed Duplication

**Definition**: Duplication that seems unavoidable - "the environment/language forces me to do it."

Often, this is a misconception. There's usually a better way.

#### Example 1A: Javadoc Duplication (Imposed)

**Violation**:
```java
/**
 * Gets the book's ISBN.
 * @return the ISBN as a string
 */
public String getIsbn() {  // Comment duplicates what code already says!
    return isbn;
}

/**
 * Sets the book's ISBN.
 * @param isbn the ISBN to set
 */
public void setIsbn(String isbn) {  // Again, adds no value
    this.isbn = isbn;
}
```

**Problem**: The documentation duplicates knowledge already expressed in the code. If method is renamed, comment becomes wrong.

**DRY Solution**:

For simple getters/setters, don't document them! Let the code speak:
```java
// No comment needed - name is clear
public String getIsbn() {
    return isbn;
}
```

For complex methods, document the *why*, not the *what*:
```java
/**
 * Validates and normalizes ISBN format.
 * Accepts ISBN-10 or ISBN-13 with optional hyphens/spaces.
 * Converts ISBN-10 to ISBN-13 for storage consistency.
 *
 * @throws IllegalArgumentException if ISBN format is invalid
 */
public ISBN normalizeIsbn(String rawIsbn) {
    // Implementation
}
```

The comment explains **why** we normalize (consistency) and **what** we accept (both formats) - knowledge not obvious from the code.

#### Example 1B: Data Structure Duplication (Imposed)

**Violation in Bibby**:

From `Book.java`:
```java
public class Book {
    private Long id;
    private String title;
    private String isbn;
    private String publisher;
    // ... 12 more fields
}
```

From `BookRequestDTO.java` (hypothetical):
```java
public class BookRequestDTO {
    private String title;
    private String isbn;
    private String publisher;
    // ... same 12 fields
}
```

**Problem**: The knowledge "what fields a book has" is duplicated in Book and BookRequestDTO.

**But wait** - is this a TRUE DRY violation?

**NO!** These represent **different knowledge**:
- `Book`: "What fields a book entity HAS in the domain"
- `BookRequestDTO`: "What fields a user PROVIDES to create a book"

They happen to overlap, but serve different purposes. They might diverge (DTO might have combined fields, or fewer fields).

**Guideline**: If two structures **always change together**, that's duplication. If they **might** change independently, it's not.

---

### Type 2: Inadvertent Duplication

**Definition**: Developers don't realize they're duplicating knowledge.

Often happens in design phase - data model has redundant information.

#### Example 2A: AuthorEntity fullName (Inadvertent)

We covered this above. The developer didn't realize `fullName` duplicates `firstName + lastName`.

**Fix**: Remove `fullName` field, make it derived.

#### Example 2B: Cached Value Gone Wrong (Inadvertent)

**Violation**:
```java
public class Book {
    private Set<AuthorEntity> authors = new HashSet<>();
    private int authorCount;  // ❌ Duplicates authors.size()

    public void addAuthor(AuthorEntity author) {
        authors.add(author);
        authorCount++;  // Manual sync - easy to forget!
    }
}
```

**Problem**: `authorCount` duplicates knowledge already in `authors.size()`. If anyone modifies `authors` directly, count is wrong.

**DRY Solution**:
```java
public class Book {
    private Set<AuthorEntity> authors = new HashSet<>();

    public void addAuthor(AuthorEntity author) {
        authors.add(author);
    }

    public int getAuthorCount() {
        return authors.size();  // Always correct!
    }
}
```

**Exception - Performance Caching**:

If `authors.size()` is expensive (unlikely for a Set, but imagine a database query), caching IS justified:

```java
private Set<AuthorEntity> authors = new HashSet<>();
private transient Integer cachedAuthorCount;  // Clearly a cache

public int getAuthorCount() {
    if (cachedAuthorCount == null) {
        cachedAuthorCount = authors.size();
    }
    return cachedAuthorCount;
}

private void invalidateCache() {
    cachedAuthorCount = null;
}

public void addAuthor(AuthorEntity author) {
    authors.add(author);
    invalidateCache();  // Explicit management
}
```

But for Bibby, this is **over-engineering**. Just use `authors.size()` directly.

---

### Type 3: Impatient Duplication

**Definition**: Lazy shortcuts - copy/paste programming. "It's faster to duplicate than to abstract."

**Short-term**: Yes, it's faster.
**Long-term**: No, you'll pay the price in maintenance.

#### Example 3A: Validation Duplication (Impatient)

**Violation** (hypothetical for Bibby):
```java
// In BookService
public void addBook(AddBookCommand command) {
    if (command.getTitle() == null || command.getTitle().isBlank()) {
        throw new ValidationException("Title required");
    }
    if (command.getIsbn() == null || command.getIsbn().isBlank()) {
        throw new ValidationException("ISBN required");
    }
    // ... create book
}

// In UpdateBookService
public void updateBook(UpdateBookCommand command) {
    if (command.getTitle() == null || command.getTitle().isBlank()) {  // ❌ Copy/paste
        throw new ValidationException("Title required");
    }
    if (command.getIsbn() == null || command.getIsbn().isBlank()) {  // ❌ Same validation
        throw new ValidationException("ISBN required");
    }
    // ... update book
}

// In ImportBookService
public void importBook(ImportBookCommand command) {
    if (command.getTitle() == null || command.getTitle().isBlank()) {  // ❌ Copy/paste again!
        throw new ValidationException("Title required");
    }
    if (command.getIsbn() == null || command.getIsbn().isBlank()) {  // ❌ Duplicated
        throw new ValidationException("ISBN required");
    }
    // ... import book
}
```

**Problem**: The knowledge "what makes a valid book" is duplicated in three places. When requirements change ("ISBN now optional"), you must update three places.

**DRY Solution**: Extract to validator
```java
@Component
public class BookCommandValidator {

    public void validate(BookCommand command) {
        validateTitle(command.getTitle());
        validateIsbn(command.getIsbn());
        validatePublisher(command.getPublisher());
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Title is required");
        }
        if (title.length() > 500) {
            throw new ValidationException("Title cannot exceed 500 characters");
        }
    }

    private void validateIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            throw new ValidationException("ISBN is required");
        }
        // ISBN format validation
        if (!isbn.matches("^\\d{9}[\\dX]$") && !isbn.matches("^\\d{13}$")) {
            throw new ValidationException("Invalid ISBN format");
        }
    }

    private void validatePublisher(String publisher) {
        if (publisher != null && publisher.length() > 200) {
            throw new ValidationException("Publisher name too long");
        }
    }
}

// Now all services use it
@Service
public class AddBookService {
    private final BookCommandValidator validator;

    public void addBook(AddBookCommand command) {
        validator.validate(command);  // Single source of truth!
        // ... create book
    }
}
```

**Knowledge now in ONE place**. Change validation rules? Update one file.

#### Example 3B: Duplicate Methods in ShelfService (Impatient)

**Real Bibby Violation** from `ShelfService.java:20-30`:
```java
public List<ShelfEntity> getAllShelves(Long bookCaseId){
    return shelfRepository.findByBookcaseId(bookCaseId);
}

public List<ShelfEntity> findByBookcaseId(Long bookcaseId) {  // ❌ IDENTICAL!
    return shelfRepository.findByBookcaseId(bookcaseId);
}
```

**Problem**: These two methods do **exactly the same thing**. Someone wrote one, forgot about it, and later added the other.

**DRY Solution**: Delete one!
```java
/**
 * Finds all shelves belonging to the specified bookcase.
 * Returns empty list if bookcase has no shelves.
 */
public List<ShelfEntity> findByBookcaseId(Long bookcaseId) {
    Objects.requireNonNull(bookcaseId, "Bookcase ID cannot be null");
    return shelfRepository.findByBookcaseId(bookcaseId);
}
```

Update callers to use the remaining method. One source of truth.

---

### Type 4: Interdeveloper Duplication

**Definition**: Multiple developers (or same developer at different times) create duplicate functionality because they don't know it already exists.

**Example**:
```java
// Developer A creates:
public class BookFormatter {
    public String formatTitle(Book book) {
        return book.getTitle().toUpperCase();
    }
}

// Developer B doesn't know about it, creates:
public class BookDisplayUtils {
    public String displayTitle(Book book) {
        return book.getTitle().toUpperCase();  // ❌ Same logic!
    }
}
```

**Prevention**:
1. **Code reviews** - "Does this functionality already exist?"
2. **Good project structure** - Clear `util` or `formatter` packages
3. **Communication** - Daily standups, design discussions
4. **Search before creating** - Use IDE search for similar methods

**DRY Solution**:

After code review:
```java
// Keep one, delete the other
public class BookFormatter {
    public String formatTitle(Book book) {
        return book.getTitle().toUpperCase();
    }

    // Make it discoverable - good naming
    public String toDisplayFormat(Book book) {
        return String.format("%s by %s (%s)",
            book.getTitle(),
            book.getAuthors().stream()
                .map(AuthorEntity::getFullName)
                .collect(Collectors.joining(", ")),
            book.getIsbn()
        );
    }
}
```

---

## Real DRY Violations in Bibby

Let's audit your actual codebase for DRY violations.

### Violation #1: AuthorEntity.fullName (Already Covered)

**Location**: `AuthorEntity.java:17, 27, 62-67`
**Type**: Inadvertent Duplication
**Fix**: Remove `fullName` field, make getter derive it

### Violation #2: ShelfService Duplicate Methods

**Location**: `ShelfService.java:20-22, 28-30`
**Type**: Impatient Duplication
**Fix**: Delete `getAllShelves`, keep `findByBookcaseId`

### Violation #3: BookService String Status Comparison

**Location**: `BookService.java:57-58`
```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");  // ❌ "CHECKED_OUT" duplicated
```

**Knowledge Duplication**: The string `"CHECKED_OUT"` appears in:
- `BookStatus` enum
- This method (hardcoded)
- Possibly other places where status is checked

**DRY Solution**: Use enum directly
```java
public void checkOut() {
    if (this.status != BookStatus.CHECKED_OUT) {  // Use enum, not string
        this.status = BookStatus.CHECKED_OUT;
        this.checkoutCount++;
    }
}
```

**Better**: Use state pattern
```java
public void checkOut() {
    this.status = this.status.checkOut();  // Status knows how to transition
}

// In BookStatus enum
public enum BookStatus {
    AVAILABLE {
        @Override
        public BookStatus checkOut() {
            return CHECKED_OUT;
        }
    },
    CHECKED_OUT {
        @Override
        public BookStatus checkOut() {
            throw new IllegalStateException("Already checked out");
        }
    };

    public abstract BookStatus checkOut();
}
```

### Violation #4: Repository Query Method Names

**Location**: Multiple repositories
**Pattern**: Repository methods like `findByTitle`, `findByTitleIgnoreCase`, `findByTitleContaining`

**Hypothetical Duplication**:
```java
// In BookRepository
BookEntity findByTitle(String title);
BookEntity findByTitleIgnoreCase(String title);
List<BookEntity> findByTitleContaining(String title);
```

**Knowledge Duplication**: The knowledge "how to search for books by title" is duplicated three times.

**Question**: Is this TRUE duplication?

**Answer**: Maybe not! These represent different search strategies:
- Exact match (case-sensitive)
- Exact match (case-insensitive)
- Partial match

If they serve **different use cases**, they're not duplication.

**BUT** - if you find yourself always using `findByTitleIgnoreCase`, and never using `findByTitle`, that's a code smell. Delete unused methods.

---

## When Duplication is Acceptable

Not all duplication is bad. Sometimes, it's the right choice.

### Acceptable Duplication #1: Different Abstractions

```java
// Book entity - domain model
public class Book {
    private Long id;
    private Title title;
    private ISBN isbn;
    private Set<Author> authors;

    public void checkOut() { ... }
}

// BookDTO - API response
public record BookDTO(
    Long id,
    String title,
    String isbn,
    List<String> authorNames
) {}
```

**Looks like duplication**, but it's not!
- `Book`: Represents domain concepts, has behavior
- `BookDTO`: Represents API contract, data transfer only

They serve different purposes and might evolve independently.

### Acceptable Duplication #2: Test Data Builders

```java
// Test 1
Book book = aBook()
    .withTitle("Domain Driven Design")
    .withStatus(AVAILABLE)
    .build();

// Test 2
Book book = aBook()
    .withTitle("Domain Driven Design")  // ❌ Duplicate title?
    .withStatus(CHECKED_OUT)
    .build();
```

**Duplicate title** - is this DRY violation?

**NO!** Each test sets up its own data for clarity. Tests should be **independent** and **readable**. Sharing data between tests couples them and makes failures confusing.

**Rule**: Duplication in test setup is often GOOD. Each test is self-contained.

### Acceptable Duplication #3: Temporary Staging

During refactoring, you might temporarily have duplication:

```java
// Old method - deprecated but not deleted yet
@Deprecated
public String getBookStatus() {
    return status.toString();
}

// New method - better design
public BookStatus getStatus() {
    return status;
}
```

This is **transitional duplication** - acceptable during migration. Once all callers switch to `getStatus()`, delete the deprecated method.

---

## The Rule of Three

**When should you extract duplication?**

**Rule of Three** (from Martin Fowler):
1. First time: Write it
2. Second time: Wince at duplication, but write it again
3. Third time: Refactor

**Rationale**:
- One instance: No duplication
- Two instances: Might be coincidence, wait and see
- Three instances: Clear pattern, extract it

**Example**:

```java
// First use
public void addBook(Book book) {
    auditLog.info("Adding book: {}", book.getId());
    repository.save(book);
}

// Second use - notice similarity, but don't extract yet
public void updateBook(Book book) {
    auditLog.info("Updating book: {}", book.getId());  // Similar...
    repository.save(book);
}

// Third use - NOW extract!
public void deleteBook(Book book) {
    auditLog.info("Deleting book: {}", book.getId());  // OK, pattern confirmed!
    repository.delete(book);
}

// Refactored
private void logAndSave(String action, Book book, Runnable operation) {
    auditLog.info("{} book: {}", action, book.getId());
    operation.run();
}

public void addBook(Book book) {
    logAndSave("Adding", book, () -> repository.save(book));
}

public void updateBook(Book book) {
    logAndSave("Updating", book, () -> repository.save(book));
}

public void deleteBook(Book book) {
    logAndSave("Deleting", book, () -> repository.delete(book));
}
```

**Don't prematurely abstract**. Wait for the pattern to emerge.

---

## Refactoring Techniques for DRY

### Technique 1: Extract Method

**Before**:
```java
public void printBookDetails(Book book) {
    System.out.println("Title: " + book.getTitle());
    System.out.println("ISBN: " + book.getIsbn());
    System.out.println("Authors: " + book.getAuthors());
}

public void logBookDetails(Book book) {
    log.info("Title: " + book.getTitle());  // Duplicate formatting
    log.info("ISBN: " + book.getIsbn());
    log.info("Authors: " + book.getAuthors());
}
```

**After**:
```java
private String formatBookDetails(Book book) {
    return String.format("Title: %s%nISBN: %s%nAuthors: %s",
        book.getTitle(),
        book.getIsbn(),
        book.getAuthors()
    );
}

public void printBookDetails(Book book) {
    System.out.println(formatBookDetails(book));
}

public void logBookDetails(Book book) {
    log.info(formatBookDetails(book));
}
```

### Technique 2: Extract Class

**Before**:
```java
@Service
public class BookService {
    public void addBook(AddBookCommand cmd) {
        validateTitle(cmd.getTitle());
        validateIsbn(cmd.getIsbn());
        // ...
    }

    public void updateBook(UpdateBookCommand cmd) {
        validateTitle(cmd.getTitle());  // Duplicate validation
        validateIsbn(cmd.getIsbn());
        // ...
    }

    private void validateTitle(String title) { ... }
    private void validateIsbn(String isbn) { ... }
}
```

**After**:
```java
@Component
public class BookValidator {  // Extracted class
    public void validate(BookCommand cmd) {
        validateTitle(cmd.getTitle());
        validateIsbn(cmd.getIsbn());
    }

    private void validateTitle(String title) { ... }
    private void validateIsbn(String isbn) { ... }
}

@Service
public class BookService {
    private final BookValidator validator;

    public void addBook(AddBookCommand cmd) {
        validator.validate(cmd);  // Delegated
        // ...
    }

    public void updateBook(UpdateBookCommand cmd) {
        validator.validate(cmd);
        // ...
    }
}
```

### Technique 3: Use Value Objects

**Before**:
```java
public class Book {
    private String isbn;

    public void setIsbn(String isbn) {
        // Validation duplicated everywhere setIsbn is called
        if (!isbn.matches("^\\d{9}[\\dX]$") && !isbn.matches("^\\d{13}$")) {
            throw new IllegalArgumentException("Invalid ISBN");
        }
        this.isbn = isbn;
    }
}
```

**After**:
```java
public record ISBN(String value) {
    public ISBN {
        // Validation in ONE place - the constructor
        Objects.requireNonNull(value, "ISBN cannot be null");
        String cleaned = value.replaceAll("[\\s-]", "");
        if (!cleaned.matches("^\\d{9}[\\dX]$") && !cleaned.matches("^\\d{13}$")) {
            throw new IllegalArgumentException("Invalid ISBN: " + value);
        }
        value = cleaned;
    }
}

public class Book {
    private ISBN isbn;  // Type safety + automatic validation

    public void setIsbn(ISBN isbn) {
        this.isbn = Objects.requireNonNull(isbn);
    }
}
```

Now **impossible** to have an invalid ISBN anywhere in the system!

---

## Action Items: Eliminating DRY Violations in Bibby

### Action Item 1: Fix AuthorEntity.fullName (30 min)

**Task**: Remove knowledge duplication in `AuthorEntity`

**Steps**:
1. Open `AuthorEntity.java`
2. Delete `private String fullName` field (line 17)
3. Update constructor to remove `this.fullName = ...` assignment
4. Change `getFullName()` to derive value:
   ```java
   public String getFullName() {
       return String.format("%s %s", firstName, lastName);
   }
   ```
5. Delete `setFullName()` method
6. Write test proving it's fixed:
   ```java
   @Test
   void fullNameShouldAlwaysBeCorrect() {
       AuthorEntity author = new AuthorEntity("Robert", "Martin");
       assertThat(author.getFullName()).isEqualTo("Robert Martin");

       author.setLastName("C. Martin");
       assertThat(author.getFullName()).isEqualTo("Robert C. Martin");
   }
   ```
7. Commit: "Fix DRY violation: remove fullName field duplication"

**Files**: `AuthorEntity.java`

### Action Item 2: Remove Duplicate Method in ShelfService (15 min)

**Task**: Delete `getAllShelves` method

**Steps**:
1. Search codebase for calls to `getAllShelves`
2. Replace with `findByBookcaseId`
3. Delete `getAllShelves` method (lines 20-22)
4. Commit: "Fix DRY violation: remove duplicate getAllShelves method"

**Files**: `ShelfService.java`, callers

### Action Item 3: Create BookValidator (1 hour)

**Task**: Extract validation logic to dedicated class

**Steps**:
1. Create new file: `src/main/java/com/penrose/bibby/library/book/BookValidator.java`
2. Implement validation methods:
   ```java
   @Component
   public class BookValidator {
       public void validateForCreation(AddBookCommand command) {
           validateTitle(command.getTitle());
           validateIsbn(command.getIsbn());
       }

       private void validateTitle(String title) {
           if (title == null || title.isBlank()) {
               throw new ValidationException("Title is required");
           }
           if (title.length() > 500) {
               throw new ValidationException("Title cannot exceed 500 characters");
           }
       }

       private void validateIsbn(String isbn) {
           if (isbn == null || isbn.isBlank()) {
               throw new ValidationException("ISBN is required");
           }
       }
   }
   ```
3. Inject `BookValidator` into `BookService`
4. Replace inline validation with `validator.validateForCreation(command)`
5. Write tests for `BookValidator`
6. Commit: "Extract validation to BookValidator - DRY principle"

**Files**: New `BookValidator.java`, `BookService.java`

### Action Item 4: Audit for More DRY Violations (45 min)

**Task**: Create `DRY_VIOLATIONS.md` document

**Template**:
```markdown
# DRY Violations in Bibby

## High Priority

### Violation #1: [Name]
- **Location**: File.java:lines
- **Type**: Inadvertent/Impatient/Interdeveloper/Imposed
- **Problem**: [Description]
- **Fix**: [Solution]
- **Effort**: [Time estimate]

## Medium Priority

...

## Low Priority / Debatable

...
```

**Search for**:
- Duplicate validation logic
- Copy/pasted methods
- Knowledge stored in multiple places
- Similar formatting code

Prioritize and fix top 3.

### Action Item 5: Apply Rule of Three (Ongoing)

**Task**: Before extracting duplication, ask:

1. Does this appear in 3+ places?
2. Does it represent the SAME knowledge?
3. Will these always change together?

If yes to all three: **Refactor**
If no: **Wait** for pattern to emerge

Document your decision in commit message.

---

## Key Takeaways

### 1. DRY is About Knowledge, Not Code

Two identical lines might be fine. Two representations of the same knowledge are not.

### 2. Recognize the Four Types

- **Imposed**: Seems unavoidable (often not!)
- **Inadvertent**: Didn't realize it's duplication
- **Impatient**: Lazy copy/paste
- **Interdeveloper**: Lack of communication

### 3. Not All Duplication is Bad

- Different abstractions (domain vs. DTO)
- Test independence
- Transitional duplication during refactoring

### 4. Use the Rule of Three

Don't prematurely abstract. Wait for the pattern to appear three times.

### 5. Extract to Appropriate Level

- Duplicate lines? Extract method
- Duplicate methods? Extract class
- Duplicate validation? Use value objects

---

## Study Resources

### Books
1. **"The Pragmatic Programmer"** - Chapter on DRY
   - Original source of the principle
   - Detailed examples

2. **"Refactoring" by Martin Fowler**
   - Chapter 3: "Bad Smells in Code" - Duplicated Code
   - Refactoring techniques

3. **"Clean Code" by Robert C. Martin**
   - Chapter 17: Smell - G5 (Duplication)

### Articles
1. **"Rule of Three" by Martin Fowler**
   - When to refactor duplication
   - https://martinfowler.com/refactoring/catalog/

2. **"AHA Programming" by Kent C. Dodds**
   - "Avoid Hasty Abstractions"
   - Counter to DRY dogma

### Videos
1. **"The Magic Tricks of Testing" by Sandi Metz** (RailsConf 2013)
   - Discusses when duplication in tests is good

---

## Coming Next

**Section 20: Orthogonality** - Eliminate effects between unrelated things

We'll explore:
- What orthogonality means in software
- How to identify coupling
- Refactoring Bibby services for independence
- Testing orthogonality

---

**Section 19 Complete** | **Time Invested**: 3-4 hours | **Files to Create**: `DRY_VIOLATIONS.md`, `BookValidator.java`

DRY isn't about eliminating all duplication - it's about having a **single source of truth for each piece of knowledge**. Apply it wisely, and your codebase will be easier to change, understand, and maintain.
