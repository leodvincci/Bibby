# Section 4: Methods & Parameters

Welcome to Section 4! Methods are the **verbs** of your program - they define what your objects can do. Understanding how parameters work, how to name methods clearly, and how to design method signatures is crucial for writing maintainable code.

Your Bibby application has some excellent method designs and a few confusing ones. Let's learn from both!

---

## What Are Methods?

A method is a **block of code that performs a specific task**. Methods have:

**Signature Components:**
```java
public Optional<BookEntity> findBookByTitle(String title) {
  ↑        ↑                  ↑              ↑      ↑
  |        |                  |              |      |
Access  Return              Name         Parameter Parameter
Modifier Type                              Type     Name
```

- **Access Modifier:** `public`, `private`, `protected`, package-private
- **Return Type:** What the method gives back (or `void`)
- **Name:** What the method does (verb-based)
- **Parameters:** Input values the method needs
- **Body:** The actual code that executes

---

## Pass-By-Value: Java's Biggest Misconception

**THE TRUTH:** Java is **always pass-by-value**. Always. No exceptions.

**But wait...** objects seem to be passed by reference, right? Let's clarify.

### For Primitives (Simple)

```java
public void increment(int number) {
    number = number + 1; // Modifies local copy only
}

int value = 5;
increment(value);
System.out.println(value); // Still 5! Not 6!
```

The **value** of `value` (5) is copied into `number`. Changes to `number` don't affect `value`.

### For Objects (The Confusing Part)

```java
public void updateTitle(BookEntity book) {
    book.setTitle("New Title"); // This DOES change the original!
}

BookEntity myBook = new BookEntity();
myBook.setTitle("Old Title");
updateTitle(myBook);
System.out.println(myBook.getTitle()); // "New Title" - it changed!
```

**What's happening?**
- `myBook` is a **reference** (memory address) pointing to a BookEntity object
- When you call `updateTitle(myBook)`, the **value of the reference** is copied into `book`
- Both `myBook` and `book` now point to the **same object** in memory
- Changes through `book` affect the same object that `myBook` points to

**Visual:**
```
Stack:                    Heap:
myBook ──────┐            ┌─────────────────┐
             └───────────→│  BookEntity     │
book   ──────┐            │  title: "..."   │
             └───────────→│                 │
                          └─────────────────┘
```

### Proof It's Pass-By-Value (Not Reference)

```java
public void reassignBook(BookEntity book) {
    book = new BookEntity(); // Creates NEW object, assigns to book
    book.setTitle("New Book");
}

BookEntity myBook = new BookEntity();
myBook.setTitle("Original");
reassignBook(myBook);
System.out.println(myBook.getTitle()); // Still "Original"!
```

If Java were pass-by-reference, `myBook` would now point to the new BookEntity. But it doesn't - because `book` is a **copy** of the reference.

---

## In Your Code: Pass-By-Value Examples

### ✅ Understanding Value Passing

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 56-62

```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**What's Happening:**
- `bookEntity` is a copy of the reference passed in
- `bookEntity.setBookStatus()` modifies the object it points to
- The caller's reference still points to the same object
- **The changes persist** because you're modifying the same object

**This is correct!** You modify the entity and save it.

---

### ⚠️ Confusing Method Signature

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 54-56

```java
public void setAuthors(AuthorEntity authors) {
    this.authors.add(authors);
}
```

**The Problem:**

The method is named `setAuthors` (plural) and the parameter is named `authors` (plural), but:
- It doesn't SET the authors (replace them)
- It ADDS one author to the set
- Parameter name is plural but it's a single AuthorEntity

**Confusion:**
```java
BookEntity book = new BookEntity();
AuthorEntity tolkien = new AuthorEntity("J.R.R.", "Tolkien");
book.setAuthors(tolkien); // Sounds like it sets ALL authors to just tolkien

// But actually it adds tolkien to the existing set!
```

**Refactored:**

```java
// OPTION 1: Rename to match behavior
public void addAuthor(AuthorEntity author) {
    this.authors.add(author);
}

// OPTION 2: Provide both methods
public void setAuthors(Set<AuthorEntity> authors) {
    this.authors = authors; // Actually replaces
}

