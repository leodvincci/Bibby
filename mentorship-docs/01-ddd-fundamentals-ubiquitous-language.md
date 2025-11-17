# SECTION 1: DDD FUNDAMENTALS & UBIQUITOUS LANGUAGE

## Your Senior Engineer's Introduction

Welcome to your journey toward software engineering mastery. I'm going to be direct with you: the code you've written for Bibby is functional, but it doesn't yet speak the language of your domain. That's not a criticism—it's an opportunity. Over the next several weeks, we're going to transform this codebase from a data-management script into a rich, expressive domain model that reads like the language a librarian would use.

Let me start with why this matters.

---

## What is Domain Driven Design?

**Domain Driven Design (DDD)** is a software development philosophy created by Eric Evans. At its core, DDD makes one bold claim: **the most important part of software is understanding and modeling the business domain, not the technical implementation.**

Think about it this way: you're building Bibby to manage your personal library. The *domain* is "personal library management" - the world of books, shelves, bookcases, checking books out, organizing collections, tracking what you're reading. That's the domain.

DDD says: **Model that domain explicitly in your code. Make your code speak the language of librarians, not the language of database tables.**

### Why DDD Matters for Your Career

As a junior engineer, you might be thinking "isn't it enough that my code works?" Here's the truth: code that "just works" becomes unmaintainable within months. Professional software engineering is about:

1. **Communication** - Your code communicates intent to other developers (including future you)
2. **Maintainability** - You'll spend 80% of your time reading/modifying code, 20% writing new code
3. **Scalability** - Not just technical scalability, but the ability to add features without breaking everything
4. **Business alignment** - Code that mirrors the business domain is easier to change when business requirements change

DDD provides patterns and principles to achieve all of this.

### Strategic Design vs. Tactical Design

DDD operates at two levels:

**Strategic Design** (the big picture):
- Identifying bounded contexts (what parts of your system are separate?)
- Context mapping (how do these parts interact?)
- Defining the core domain vs. supporting subdomains
- Architecture decisions

**Tactical Design** (the implementation patterns):
- Entities and Value Objects
- Aggregates and Aggregate Roots
- Repositories
- Domain Services
- Domain Events

We'll cover both, but we're starting with the foundation: **speaking the language of the domain**.

---

## Ubiquitous Language: The Foundation of Everything

Here's a scenario: You're explaining Bibby to a friend who loves books. You'd say something like:

> "I have a library in my basement with several bookcases. Each bookcase has multiple shelves. Books are placed on shelves. I can check out a book when I'm reading it, then check it back in when I'm done. I can search my collection by title or author."

Notice what you *didn't* say:

> "I have a database with a books table that has a foreign key to a shelves table, which has a foreign key to a bookcases table. I update the book_status column to 'CHECKED_OUT' when I execute the checkout transaction."

The first description uses **ubiquitous language** - the natural language of the domain. The second uses **technical implementation language**.

### The Ubiquitous Language Principle

**Ubiquitous Language** means:
- Use the same terminology in code that domain experts (in this case, you as a librarian) use in conversation
- No translation layer between "business speak" and "tech speak"
- Class names, method names, variable names should all sound like domain concepts
- If you wouldn't say it when describing your library to a friend, it shouldn't be in your domain model

This isn't just about naming - it's about creating a shared mental model between the problem space and the solution space.

---

## Analyzing Bibby's Current Language

Let's look at what your code is currently saying. I'm going to be your code reviewer and point out where the language breaks down.

### Discovery 1: "BookEntity" vs "Book" - A Confused Vocabulary

In `com.penrose.bibby.library.book`, you have two classes:
- `Book.java` - A domain model with getters/setters
- `BookEntity.java` - A JPA entity annotated with `@Entity`

**Question for you to think about**: If someone asked "What's a book in your library?" which class would you point to? What's the difference between a "Book" and a "BookEntity"?

The truth is, in ubiquitous language, there's only one concept: **a Book**.

**The Problem**: You've split your domain concept into two classes because of technical concerns (JPA annotations). This violates the ubiquitous language principle. Your domain is leaking technical infrastructure concerns.

