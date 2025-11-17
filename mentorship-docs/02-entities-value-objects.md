# SECTION 2: ENTITIES & VALUE OBJECTS

## The Two Building Blocks of Domain Models

Welcome to Section 2. In Section 1, we established the importance of ubiquitous language. Now we're going to learn the two fundamental building blocks of any domain model: **Entities** and **Value Objects**.

Understanding the difference between these two patterns is crucial. Get this right, and your domain model will be expressive, type-safe, and maintainable. Get it wrong, and you'll end up with either god objects or primitive obsession everywhere.

Let's dive deep into both patterns, then analyze your Bibby codebase to see where each should be applied.

---

## What is an Entity?

An **Entity** is an object that has a unique identity that persists over time, regardless of changes to its attributes.

### The Identity Principle

Think about yourself as a person. If you change your hair color, are you still you? Yes. If you move to a new address, are you still you? Yes. If you change your name legally, are you still the same person? Yes.

That's because **you have an identity** that's separate from your attributes. Your identity (perhaps a social security number, or just the fact that you're a continuous thread of consciousness) doesn't change even when everything else about you does.

**That's an Entity.**

In Bibby, a **Book** is an Entity. Even if you:
- Update the description
- Mark it as checked out vs. available
- Move it to a different shelf
- Increment the checkout count

...it's still the *same Book*. It has an identity (bookId) that persists through all these changes.

### Entity Characteristics

An Entity has these properties:

1. **Unique Identity**: Every entity has an identifier (ID) that distinguishes it from all other entities
2. **Mutable**: Entities can change over time
3. **Continuity**: The same entity persists through its lifecycle
4. **Identity Equality**: Two entities are equal if they have the same ID, regardless of attribute values
5. **Lifecycle**: Entities are created, modified, and potentially deleted

### Entities in Your Domain

Let's identify entities in Bibby:

**Book** - Each physical book has a unique identity
- Why: Two copies of "Clean Code" are different books with different histories
- Identity: `bookId`
- Lifecycle: Added to collection → placed on shelf → checked out → returned → moved → removed
- Mutable attributes: status, shelf location, checkout count, description

**Author** - Each author has a unique identity
- Why: Even if two authors have the same name, they're different people
- Identity: `authorId`
- Lifecycle: Created when first book added → associated with multiple books
- Mutable attributes: First name, last name (if they change their name)

**Shelf** - Each physical shelf has a unique identity
- Why: The top shelf in your basement bookcase is a specific, identifiable shelf
- Identity: `shelfId`
- Lifecycle: Created with bookcase → books placed on it → reorganized → removed
- Mutable attributes: Label, position, books on it

**Bookcase** - Each bookcase has a unique identity
- Why: The bookcase in your basement is a specific piece of furniture
- Identity: `bookcaseId`
- Lifecycle: Added to library → shelves organized → moved → removed
- Mutable attributes: Label, location, shelves

### Entity Example: Proper Implementation

Here's what a proper Entity looks like in code:

```java
// domain/model/Book.java
public class Book {
    // Identity - immutable after creation
    private final BookId id;

    // Attributes - mutable
    private Title title;
    private Set<Author> authors;
    private ISBN isbn;
    private Shelf currentLocation;
    private AvailabilityStatus status;
    private int checkoutCount;

    // Constructor ensures valid creation
    public Book(BookId id, Title title, ISBN isbn, Set<Author> authors) {
        this.id = Objects.requireNonNull(id, "Book ID cannot be null");
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.isbn = Objects.requireNonNull(isbn, "ISBN cannot be null");
        this.authors = new HashSet<>(Objects.requireNonNull(authors, "Authors cannot be null"));
        this.status = AvailabilityStatus.AVAILABLE;
        this.checkoutCount = 0;

        if (authors.isEmpty()) {
            throw new IllegalArgumentException("Book must have at least one author");
        }
    }

    // Behavior - entities DO things
    public void checkOut() {
        if (!status.isAvailable()) {
            throw new BookNotAvailableException(
                "Cannot check out book in status: " + status
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

    public void placeOnShelf(Shelf shelf) {
        Objects.requireNonNull(shelf, "Shelf cannot be null");
        this.currentLocation = shelf;
    }

    // Identity-based equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book other = (Book) o;
        return id.equals(other.id);  // Only compare IDs!
    }

    @Override
    public int hashCode() {
        return id.hashCode();  // Only hash the ID!
    }

    // Getters for reading state
    public BookId getId() { return id; }
    public Title getTitle() { return title; }
    public AvailabilityStatus getStatus() { return status; }
    public int getCheckoutCount() { return checkoutCount; }
}
```

**Key points**:
- ID is `final` (immutable)
- Behavior methods (`checkOut()`, `returnToLibrary()`) encapsulate business logic
- Validation in constructor
- Equality based on ID only
- Rich domain model, not anemic

---

## What is a Value Object?

A **Value Object** is an object that has no unique identity. It's defined entirely by its attributes. Two value objects with the same attributes are considered identical and interchangeable.

### The Interchangeability Principle

Think about a $20 bill. If you have a $20 bill and I have a $20 bill, do they have different identities? No. They're functionally identical. We can swap them and nothing changes.

If you tear your $20 bill in half, is it still the same $20 bill? No - you now have a different value (two pieces of worthless paper).

**That's a Value Object.** Defined by its value, not by identity.

In Bibby, an **ISBN** is a Value Object. The ISBN "978-0134685991" is the same whether it's in your Book object or my Book object. There's no "identity" to an ISBN - just the value itself.

### Value Object Characteristics

A Value Object has these properties:

1. **No Identity**: No ID field. The entire value defines it.
2. **Immutable**: Once created, cannot be changed
3. **Structural Equality**: Two value objects are equal if all their attributes are equal
4. **Replaceable**: If you want to "change" a value object, you replace it with a new one
5. **Side-Effect Free**: Methods on value objects don't modify state, they return new values
6. **Self-Validating**: Construction ensures validity

### Value Objects in Your Domain

Let's identify value objects in Bibby:

**ISBN** - International Standard Book Number
- Why: ISBNs don't have identity, just a standardized format
- Attributes: The ISBN string (with validation)
- Immutable: Yes - an ISBN never changes
- Example: `ISBN("978-0134685991")`

**Title** - Book title
- Why: Titles don't have identity separate from their text
- Attributes: The title text, possibly subtitle
- Immutable: Yes - to "change" a title you replace the whole value
- Example: `Title("Domain-Driven Design")`

**Author Name** - Person's name
- Why: A name is a value describing a person, not an identity itself
- Attributes: First name, last name
- Immutable: Yes - if someone changes their name, you create a new Name value
- Example: `AuthorName("Eric", "Evans")`
- Note: Author is an Entity, but their Name is a Value Object!

**Shelf Label** - The label on a shelf
- Why: Labels are just text values
- Attributes: The label text
- Immutable: Yes
- Example: `ShelfLabel("Fiction A-F")`

**Publication Year** - Year a book was published
- Why: A year is a value, not an entity
- Attributes: The year
- Immutable: Yes
- Example: `PublicationYear(2003)`

**Publisher** - Publishing company name
- Why: For Bibby's purposes, just the name as a value
- Attributes: Publisher name
- Immutable: Yes
- Example: `Publisher("Addison-Wesley")`

**Location** - Physical location of a bookcase
- Why: Locations like "Basement" or "Office" are descriptive values
- Attributes: Location description
- Immutable: Yes
- Example: `Location("Basement")`

### Value Object Example: ISBN Implementation

Here's a complete, production-quality value object:

```java
// domain/model/ISBN.java
public final class ISBN {  // final = cannot be subclassed
    private final String value;

    // Factory methods provide semantic construction
    public static ISBN fromString(String isbnString) {
        return new ISBN(isbnString);
    }

    // Constructor is private or package-private
    private ISBN(String value) {
        String normalized = normalize(value);
        validate(normalized);
        this.value = normalized;
    }

    private String normalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("ISBN cannot be null");
        }
        // Remove hyphens, spaces
        return input.replaceAll("[\\s-]", "").toUpperCase();
    }

    private void validate(String normalized) {
        if (!normalized.matches("\\d{10}|\\d{13}")) {
            throw new IllegalArgumentException(
                "ISBN must be 10 or 13 digits, got: " + normalized
            );
        }

        if (normalized.length() == 10) {
            validateISBN10(normalized);
        } else {
            validateISBN13(normalized);
        }
    }

    private void validateISBN10(String isbn) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (isbn.charAt(i) - '0') * (10 - i);
        }
        char checkChar = isbn.charAt(9);
        int checkDigit = (checkChar == 'X') ? 10 : (checkChar - '0');

        if ((sum + checkDigit) % 11 != 0) {
            throw new IllegalArgumentException("Invalid ISBN-10 check digit");
        }
    }

    private void validateISBN13(String isbn) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = isbn.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = isbn.charAt(12) - '0';
        int calculated = (10 - (sum % 10)) % 10;

        if (checkDigit != calculated) {
            throw new IllegalArgumentException("Invalid ISBN-13 check digit");
        }
    }

    // Value object equality: compare all attributes
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISBN)) return false;
        ISBN other = (ISBN) o;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return formatForDisplay();
    }

    private String formatForDisplay() {
        if (value.length() == 13) {
            // Format as: 978-0-13-468599-1
            return String.format("%s-%s-%s-%s-%s",
                value.substring(0, 3),
                value.substring(3, 4),
                value.substring(4, 6),
                value.substring(6, 12),
                value.substring(12, 13)
            );
        } else {
            // ISBN-10 format: 0-13-468599-1
            return String.format("%s-%s-%s-%s",
                value.substring(0, 1),
                value.substring(1, 3),
                value.substring(3, 9),
                value.substring(9, 10)
            );
        }
    }

    // Getter returns the value
    public String getValue() {
        return value;
    }

    // No setters! Immutable!
}
```

**Key characteristics demonstrated**:
- **Immutable**: No setters, `final` field
- **Self-validating**: Constructor validates the ISBN format
- **Value equality**: `equals()` compares the value, not identity
- **Encapsulation**: Validation logic lives with the concept
- **Type safety**: Can't pass any string where ISBN is expected

### Value Object Example: Title Implementation

Here's a simpler value object:

```java
// domain/model/Title.java
public final class Title {
    private final String value;

    public Title(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
        if (value.length() > 500) {
            throw new IllegalArgumentException("Title too long (max 500 characters)");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    // Value object behavior: transform, don't mutate
    public Title toUpperCase() {
        return new Title(value.toUpperCase());  // Returns NEW Title
    }

    public Title truncate(int maxLength) {
        if (value.length() <= maxLength) {
            return this;  // No change needed, return same object
        }
        return new Title(value.substring(0, maxLength) + "...");  // New object
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Title)) return false;
        Title other = (Title) o;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
```

---

## Entity vs. Value Object: The Decision Framework

How do you decide if something should be an Entity or a Value Object?

### The Questions to Ask

**1. Does it have a unique identity that matters to the domain?**
- Yes → Entity
- No → Value Object

**2. If all attributes are the same, are two instances interchangeable?**
- Yes → Value Object
- No → Entity

**3. Does it have a lifecycle that needs to be tracked?**
- Yes → Entity
- No → Value Object

**4. Does it change over time in meaningful ways?**
- Yes, and identity persists → Entity
- No, or changes mean replacement → Value Object

### Examples from Bibby

| Concept | Entity or Value Object? | Why |
|---------|------------------------|-----|
| Book | Entity | Has unique identity (specific physical book), tracks lifecycle |
| ISBN | Value Object | "978-0134685991" is the same everywhere, no identity |
| Author (person) | Entity | Unique person with identity, lifecycle |
| Author Name | Value Object | "Eric Evans" is just a value describing the author |
| Shelf | Entity | Specific physical shelf with identity and lifecycle |
| Shelf Label | Value Object | "Fiction A-F" is just text, no identity |
| Bookcase | Entity | Specific furniture with identity |
| Publication Year | Value Object | "2003" is just a number, interchangeable |
| Genre | Value Object | "Fiction" is a category, not an entity |
| Checkout Count | Value Object | Just an integer, no identity |

### The Gray Areas

Some concepts can be modeled either way depending on your domain needs:

**Address**:
- Value Object: If you only care about the location as data
- Entity: If you need to track address history, validate against postal service, etc.

**Phone Number**:
- Value Object: Usually just a formatted string
- Entity: If you track which number is primary, when it was verified, etc.

**Money**:
- Value Object: In most domains (amount + currency)
- Entity: If you're building a banking system tracking specific bills

**For Bibby**, stick with simpler value object implementations unless you discover a clear need for identity.

---

## Analyzing Your Current Code

Let's review your Bibby codebase and identify what needs to change.

### Current State: Book.java

```java
// Your current Book.java
public class Book {
    private Long id;
    private int edition;
    private String title;  // ❌ Should be Title value object
    private AuthorEntity authorEntity;  // ❌ Naming issue
    private String isbn;  // ❌ Should be ISBN value object
    private String publisher;  // ❌ Should be Publisher value object
    private int publicationYear;  // ❌ Should be PublicationYear value object
    private Genre genre;  // ✓ Good if Genre is enum/value object
    private Shelf shelf;
    private String description;
    private BookStatus status;
    private Integer checkoutCount;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // ❌ Only getters/setters, no behavior
}
```

**Problems identified**:
1. **Primitive Obsession**: `String title`, `String isbn`, `String publisher`, `int publicationYear`
2. **Anemic Model**: No behavior, just getters/setters
3. **Mutable ID**: `setId()` exists - IDs should be immutable
4. **No validation**: Can set invalid values
5. **Equality not implemented properly**: Uses ID but is mutable

### Current State: Author.java

```java
// Your current Author.java
public class Author {
    private Long authorId;
    private String firstName;  // ❌ Should be part of AuthorName value object
    private String lastName;   // ❌ Should be part of AuthorName value object

    // ❌ Only getters/setters, no behavior
}
```

**Problems**:
1. First/last name should be a value object
2. Anemic model
3. Mutable ID

### What's Right: BookStatus.java

```java
// Your BookStatus enum - Good!
public enum BookStatus {
    AVAILABLE,
    CHECKED_OUT,
    RESERVED,
    LOST,
    ARCHIVED
}
```

**This is good!** An enum is essentially a value object. But it can be improved with behavior:

```java
public enum BookStatus {
    AVAILABLE(true, "Available for checkout"),
    CHECKED_OUT(false, "Currently checked out"),
    RESERVED(false, "Reserved for pickup"),
    LOST(false, "Cannot be located"),
    ARCHIVED(false, "Archived, not in circulation");

    private final boolean availableForCheckout;
    private final String description;

    BookStatus(boolean availableForCheckout, String description) {
        this.availableForCheckout = availableForCheckout;
        this.description = description;
    }

    public boolean isAvailableForCheckout() {
        return availableForCheckout;
    }

    public String getDescription() {
        return description;
    }
}
```

---

## Refactoring Examples: From Primitives to Value Objects

Let me show you concrete refactoring examples from your code.

### Refactoring 1: String title → Title Value Object

**Before** (your current code):
```java
public class Book {
    private String title;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}

// Usage - no validation
Book book = new Book();
book.setTitle("");  // ❌ Empty title allowed
book.setTitle(null);  // ❌ Null allowed
book.setTitle(veryLongString);  // ❌ No length limit
```

**After** (with Title value object):
```java
// domain/model/Title.java
public final class Title {
    private final String value;

    public Title(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (value.length() > 500) {
            throw new IllegalArgumentException("Title too long");
        }
        this.value = value.trim();
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Title)) return false;
        return value.equals(((Title) o).value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }

    @Override
    public String toString() { return value; }
}

// domain/model/Book.java
public class Book {
    private Title title;  // Not String!

    public Book(BookId id, Title title, ...) {
        this.title = Objects.requireNonNull(title);
    }

    public Title getTitle() { return title; }

    // No setter! To change title, use a behavior method
    public void updateTitle(Title newTitle) {
        this.title = Objects.requireNonNull(newTitle);
    }
}

// Usage - validation automatic
Title title = new Title("Domain-Driven Design");  // ✓ Valid
Book book = new Book(id, title, ...);

Title empty = new Title("");  // ❌ Throws exception
Title nullTitle = new Title(null);  // ❌ Throws exception
```

**Benefits**:
- Type safety: can't pass any string
- Automatic validation
- Expressive code: `Title` vs `String`
- Single place for title rules

### Refactoring 2: String isbn → ISBN Value Object

**Before**:
```java
public class Book {
    private String isbn;

    public void setIsbn(String isbn) {
        this.isbn = isbn;  // No validation
    }
}

// Usage
book.setIsbn("banana");  // ❌ Compiles and runs!
book.setIsbn("123");  // ❌ Invalid ISBN accepted
```

**After**:
```java
// domain/model/ISBN.java (shown earlier in full)
public final class ISBN {
    private final String value;

    private ISBN(String value) {
        validate(value);  // Full ISBN validation
        this.value = normalize(value);
    }

    public static ISBN fromString(String input) {
        return new ISBN(input);
    }
    // ... validation methods ...
}

// domain/model/Book.java
public class Book {
    private ISBN isbn;

    public Book(BookId id, Title title, ISBN isbn, ...) {
        this.isbn = Objects.requireNonNull(isbn);
    }

    public ISBN getIsbn() { return isbn; }
}

// Usage
ISBN isbn = ISBN.fromString("978-0-13-468599-1");  // ✓ Valid
Book book = new Book(id, title, isbn, ...);

ISBN invalid = ISBN.fromString("banana");  // ❌ Throws exception immediately
```

### Refactoring 3: firstName/lastName → AuthorName Value Object

**Before**:
```java
public class Author {
    private String firstName;
    private String lastName;

    public String getFullName() {
        return firstName + " " + lastName;  // Null pointer risk
    }
}
```

**After**:
```java
// domain/model/AuthorName.java
public final class AuthorName {
    private final String firstName;
    private final String lastName;

    public AuthorName(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name required");
        }
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getLastNameFirst() {
        return lastName + ", " + firstName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthorName)) return false;
        AuthorName that = (AuthorName) o;
        return firstName.equals(that.firstName) &&
               lastName.equals(that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName);
    }

    @Override
    public String toString() {
        return getFullName();
    }
}

// domain/model/Author.java (Entity)
public class Author {
    private final AuthorId id;
    private AuthorName name;  // Value object, can be replaced

    public Author(AuthorId id, AuthorName name) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
    }

    public void changeName(AuthorName newName) {
        this.name = Objects.requireNonNull(newName);
    }

    public AuthorName getName() { return name; }
    public AuthorId getId() { return id; }
}
```

---

## Java Records for Value Objects

**Java 16+** introduced records, which are perfect for simple value objects:

```java
// Using a record for simple value objects
public record Title(String value) {
    public Title {  // Compact constructor
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (value.length() > 500) {
            throw new IllegalArgumentException("Title too long");
        }
        value = value.trim();  // Normalize
    }
}

public record Publisher(String name) {
    public Publisher {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Publisher name required");
        }
        name = name.trim();
    }
}

public record PublicationYear(int year) {
    public PublicationYear {
        if (year < 1450 || year > LocalDate.now().getYear() + 5) {
            throw new IllegalArgumentException("Invalid publication year: " + year);
        }
    }
}

public record AuthorName(String firstName, String lastName) {
    public AuthorName {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name required");
        }
        firstName = firstName.trim();
        lastName = lastName.trim();
    }

    public String fullName() {
        return firstName + " " + lastName;
    }
}
```

**Benefits of records**:
- Immutable by default
- `equals()` and `hashCode()` automatically generated
- Compact syntax
- Perfect for simple value objects

**When to use classes instead**:
- Complex validation logic (like ISBN)
- Many methods/behaviors
- Need to hide implementation details
- Want more control over API

---

## Common Mistakes to Avoid

### Mistake 1: Making Value Objects Mutable

```java
// ❌ Bad - mutable value object
public class ISBN {
    private String value;

    public void setValue(String value) {  // NO! Don't do this!
        this.value = value;
    }
}
```

**Why it's bad**: Value objects should be immutable. If you want a different value, create a new object.

### Mistake 2: Using Identity for Value Objects

```java
// ❌ Bad - value object with ID
public class Title {
    private Long id;  // NO! Value objects don't have identity!
    private String value;
}
```

### Mistake 3: Entity with Value Equality

```java
// ❌ Bad - entity comparing all fields
public class Book {
    private Long id;
    private String title;

    @Override
    public boolean equals(Object o) {
        Book other = (Book) o;
        return id.equals(other.id) &&
               title.equals(other.title);  // NO! Only compare ID!
    }
}
```

### Mistake 4: Primitive Obsession

```java
// ❌ Bad - primitives everywhere
public void addBook(String title, String isbn, String author) {
    // No type safety, no validation
}

// ✓ Good - value objects
public void addBook(Title title, ISBN isbn, AuthorName author) {
    // Type safety, automatic validation
}
```

---

## Action Items

Here's what you should do before Section 3:

### 1. Create Three Value Objects (3-4 hours)

Implement these value objects for Bibby:

**ISBN** (most complex):
- Full ISBN-10 and ISBN-13 validation
- Normalization (remove hyphens)
- Formatting for display
- Use a class, not a record (complexity warrants it)

**Title** (medium complexity):
- Validation (not blank, max length)
- Use Java record

**AuthorName** (simple):
- First and last name
- `fullName()` method
- Use Java record

Create these in a new package: `com.penrose.bibby.domain.model.valueobject`

### 2. Identify All Value Objects in Bibby (1 hour)

Create a document listing every value object candidate:
- Publisher
- PublicationYear
- Genre (if not enum)
- ShelfLabel
- Location
- Description
- Edition

For each, note:
- What it represents
- Validation rules needed
- Whether to use record or class

### 3. Refactor Book Entity (2-3 hours)

Take your current `Book.java` and:
- Make `id` field `final`
- Remove `setId()` method
- Add at least one behavior method (like `checkOut()`)
- Implement proper equality (ID-based only)

Don't worry about using value objects yet - just make it a proper entity.

### 4. Study Equality Implementations (30 minutes)

Review your `Book.java` and `Author.java`:
- Do they implement `equals()` and `hashCode()`?
- Are they correct (ID-only for entities)?
- Fix any issues

### 5. Read and Reflect (1 hour)

Read:
- Martin Fowler on "Value Object": https://martinfowler.com/bliki/ValueObject.html
- Vaughn Vernon's "Implementing Domain-Driven Design" - Chapter 5 (Entities), Chapter 6 (Value Objects)

Write a reflection:
- What's the most important difference between entities and value objects?
- Which pattern did you not understand before?
- How will you apply this to Bibby?

---

## Key Takeaways

### 1. Entities Have Identity, Value Objects Don't
- Entity: "This specific book" (identity matters)
- Value Object: "The ISBN 978-0134685991" (just a value)

### 2. Immutability is Key for Value Objects
- Once created, never modified
- Want a different value? Create a new object
- Use `final` fields and no setters

### 3. Equality Works Differently
- Entity equality: compare IDs only
- Value object equality: compare all attributes
- Get this wrong and you'll have subtle bugs

### 4. Primitive Obsession is a Code Smell
- Don't use `String` for ISBNs, Titles, etc.
- Create value objects for domain concepts
- Type safety + validation + expressiveness

### 5. Value Objects Eliminate Validation Scatter
- Validation in constructor
- Valid state guaranteed
- No need to validate everywhere else in code

---

## Further Study

**Books**:
- "Domain-Driven Design" by Eric Evans - Chapters 5 (Entities), 6 (Value Objects)
- "Implementing Domain-Driven Design" by Vaughn Vernon - Chapters 5-6

**Articles**:
- Martin Fowler: "Value Object", "Entity"
- Thoughtworks: "ValueObject" pattern

**Code Examples**:
- Study well-designed DDD projects on GitHub
- Look for value object implementations
- Notice the patterns

---

## Mentor's Note

This section covered a LOT of ground. Entities and value objects are the foundation of tactical DDD. Master these, and everything else becomes easier.

You might be thinking "this seems like a lot of classes for simple concepts." You're right - it is more code upfront. But here's what you gain:

1. **Type Safety**: Compiler catches errors
2. **Validation**: Happens once, automatically
3. **Expressiveness**: Code reads like domain language
4. **Maintainability**: Change rules in one place
5. **Testability**: Easy to test value objects in isolation

The investment pays off as your codebase grows. A few extra value objects now prevent hundreds of validation bugs later.

In Section 3, we'll learn about Aggregates - how to group entities and value objects into consistency boundaries. This is where DDD really starts to shine.

Great work getting through this section. Take your time with the action items - they're the key to truly understanding these patterns.

See you in Section 3!

---

**Section 2 Complete** | Next: Section 3 - Aggregates & Aggregate Roots