public void addAuthor(AuthorEntity author) {
    this.authors.add(author); // Adds one
}

// OPTION 3: Builder pattern (advanced)
public BookEntity withAuthor(AuthorEntity author) {
    this.authors.add(author);
    return this; // Enables chaining
}

// Usage:
book.withAuthor(tolkien).withAuthor(lewis);
```

**What Changed:**
- ✅ Method name matches behavior (`addAuthor` adds one)
- ✅ Parameter name is singular (`author` not `authors`)
- ✅ Clear intent (add vs set)
- ✅ Option to set entire collection separately

---

## Method Overloading

**Method overloading** means multiple methods with the same name but different parameters.

### ✅ Good Overloading Example

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseEntity.java`
📍 Lines: 15-27

```java
public BookcaseEntity(String bookcaseLabel, int shelfCapacity) {
    this.bookcaseLabel = bookcaseLabel;
    this.shelfCapacity = shelfCapacity;
}

public BookcaseEntity(Long bookcaseId, String bookcaseLabel, int shelfCapacity) {
    this.bookcaseId = bookcaseId;
    this.bookcaseLabel = bookcaseLabel;
    this.shelfCapacity = shelfCapacity;
}

public BookcaseEntity() {

}
```

**Why This Works:**

You have **three constructors**:
1. **No-args** - Required by JPA for entity instantiation
2. **Label + Capacity** - For creating new bookcases (no ID yet)
3. **ID + Label + Capacity** - For reconstructing from database (has ID)

**How Java Chooses:**

```java
// Calls constructor #1 (no args)
BookcaseEntity bc1 = new BookcaseEntity();

// Calls constructor #2 (2 parameters)
BookcaseEntity bc2 = new BookcaseEntity("Main Bookcase", 5);

// Calls constructor #3 (3 parameters)
BookcaseEntity bc3 = new BookcaseEntity(1L, "Main Bookcase", 5);
```

Java matches based on:
- Number of parameters
- Type of parameters
- Order of parameters

---

### ⚠️ Constructor Overloading Issue

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 30-36

```java
public BookEntity() {
}

public BookEntity(String title, Set<AuthorEntity> authors) {
    this.title = title;
    this.authors = authors;
}
```

**The Problem:**

Your second constructor sets `title` and `authors`, but:
- Doesn't set other required fields (ISBN, publisher, etc.)
- Creates partially initialized objects
- Makes it easy to forget required data

**Example of the Problem:**

```java
BookEntity book = new BookEntity("The Hobbit", authorSet);
// Now what?
// ISBN? null
// Publisher? null
// Genre? null
// Status? null
// This book is incomplete!
```

**Better Approach - Builder Pattern (Advanced):**

```java
// Builder class (nested inside BookEntity)
public static class Builder {
    private String title;
    private Set<AuthorEntity> authors = new HashSet<>();
    private String isbn;
    private String publisher;
    private Integer publicationYear;

    public Builder withTitle(String title) {
        this.title = title;
        return this;
    }

    public Builder withAuthor(AuthorEntity author) {
        this.authors.add(author);
        return this;
    }

    public Builder withIsbn(String isbn) {
        this.isbn = isbn;
        return this;
    }

    // ... other with methods ...

    public BookEntity build() {
        // Validate required fields
        if (title == null || title.isBlank()) {
            throw new IllegalStateException("Title is required");
        }
        if (isbn == null) {
            throw new IllegalStateException("ISBN is required");
        }

        BookEntity book = new BookEntity();
        book.title = this.title;
        book.authors = this.authors;
        book.isbn = this.isbn;
        book.publisher = this.publisher;
        book.publicationYear = this.publicationYear;
        return book;
    }
}

// Usage:
BookEntity book = new BookEntity.Builder()
    .withTitle("The Hobbit")
    .withAuthor(tolkien)
    .withIsbn("978-0547928227")
    .withPublisher("Houghton Mifflin")
    .withPublicationYear(1937)
    .build(); // Validates and creates
```

**Simpler Alternative - All-Args Constructor:**