**What this tells me**: You're thinking in terms of "things in the database" rather than "things in my library domain." This is one of the most common mistakes junior engineers make, and you're not alone.

### Discovery 2: Primitive Obsession - ISBN as String

Let's look at `BookEntity.java:18`:

```java
private String isbn;
```

And in the service layer (`BookService.java`), ISBN is handled as a plain String throughout.

**Question**: What's an ISBN in the real world?

An ISBN (International Standard Book Number) isn't just any string - it's a very specific type of identifier with:
- A defined format (ISBN-10 or ISBN-13)
- Validation rules (check digits, proper grouping)
- Specific meaning in the domain

By representing it as `String isbn`, you're saying "ISBN is just any text." But in ubiquitous language, a librarian would say "ISBN is a standardized book identifier."

**The Problem**: This is called **primitive obsession** - using primitive types (String, int, Long) for domain concepts that deserve their own identity.

**Why it matters**:
1. Nothing prevents you from setting `isbn = "banana"` - there's no type safety
2. Validation logic has nowhere to live (do you validate in the service? In the controller? Everywhere?)
3. The concept remains implicit rather than explicit

**What you should have**: An `ISBN` value object that encapsulates the rules and behavior of ISBNs.

### Discovery 3: "shelfId" - Lost Relationships

In `BookEntity.java:24`:

```java
private Long shelfId;
```

**The Language Problem**: In domain language, you don't say "a book has a shelf ID." You say "a book is placed on a shelf" or "a book resides on a shelf."

By using `shelfId`, you're thinking in terms of foreign keys and database joins. But in the domain, the relationship is richer than that - a Book has a *location* on a specific Shelf, which is in a specific Bookcase, which is in a specific location in your home.

**Current state**: Technical language (foreign key thinking)
**Target state**: Domain language (relationship thinking)

### Discovery 4: "bookStatus" as String - Missing Domain Concepts

In `BookEntity.java:26`:

```java
private String bookStatus;
```

And then in `BookService.java:57-58`:

```java
if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
    bookEntity.setBookStatus("CHECKED_OUT");
```

**The Confusion**: You have a `BookStatus` enum (good!), but you're storing it as a String and comparing using `.toString()`. This is halfway to good domain modeling.

**In ubiquitous language**: A book isn't in a "status" - it has an *availability state*. A librarian would say:
- "This book is available"
- "I've checked out this book"
- "I'm currently reading this book"

These are states with meaning and behavior, not just strings.

### Discovery 5: "checkOutBook()" - Anemic Domain Model

Look at `BookService.java:56-62`:

```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**Question**: In the real world, if someone asks "How do I check out a book?", would they ask the library service desk, or would they ask the book itself?

The answer reveals a fundamental issue: **The Book has no behavior**. It's just a bag of data (getters/setters). All the behavior lives in `BookService`.

**In ubiquitous language**: You'd say "I checked out the book" or "the book was checked out." The *book* is the subject performing (or experiencing) the action.

**The Problem**: This is called an **anemic domain model** - domain objects with no behavior, just data. All logic lives in service classes. This is a procedural programming style, not object-oriented design.

---

## Summary of Language Problems in Your Current Code

Let me compile what we've found:

| Current Code | Language Issue | What It Should Be |
|--------------|----------------|-------------------|
| `BookEntity` vs `Book` | Technical terms in domain | Just `Book` (hide infrastructure) |
| `String isbn` | Primitive obsession | `ISBN` value object |
| `Long shelfId` | Foreign key thinking | `Shelf shelf` (relationship) |
| `String bookStatus` | Weak typing | `BookStatus` enum (properly used) |
| `BookService.checkOutBook()` | Anemic domain model | `book.checkOut()` (behavior in entity) |
| `setTitle()`, `setIsbn()` | CRUD thinking | Behavior-focused methods |
| `AuthorEntity` | Technical term | Just `Author` |
| `updateBook()` | Technical operation | Domain-meaningful methods |

### The Pattern I See

You're thinking in **CRUD operations** (Create, Read, Update, Delete) and **database tables**, not in **domain behaviors** and **domain concepts**.

This isn't wrong for a simple CRUD app, but Bibby is more than that. You have a rich domain:
- Books that can be checked out and returned
- Shelves that hold books and have capacity limits
- Bookcases that organize your physical space
- A personal library with search, organization, and tracking capabilities

That richness is currently hidden behind `getters`, `setters`, and `service` classes.

---

## What is the Domain of Bibby?

Before we can use ubiquitous language, we need to understand the domain deeply. Let's do a domain discovery exercise.

### Domain Discovery Questions

**1. What is the core purpose of Bibby?**
- To manage your personal library of physical books
- To help you locate books in your physical space
- To track which books you own, what you're reading, and what's available

**2. Who are the domain experts?**
- You! You're both the developer and the domain expert
- Think like a librarian, not a programmer

**3. What are the key domain concepts?**

Let's list them in domain language:

**Physical Organization**:
- **Book** - A physical book in your collection (not a database record)
- **Author** - The person who wrote a book (books can have multiple authors)
- **Shelf** - A physical shelf that holds books
- **Bookcase** - A piece of furniture containing multiple shelves
- **Location** - Where a bookcase is in your home (basement, office, etc.)

**Collection Management**:
- **Personal Library** (or **Collection**) - The complete set of books you own
- **Catalog** - A way to organize and find books
- **Genre** - A category for organizing books

**Reading & Tracking**:
- **Checkout** - Taking a book to read it
- **Checkin** - Returning a book to its location
- **Reading Status** - What you're currently reading, finished, want to read
- **Checkout History** - Tracking how often you read each book

### Defining the Ubiquitous Language

Based on the domain above, here's the language we should be using:

**Nouns (Things)**:
- Book, Author, Shelf, Bookcase, Collection, ISBN, Publisher, Edition

**Verbs (Actions/Behaviors)**:
- Check out a book, Return a book, Place a book on a shelf, Search the collection, Add a book to the collection, Remove a book from the collection

**States**:
- Available, Checked Out, Reading, Missing

**Relationships**:
- A Book is written by one or more Authors
- A Book is placed on a Shelf
- A Shelf is contained in a Bookcase
- A Bookcase is located in a specific place
- A Collection contains all Books

**Rules (Invariants)**:
- A book can only be checked out if it's currently available
- A book can only be on one shelf at a time
- An ISBN uniquely identifies a book edition
- A shelf has limited capacity

---

## Refactoring Toward Ubiquitous Language

Let me show you concrete examples of how to refactor your code to use ubiquitous language.

### Refactoring 1: From "BookEntity" to "Book"

**Current structure**:
```java
// Book.java - domain model (but anemic)
public class Book {
    private Long id;
    private String title;
    // ... just getters/setters
}

// BookEntity.java - persistence model
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    private Long bookId;
    private String title;
    // ... JPA annotations everywhere
}
```

**Problem**: Two classes for one domain concept. Pollution of domain language.

**Refactored structure** (we'll implement this in future sections):
```java
// domain/model/Book.java - The ONLY Book class
public class Book {  // This is your domain model
    private BookId id;
    private Title title;
    private ISBN isbn;
    private List<Author> authors;
    private Shelf currentLocation;
    private AvailabilityStatus status;

    // Behavior, not just data!
    public void checkOut() {
        if (!this.status.isAvailable()) {
            throw new BookNotAvailableException("Cannot check out: " + this.status);
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;
    }

    public void returnToLibrary() {
        this.status = AvailabilityStatus.AVAILABLE;
    }

    public void placeOnShelf(Shelf shelf) {
        this.currentLocation = shelf;
    }
}

// infrastructure/persistence/JpaBook.java - Persistence concern, hidden
@Entity
@Table(name = "books")
class JpaBook {  // Package-private! Not exposed to domain
    @Id private Long id;
    private String title;
    // ... This is an implementation detail
}
```

**What changed**:
- One `Book` class in domain language
- Persistence details hidden in infrastructure layer
- Behavior added to the domain model
- Domain-centric naming throughout

### Refactoring 2: From "String isbn" to "ISBN Value Object"

**Current code** (`BookEntity.java:18`):
```java
private String isbn;

public String getIsbn() { return isbn; }
public void setIsbn(String isbn) { this.isbn = isbn; }
```

**Refactored** (proper value object):
```java
// domain/model/ISBN.java
public class ISBN {
    private final String value;