```java
public BookEntity(
    String title,
    Set<AuthorEntity> authors,
    String isbn,
    String publisher,
    Integer publicationYear,
    String genre,
    Integer edition
) {
    // Validate
    if (title == null || title.isBlank()) {
        throw new IllegalArgumentException("Title cannot be blank");
    }
    if (isbn == null) {
        throw new IllegalArgumentException("ISBN cannot be null");
    }

    // Assign
    this.title = title;
    this.authors = authors != null ? authors : new HashSet<>();
    this.isbn = isbn;
    this.publisher = publisher;
    this.publicationYear = publicationYear;
    this.genre = genre;
    this.edition = edition;
    this.bookStatus = "AVAILABLE"; // Default
    this.checkoutCount = 0; // Default
    this.createdAt = LocalDate.now();
}
```

---

## Parameter Validation

**The Rule:** Validate at the boundary (public methods, constructors).

### ⚠️ Missing Validation

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 64-66

```java
public void setTitle(String title) {
    this.title = title;
}
```

**What Could Go Wrong:**

```java
book.setTitle(null);     // NullPointerException when you use it later!
book.setTitle("");       // Empty title - is that valid?
book.setTitle("   ");    // Whitespace only - probably invalid
```

**Refactored with Validation:**

```java
public void setTitle(String title) {
    if (title == null || title.isBlank()) {
        throw new IllegalArgumentException("Title cannot be null or blank");
    }
    this.title = title.trim(); // Remove leading/trailing whitespace
}
```

**Benefits:**
- ✅ Fail fast (catch errors immediately, not later)
- ✅ Clear error message (developer knows what's wrong)
- ✅ Data integrity (invalid state prevented)

---

### ⚠️ No Null Check Before Use

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java`
📍 Lines: 43-49

```java
public void addShelf(BookcaseEntity bookcaseEntity, int label, int position){
    ShelfEntity shelfEntity = new ShelfEntity();
    shelfEntity.setBookcaseId(bookcaseEntity.getBookcaseId());
    //                        ^^^^^^^^^^^^^^^^^^^^^^^^^
    // What if bookcaseEntity is null? NullPointerException!
    // What if bookcaseEntity.getBookcaseId() returns null? ShelfEntity has null ID!
    shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());
    shelfEntity.setShelfPosition(position);
    shelfRepository.save(shelfEntity);
}
```

**Refactored with Validation:**

```java
public void addShelf(BookcaseEntity bookcaseEntity, int label, int position) {
    // Validate parameters
    if (bookcaseEntity == null) {
        throw new IllegalArgumentException("BookcaseEntity cannot be null");
    }
    if (bookcaseEntity.getBookcaseId() == null) {
        throw new IllegalStateException("Bookcase must have an ID (must be persisted first)");
    }
    if (label < 0) {
        throw new IllegalArgumentException("Label cannot be negative");
    }
    if (position < 0) {
        throw new IllegalArgumentException("Position cannot be negative");
    }

    ShelfEntity shelfEntity = new ShelfEntity();
    shelfEntity.setBookcaseId(bookcaseEntity.getBookcaseId());
    shelfEntity.setShelfLabel("Shelf " + label);
    shelfEntity.setShelfPosition(position);
    shelfRepository.save(shelfEntity);
}
```

**Bonus Observation:**

```java
"Shelf " + Integer.valueOf(label).toString()
```

This is unnecessarily complex! `Integer.valueOf(label).toString()` just converts `int → Integer → String`.

**Simplified:**
```java
"Shelf " + label  // Java auto-converts int to String
// Or more explicit:
"Shelf " + String.valueOf(label)
```

---

## Return Type Choices

### ⚠️ Returning Null vs Optional

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 43-54

```java
public BookEntity findBookByTitle(String title){
    Optional<BookEntity> bookEntity = Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    for(BookEntity b : bookEntities){
        System.out.println(b.getTitle());
    }

    if(bookEntity.isEmpty()){
        return null; // ← RETURNING NULL!
    }
    return bookEntity.get();
}
```

**The Problem:**

You create an `Optional` internally, then return `null`! This defeats the purpose of Optional.

**Caller's Perspective:**

```java
// Caller has to null-check
BookEntity book = bookService.findBookByTitle("The Hobbit");
if (book == null) { // Could forget this check!
    System.out.println("Not found");
}
```

**Better - Return Optional:**

```java
public Optional<BookEntity> findBookByTitle(String title) {
    Optional<BookEntity> bookEntity = Optional.ofNullable(
        bookRepository.findByTitleIgnoreCase(title)
    );

    // Debug: print similar titles
    List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
    bookEntities.forEach(b -> System.out.println(b.getTitle()));

    return bookEntity; // Return the Optional!
}

// Caller uses Optional methods
bookService.findBookByTitle("The Hobbit")
    .ifPresentOrElse(
        book -> System.out.println("Found: " + book.getTitle()),
        () -> System.out.println("Not found")
    );
```

**Benefits:**
- ✅ Type system enforces handling (Optional is explicit)
- ✅ Can't accidentally use null value (compiler helps)
- ✅ Chainable methods (map, flatMap, orElse, etc.)

---

### ⚠️ Void Methods That Could Return Values

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 65-67

```java
public void updateBook(BookEntity bookEntity){
    bookRepository.save(bookEntity);
}
```

**The Problem:**

`bookRepository.save()` **returns** the saved entity (which might have been updated by JPA, e.g., generated ID, updated timestamp). You're throwing away that return value!

**Refactored:**

```java
public BookEntity updateBook(BookEntity bookEntity) {
    return bookRepository.save(bookEntity);
}

// Caller can use the returned entity
BookEntity updatedBook = bookService.updateBook(book);
System.out.println("Updated at: " + updatedBook.getUpdatedAt());
```

**Why Return It:**
- JPA might modify the entity (generated IDs, timestamps)
- Caller might need the "fresh" version from database
- Enables method chaining
- Explicit about what happened (return value = success)

---

### ⚠️ Void for Side-Effect Methods

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 73-77

```java
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);
    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());
    updateBook(bookEntity);
}
```

**Hidden Dangers:**

1. `findBookByTitle` can return `null` → NullPointerException!
2. No return value → Caller doesn't know if it succeeded
3. No validation → What if bookTitle is null or blank?

**Refactored:**

```java
public BookEntity checkInBook(String bookTitle) {
    // Validate parameter
    if (bookTitle == null || bookTitle.isBlank()) {
        throw new IllegalArgumentException("Book title cannot be null or blank");
    }

    // Find book
    BookEntity bookEntity = findBookByTitle(bookTitle);

    // Guard: Check if found
    if (bookEntity == null) {
        throw new IllegalArgumentException("Book not found: " + bookTitle);
    }

    // Guard: Check if already available
    if (BookStatus.AVAILABLE.toString().equals(bookEntity.getBookStatus())) {
        throw new IllegalStateException("Book is already available: " + bookTitle);
    }

    // Update status
    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());

    // Save and return
    return updateBook(bookEntity);
}

// Caller knows it succeeded
try {
    BookEntity book = bookService.checkInBook("The Hobbit");
    System.out.println("Checked in: " + book.getTitle());
} catch (IllegalArgumentException e) {
    System.out.println("Error: " + e.getMessage());
}
```

---

## Method Naming Conventions

### ✅ Good Method Names

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`

```java
public void createNewBook(...)         // Action: create
public BookEntity findBookByTitle(...) // Query: find + criteria
public void checkOutBook(...)          // Action: checkOut
public void checkInBook(...)           // Action: checkIn
public List<BookEntity> findBooksByShelf(...) // Query: findBy + plural
```

**Why These Are Good:**
- ✅ Verb-based (createNew, find, checkOut, checkIn)
- ✅ Clear intent (you know what they do)
- ✅ Consistent pattern (findBy... for queries)
- ✅ Plurals for collections (findBooks, not findBook)

---

### ⚠️ Inconsistent Naming

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 22-41 vs 43-54

```java
public void createNewBook(BookRequestDTO bookRequestDTO) {
    // Creates new book
}

public BookEntity findBookByTitle(String title) {
    // Finds existing book
}
```

**Inconsistency:**