    public ISBN(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid ISBN format: " + value);
        }
        this.value = normalize(value);  // Remove hyphens, etc.
    }

    private boolean isValid(String value) {
        // ISBN-10 or ISBN-13 validation logic
        if (value == null || value.isBlank()) return false;
        String normalized = value.replaceAll("-", "");
        return normalized.matches("\\d{10}|\\d{13}") && hasValidCheckDigit(normalized);
    }

    private boolean hasValidCheckDigit(String isbn) {
        // ISBN check digit algorithm
        // ...implementation...
        return true;  // simplified
    }

    private String normalize(String value) {
        return value.replaceAll("-", "").trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISBN)) return false;
        ISBN isbn = (ISBN) o;
        return value.equals(isbn.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        // Format as ISBN-13: 978-0-13-468599-1
        return formatAsISBN13(value);
    }
}

// In Book.java
public class Book {
    private ISBN isbn;  // Not String!

    public Book(ISBN isbn, Title title, ...) {
        this.isbn = isbn;  // Validation already happened in ISBN constructor
        // ...
    }
}
```

**Why this is better**:
1. **Type Safety**: Can't accidentally pass any string as an ISBN
2. **Encapsulation**: Validation logic lives with the concept
3. **Immutability**: ISBN can't change after creation (value objects are immutable)
4. **Expressiveness**: Code reads "Book has an ISBN" not "Book has a string"
5. **Single Responsibility**: ISBN knows how to validate itself

**Using it**:
```java
// This compiles and runs:
ISBN isbn = new ISBN("978-0134685991");
Book book = new Book(isbn, ...);

// This throws IllegalArgumentException at construction time:
ISBN invalid = new ISBN("banana");  // Fails fast!
```

### Refactoring 3: From "bookStatus" String to Rich Enum

**Current code** (`BookEntity.java:26` and `BookService.java:57`):
```java
private String bookStatus;

// Later...
if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
    bookEntity.setBookStatus("CHECKED_OUT");
}
```

**Refactored**:
```java
// domain/model/AvailabilityStatus.java
public enum AvailabilityStatus {
    AVAILABLE("Available for checkout"),
    CHECKED_OUT("Currently checked out"),
    READING("Currently being read"),
    MISSING("Cannot be located");

    private final String description;

    AvailabilityStatus(String description) {
        this.description = description;
    }

    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    public boolean canCheckOut() {
        return this == AVAILABLE;
    }

    public String getDescription() {
        return description;
    }
}

// In Book.java
public class Book {
    private AvailabilityStatus status;