- `createNewBook` → `void` return type
- `findBookByTitle` → `BookEntity` return type

Both are **command methods** (do something), but inconsistent returns. Why?

**More Consistent:**

```java
// Option 1: Both return entities
public BookEntity createNewBook(BookRequestDTO bookRequestDTO) {
    // ... create logic ...
    return bookRepository.save(bookEntity);
}

public Optional<BookEntity> findBookByTitle(String title) {
    // ... find logic ...
    return Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
}

// Option 2: Both void (not recommended for queries!)
public void createNewBook(...) { }
public void findBookByTitle(...) { } // ← How do you get the result?
```

**Recommendation:** Queries should return values, commands can return void OR the modified entity.

---

## Parameter Names and Order

### ✅ Clear Parameter Names

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java`
📍 Lines: 25-40

```java
public String createNewBookCase(String label, int capacity){
    // Parameters are clearly named
    // label = bookcase label
    // capacity = number of shelves
}
```

**Why This Is Good:**
- ✅ Short but descriptive
- ✅ Can't confuse order (label vs capacity are different types)
- ✅ Reading the call is clear: `createNewBookCase("Main", 5)`

---

### ⚠️ Parameter Order Confusion

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java`
📍 Lines: 43-49

```java
public void addShelf(BookcaseEntity bookcaseEntity, int label, int position){
    //                                                ^^^^^^^  ^^^^^^^^
    //                                                Both are ints - easy to swap!
    shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());
    shelfEntity.setShelfPosition(position);
}
```

**The Danger:**

```java
addShelf(bookcase, 5, 3);  // label=5, position=3
addShelf(bookcase, 3, 5);  // label=3, position=5 - WRONG ORDER!
// Compiler can't catch this - both are ints!
```

**Better Approach:**

**Option 1: Use descriptive types**
```java
public record ShelfLabel(String value) {}
public record ShelfPosition(int value) {}

public void addShelf(
    BookcaseEntity bookcaseEntity,
    ShelfLabel label,
    ShelfPosition position
) {
    // Now you can't mix them up!
    shelfEntity.setShelfLabel(label.value());
    shelfEntity.setShelfPosition(position.value());
}

// Usage:
addShelf(bookcase, new ShelfLabel("A1"), new ShelfPosition(0));
// Can't accidentally swap - compiler error!
```

**Option 2: Single parameter object**
```java
public record ShelfInfo(String label, int position) {}

public void addShelf(BookcaseEntity bookcaseEntity, ShelfInfo info) {
    shelfEntity.setShelfLabel(info.label());
    shelfEntity.setShelfPosition(info.position());
}

// Usage:
addShelf(bookcase, new ShelfInfo("A1", 0));
```

**Option 3: Make parameter names match setters (current approach is okay)**
```java
// Just being very careful at call sites
addShelf(bookcase, /* label= */ 0, /* position= */ 0);
```

---

## Method Length and Single Responsibility

### ⚠️ Method Doing Too Much

📁 File: `src/main/java/com/penrose/bibby/library/book/BookService.java`
📍 Lines: 22-41

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    String firstName = bookRequestDTO.firstName();
    String lastName = bookRequestDTO.lastName();
    String title = bookRequestDTO.title();
    BookEntity bookEntity = bookRepository.findByTitle(title);
    AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(firstName, lastName);

    if (authorEntity == null) {
        authorEntity = new AuthorEntity(firstName, lastName);
        authorRepository.save(authorEntity);
    }

    if (bookEntity == null) {
        bookEntity = new BookEntity();
        bookEntity.setTitle(title);
    }
    bookEntity.setAuthors(authorEntity);
    bookRepository.save(bookEntity);
}
```

**This Method:**
1. Extracts data from DTO
2. Finds or creates an author
3. Finds or creates a book
4. Links them together
5. Saves the book

That's **5 responsibilities**! Let's refactor:

**Refactored - Single Responsibility:**

```java
@Transactional
public BookEntity createNewBook(BookRequestDTO bookRequestDTO) {
    // Extract data
    String firstName = bookRequestDTO.firstName();
    String lastName = bookRequestDTO.lastName();
    String title = bookRequestDTO.title();

    // Find or create author
    AuthorEntity author = findOrCreateAuthor(firstName, lastName);

    // Find or create book
    BookEntity book = findOrCreateBook(title);

    // Link and save
    book.addAuthor(author); // Renamed from setAuthors!
    return bookRepository.save(book);
}

private AuthorEntity findOrCreateAuthor(String firstName, String lastName) {
    return Optional.ofNullable(
        authorRepository.findByFirstNameAndLastName(firstName, lastName)
    ).orElseGet(() -> {
        AuthorEntity author = new AuthorEntity(firstName, lastName);
        return authorRepository.save(author);
    });
}

private BookEntity findOrCreateBook(String title) {
    return Optional.ofNullable(
        bookRepository.findByTitle(title)
    ).orElseGet(() -> {
        BookEntity book = new BookEntity();
        book.setTitle(title);
        return book; // Don't save yet - caller will save
    });
}
```

**Benefits:**
- ✅ Each method has one clear purpose
- ✅ Easier to test (can test findOrCreateAuthor independently)
- ✅ Reusable (findOrCreateAuthor can be used elsewhere)
- ✅ Main method reads like a story

---

## Key Takeaways

**Method Design Principles:**

1. **Pass-By-Value Always** - Java passes copies of primitives and copies of references
2. **Name Methods Clearly** - Verb-based, describes what it does
3. **Validate Parameters** - Check at public boundaries, fail fast
4. **Return Meaningful Values** - Prefer Optional over null, return modified entities
5. **Single Responsibility** - One method, one purpose
6. **Overload Thoughtfully** - Different parameters should mean different use cases

**Parameter Guidelines:**

1. **Limit Parameters** - Ideally 0-3, max 5 (consider parameter object beyond that)
2. **Avoid Same-Type Parameters** - Easy to accidentally swap (use custom types)
3. **Validate Early** - Null checks, range checks, state checks
4. **Name Descriptively** - Short but clear

**Return Type Decision Tree:**

```
Does the method always succeed with a value?
├─ Yes: Return the type directly (BookEntity, String, etc.)
└─ No: Might not find/produce a value?
    ├─ Yes: Return Optional<T>
    └─ No: Pure side-effect?
        ├─ Yes: void (but consider returning success indicator)
        └─ No: Return modified entity for fluent chains
```

---

## Practice Exercise: Refactor BookService Methods

**Your Task:**

Refactor `BookService` to improve method signatures and parameter handling.

**Part 1: Fix findBookByTitle**

Current:
```java
public BookEntity findBookByTitle(String title)
```

Issues:
- Returns null instead of Optional
- Creates Optional internally then unwraps it
- No parameter validation

Refactor to:
```java
public Optional<BookEntity> findBookByTitle(String title) {
    // Add validation
    // Return Optional
    // Remove unnecessary Optional creation and unwrapping
}
```

**Part 2: Fix updateBook**

Current:
```java
public void updateBook(BookEntity bookEntity)
```

Issues:
- Returns void (throws away saved entity)
- No parameter validation

Refactor to:
```java
public BookEntity updateBook(BookEntity bookEntity) {
    // Add validation
    // Return saved entity
}
```

**Part 3: Extract Methods from createNewBook**

Current:
- Single method with 5 responsibilities

Refactor to:
- `createNewBook` (main method)
- `findOrCreateAuthor` (helper)
- `findOrCreateBook` (helper)

**Solution:**

```java
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    // Part 1: Fixed findBookByTitle
    public Optional<BookEntity> findBookByTitle(String title) {
        // Validate parameter
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }

        // Return Optional directly
        return Optional.ofNullable(bookRepository.findByTitleIgnoreCase(title));
    }

    // Part 2: Fixed updateBook
    public BookEntity updateBook(BookEntity bookEntity) {
        // Validate parameter
        if (bookEntity == null) {
            throw new IllegalArgumentException("BookEntity cannot be null");
        }
        if (bookEntity.getBookId() == null) {
            throw new IllegalStateException("Cannot update book without ID (use create for new books)");
        }

        // Return saved entity
        return bookRepository.save(bookEntity);
    }

    // Part 3: Refactored createNewBook
    @Transactional
    public BookEntity createNewBook(BookRequestDTO bookRequestDTO) {
        // Validate DTO
        if (bookRequestDTO == null) {
            throw new IllegalArgumentException("BookRequestDTO cannot be null");
        }

        // Extract data
        String firstName = bookRequestDTO.firstName();
        String lastName = bookRequestDTO.lastName();
        String title = bookRequestDTO.title();

        // Find or create author
        AuthorEntity author = findOrCreateAuthor(firstName, lastName);

        // Find or create book
        BookEntity book = findOrCreateBook(title);

        // Link and save
        book.addAuthor(author); // Note: renamed from setAuthors
        return bookRepository.save(book);
    }

    private AuthorEntity findOrCreateAuthor(String firstName, String lastName) {
        return Optional.ofNullable(
            authorRepository.findByFirstNameAndLastName(firstName, lastName)
        ).orElseGet(() -> {
            AuthorEntity author = new AuthorEntity(firstName, lastName);
            return authorRepository.save(author);
        });
    }

    private BookEntity findOrCreateBook(String title) {
        return Optional.ofNullable(
            bookRepository.findByTitle(title)
        ).orElseGet(() -> {
            BookEntity book = new BookEntity();
            book.setTitle(title);
            return book; // Caller will save
        });
    }
}
```

---

## Action Items for This Week

**1. Rename setAuthors to addAuthor**
**Priority:** HIGH
**Estimated Time:** 10 minutes
**File:** `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
**Lines:** 54-56