    public void checkOut() {
        if (!status.canCheckOut()) {
            throw new BookNotAvailableException(
                "Cannot check out book: currently " + status.getDescription()
            );
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
    }

    // No more string comparisons!
    public boolean isAvailable() {
        return status.isAvailable();
    }
}
```

**What improved**:
- No more `.toString()` and string comparisons
- Behavior added to the enum (`isAvailable()`, `canCheckOut()`)
- Type safety: can't set status to invalid value
- Expressive: `status.isAvailable()` reads like English

### Refactoring 4: From Service Methods to Entity Behavior

**Current code** (`BookService.java:56-62`):
```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**The problem**: The Book has no behavior. It's operated on by the Service.

**Refactored**:
```java
// domain/model/Book.java
public class Book {
    private AvailabilityStatus status;
    private int checkoutCount;

    public void checkOut() {  // Behavior lives in the entity!
        if (!status.canCheckOut()) {
            throw new BookNotAvailableException(
                "Cannot check out: book is " + status.getDescription()
            );
        }
        this.status = AvailabilityStatus.CHECKED_OUT;
        this.checkoutCount++;
    }

    public void returnToLibrary() {
        if (status == AvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException("Book is already available");
        }
        this.status = AvailabilityStatus.AVAILABLE;
    }
}

// application/service/BookService.java
@Service
public class BookService {
    private final BookRepository bookRepository;

    public void checkOutBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BookNotFoundException(bookId));

        book.checkOut();  // Domain logic in the entity!

        bookRepository.save(book);  // Just persistence
    }
}
```

**What changed**:
- Domain logic (`checkOut()` rules) lives in `Book` entity
- Service is thin - it just orchestrates: load, call domain method, save
- Business rules are encapsulated in the domain model
- The code reads like domain language: `book.checkOut()`

---

## Identifying More Language Problems in Your Code

Let's do a code review together. I'm going to point out additional language issues for you to think about.

### Issue 1: "AuthorEntity" - Another Technical Term

`com.penrose.bibby.library.author.AuthorEntity`

**Question**: Would you ever say "Let me look up the author entity"? No - you'd say "Let me look up the author."

**Action**: Rename to just `Author`. Hide persistence concerns.

### Issue 2: "BookRequestDTO" - Breaking Abstraction

`com.penrose.bibby.library.book.BookRequestDTO`

**What it is**: Data Transfer Object for API requests.

**Why it's problematic**: It's not a domain concept - it's a technical artifact of your API layer. It shouldn't live in the `library.book` package (which should be your domain layer).

**Better location**: `interfaces.api.dto` or `application.dto`

### Issue 3: "Shelf.addToShelf(Book book)" - Anemic Implementation

In `Shelf.java:32-34`:

```java
public void addToShelf(Book book){
    // Empty! No implementation!
}
```

**The problem**: Method signature exists but does nothing. This is a broken promise.

**What should happen**: The shelf should actually maintain a collection of books and enforce capacity limits.

**Refactored**:
```java
public class Shelf {
    private final ShelfId id;
    private final List<Book> books;
    private final int capacity;

    public void placeBook(Book book) {  // Better verb: "place" not "add"
        if (books.size() >= capacity) {
            throw new ShelfFullException("Shelf is at capacity: " + capacity);
        }
        if (books.contains(book)) {
            throw new IllegalStateException("Book already on this shelf");
        }
        books.add(book);
        book.placeOnShelf(this);  // Bidirectional relationship
    }

    public void removeBook(Book book) {
        if (!books.remove(book)) {
            throw new BookNotFoundOnShelfException();
        }
    }

    public int getAvailableSpace() {
        return capacity - books.size();
    }

    public boolean isFull() {
        return books.size() >= capacity;
    }
}
```

**Notice**: Rich behavior, domain language, enforced invariants.

### Issue 4: Inconsistent Naming - "shelfLabel" vs "bookCaseLabel"

- `Shelf.java:9`: `private String shelfLabel;`
- `Bookcase.java:5`: `private String bookcaseLabel;`

**The inconsistency**: One is camelCase combined (`bookcaseLabel`), one is separated (`shelfLabel`).

**In ubiquitous language**: What would you call these? Probably just "name" or "label." Be consistent.

**Better**:
```java
public class Shelf {
    private Label label;  // Value object for label
}

public class Bookcase {
    private Label label;  // Same concept, same type
}
```

### Issue 5: Missing Domain Concepts

Looking at your README and code, I see concepts mentioned but not modeled:

**1. "Collection" or "Personal Library"**
- Mentioned in README but not modeled in code
- This could be an aggregate root that contains all books
- Would handle collection-level operations (statistics, searching, cataloging)

**2. "Location" (for Bookcases)**
- README mentions "basement"
- This is a domain concept worth modeling

**3. "Reading History"**
- You track `checkoutCount` but not when/how long books were checked out
- Could be valuable domain information

**4. "Genre"**
- You have `Genre.java` but I don't see it actively used
- This should be part of book classification

---

## Creating Your Domain Glossary

One of the most powerful exercises in DDD is creating a glossary of domain terms. This becomes your ubiquitous language reference.

### Bibby Domain Glossary (Draft)

Here's a starting glossary based on our analysis:

| Term | Definition | Code Representation | Notes |
|------|------------|---------------------|-------|
| **Book** | A physical book in the personal library | `Book` entity | Has identity, mutable |
| **Author** | Person who wrote a book | `Author` entity | Books can have multiple authors |
| **ISBN** | International Standard Book Number | `ISBN` value object | Unique identifier for book editions |
| **Title** | Name of a book | `Title` value object | Could include subtitle |
| **Shelf** | Physical shelf holding books | `Shelf` entity | Has location, capacity |
| **Bookcase** | Furniture containing shelves | `Bookcase` entity | Contains multiple shelves |
| **Collection** | Complete personal library | `Collection` aggregate? | TBD - may be repository concept |
| **Check Out** | Taking a book to read | `book.checkOut()` method | Changes availability status |
| **Return** | Bringing book back | `book.returnToLibrary()` | Restores availability |
| **Availability Status** | Whether book can be checked out | `AvailabilityStatus` enum | Available, Checked Out, Reading, Missing |
| **Genre** | Category of book | `Genre` enum or entity | Fiction, Non-fiction, Technical, etc. |
| **Publisher** | Company that published the book | `Publisher` value object | Could be simple String for now |
| **Edition** | Version/printing of book | `Edition` value object | First edition, Revised edition, etc. |
| **Location** | Where bookcase is physically | `Location` value object | "Basement", "Office", etc. |

**Exercise for you**:
1. Review this glossary
2. Add any terms I missed
3. Correct any definitions that don't match how you think about your library
4. This will become your reference document

---

## Why This Matters: Real-World Impact

You might be thinking "This is a lot of work just to rename things." Let me show you the real impact.

### Scenario: Adding a New Feature

**Feature Request**: "I want to loan books to friends and track who has which book."

**In your current codebase** (without ubiquitous language):
```java
// How would you implement this?
// - New "LoanEntity" table?
// - Add "loanedTo" string field to BookEntity?
// - New "BookLoanService"?
// - Is this different from "check out"?

public class BookService {
    public void loanBookToFriend(Long bookId, String friendName) {
        BookEntity book = bookRepository.findById(bookId);
        book.setBookStatus("LOANED");  // New status?
        book.setLoanedTo(friendName);   // New field?
        // What about existing checkouts?
        // How is this different?
    }
}
```

**With ubiquitous language and DDD**:
```java
// The domain language makes the distinction clear:
// - "Check out" = you're reading it
// - "Loan" = someone else is reading it

public class Book {
    private AvailabilityStatus status;
    private Optional<Loan> currentLoan;

    public void loanTo(Friend friend, LoanPeriod period) {
        if (!status.isAvailable()) {
            throw new BookNotAvailableException();
        }
        this.currentLoan = Optional.of(new Loan(friend, period));
        this.status = AvailabilityStatus.LOANED_OUT;
    }

    public void returnFromLoan() {
        if (currentLoan.isEmpty()) {
            throw new IllegalStateException("Book is not loaned out");
        }
        this.currentLoan = Optional.empty();
        this.status = AvailabilityStatus.AVAILABLE;
    }
}

// New domain concepts emerge naturally:
public class Loan {
    private final Friend borrower;
    private final LocalDate loanDate;
    private final LocalDate dueDate;

    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate);
    }
}

public class Friend {  // New domain concept!
    private final String name;
    private final ContactInfo contact;
}
```

**What happened**: By thinking in domain terms, we naturally discovered:
- `Loan` is a domain concept (not just a string field)
- `Friend` is a domain concept (not just a name)
- `LoanPeriod` and due dates matter
- The behavior is crystal clear

This is the power of ubiquitous language - it guides your design.

---

## Action Items: Your Next Steps

Here's what you should do before we meet for Section 2:

### 1. Create Your Domain Glossary (30-60 minutes)

Create a file `domain-glossary.md` in your project:
- List all domain terms from Bibby
- Define each in natural language (how you'd explain to a friend)
- Note which are entities, which are value objects (we'll refine this in Section 2)
- Identify any missing concepts

### 2. Identify Language Problems (1-2 hours)

Review your entire codebase and document:
- All uses of "Entity" suffix in domain classes
- All primitive fields that should be value objects (String, int, Long representing domain concepts)
- All anemic entities (classes with only getters/setters)
- All service methods that should be entity methods
- Package structure issues (domain vs. infrastructure confusion)

Create `language-problems.md` with your findings.

### 3. Rename One Class (30 minutes)

Practice refactoring toward ubiquitous language:
- Pick one class with a technical name (like `AuthorEntity`)
- Rename it to the domain term (just `Author`)
- Update all references
- Commit with message: "Refactor: Use ubiquitous language for Author"

This will give you practice with IDE refactoring tools and build confidence.

### 4. Design One Value Object (1 hour)

Choose one primitive field that should be a value object:
- `ISBN` is a great candidate
- Sketch out the class design (validation, immutability, equality)
- You don't have to implement it yet, just design it on paper or in comments

### 5. Read and Reflect (1 hour)

Read these short articles:
- Martin Fowler's "Ubiquitous Language": https://martinfowler.com/bliki/UbiquitousLanguage.html
- Eric Evans on "Getting Started with DDD": Search for his talks/articles

Then write a short reflection:
- What surprised you?
- What questions do you have?
- How do you see this applying to Bibby?

---

## Key Takeaways

Before we wrap up Section 1, let's cement the core lessons:

### 1. DDD is About Understanding the Domain First
- Technology is secondary to domain understanding
- Your code should reflect how domain experts (you!) think and talk
- The goal is a shared language between business and code

### 2. Ubiquitous Language is the Foundation
- Every class, method, variable should use domain terminology
- Avoid technical terms in your domain model
- If you wouldn't say it when explaining your library, don't code it that way

### 3. Your Current Code Has Language Problems
- Technical terms (`BookEntity`, `AuthorEntity`)
- Primitive obsession (`String isbn`, `Long shelfId`)
- Anemic domain model (no behavior in entities)
- Service-centric design (logic in services, not domain)

### 4. Small Changes Have Big Impact
- Renaming `BookEntity` → `Book` improves clarity
- Creating `ISBN` value object adds type safety and validation
- Moving behavior from service to entity enables rich domain model

### 5. This is a Journey, Not a Destination
- We're not going to refactor everything overnight
- Each section will build on the previous
- You'll see incremental improvement with each change

---

## Further Study

To deepen your understanding before Section 2:

**Books (specific chapters)**:
- "Domain-Driven Design" by Eric Evans - Chapters 1-3 (Introduction, Ubiquitous Language, Model-Driven Design)
- "Implementing Domain-Driven Design" by Vaughn Vernon - Chapter 1 (Getting Started with DDD)

**Articles**:
- Martin Fowler's DDD series: https://martinfowler.com/tags/domain%20driven%20design.html
- Specifically read: "Ubiquitous Language", "Anemic Domain Model"

**Videos** (optional):
- Eric Evans' "What I've learned about DDD since the book" (YouTube)
- Any talk by Vaughn Vernon on tactical DDD patterns

**Practice**:
- Look at well-designed open-source Java projects that use DDD
- Pay attention to their naming and package structure
- Notice how they separate domain from infrastructure

---

## Mentor's Note: Your Growth Path

I want you to know: what you've built with Bibby shows real engineering skill. You understand Spring Boot, JPA, Spring Shell, database relationships - that's not trivial. You're further along than many junior engineers.

The gap we're addressing is the *design* aspect of software engineering - moving from "code that works" to "code that communicates." This is exactly the right time in your career to be learning DDD. You have enough technical skill to appreciate the patterns, but you haven't yet formed bad habits that are hard to break.

DDD will feel weird at first. You'll think "isn't this over-engineering?" Sometimes it might be. But for Bibby - a domain-rich application - it's exactly the right approach. And learning to think in DDD will make you a dramatically better engineer.

The fact that you're investing in this learning tells me you're serious about your craft. That's the #1 predictor of success in software engineering.

See you in Section 2, where we'll dive deep into Entities and Value Objects, and you'll start to see how all of this comes together.

---

**Section 1 Complete** | Next: Section 2 - Entities & Value Objects

---

*This mentorship guide was created specifically for your Bibby codebase. All examples are drawn from your actual code. As we progress through sections, we'll refactor this code together, building both the application and your understanding simultaneously.*