Change:
```java
public void setAuthors(AuthorEntity authors)
```

To:
```java
public void addAuthor(AuthorEntity author)
```

And update all call sites in BookService.

**2. Add Parameter Validation to Service Methods**
**Priority:** HIGH
**Estimated Time:** 30 minutes
**Files:** BookService.java, BookcaseService.java

Add validation to public methods:
- Null checks for object parameters
- Blank checks for String parameters
- Range checks for numeric parameters

Methods to validate:
- `BookService.createNewBook()`
- `BookService.findBookByTitle()`
- `BookService.checkInBook()`
- `BookcaseService.addShelf()`

**3. Refactor Methods to Return Values**
**Priority:** MEDIUM
**Estimated Time:** 20 minutes
**File:** `src/main/java/com/penrose/bibby/library/book/BookService.java`

Change:
- `updateBook` to return `BookEntity`
- `findBookByTitle` to return `Optional<BookEntity>`
- `checkInBook` to return `BookEntity`

Update callers to use returned values.

---

## Further Study

**Books:**
- *Clean Code* by Robert Martin - Chapter 3: "Functions"
- *Effective Java* (3rd Ed) by Joshua Bloch - Item 49: "Check parameters for validity"
- *Effective Java* - Item 50: "Make defensive copies when needed"

**Articles:**
- Baeldung: "Guide to the Java 8 Optional" - https://www.baeldung.com/java-optional
- Oracle: "Returning a Value from a Method"
- Martin Fowler: "Replace Parameter with Method" refactoring

**Concepts:**
- Single Responsibility Principle (SRP)
- Command-Query Separation (CQS)
- Parameter Object pattern
- Builder pattern for complex construction

---

## Summary

You've learned:
- ✅ Java is always pass-by-value (copies of primitives, copies of references)
- ✅ Method overloading requires different parameter types/counts
- ✅ Parameter validation should happen at public boundaries
- ✅ Return Optional instead of null for queries
- ✅ Return modified entities from update methods
- ✅ Name methods as verbs that describe what they do
- ✅ Extract methods to achieve single responsibility

**Your code showed:**
- **Strengths:** Good constructor overloading, clear method names, transactional boundaries
- **Opportunities:** Missing validation, confusing method names (setAuthors), returning null instead of Optional

**Next Up:** Section 5 - OOP: Encapsulation & Abstraction (examining access modifiers, information hiding, and interface design in your entities and services)

---

*Section created: 2025-11-17*
*Files analyzed: BookService, BookEntity, BookcaseService, BookcaseEntity*
*Method issues identified: 12*
*Refactoring patterns provided: 8*
