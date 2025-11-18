# Section 26: Entity Design & JPA

**Learning objective:** Master JPA entity annotations, understand relationship mappings (`@ManyToOne`, `@OneToMany`, `@ManyToMany`), fix missing relationships in Bibby, learn ID generation strategies, and implement proper `equals()` and `hashCode()` for entities.

**Why this matters:** Your entities are the foundation of your application. Poor entity design leads to N+1 query problems, impossible queries, and runtime errors. In Section 25, we discovered that `BookEntity.shelfId` is a plain `Long` when it should be a `@ManyToOne` relationship. This section fixes that and more.

**Duration:** 75 min read

---

## The Critical Question

**Your current entity structure:**

```
Bookcase
   ↓ (bookcaseId as Long)
Shelf
   ↓ (shelfId as Long)
Book
   ↔ (authors as @ManyToMany)
Author
```

**Question:** Why can't you write this query?

```java
// Find all books in a specific bookcase:
List<BookEntity> findByShelf_Bookcase_BookcaseId(Long bookcaseId);
```

**Answer:** Because `BookEntity.shelfId` is a **plain `Long` field**, not a `@ManyToOne` relationship to `ShelfEntity`.

This section reveals how to fix this and build proper entity relationships.

---

## What You'll Learn From Your Code

You have **5 entities** in Bibby:
- **BookEntity** - 13 fields, @ManyToMany with AuthorEntity ✅, shelfId as Long ❌
- **AuthorEntity** - 4 fields, @ManyToMany(mappedBy) with BookEntity ✅
- **ShelfEntity** - 4 fields, bookcaseId as Long ❌
- **BookcaseEntity** - 3 fields, no relationships defined ❌
- **CatalogEntity** - Empty class, NO JPA annotations ❌

We'll analyze every annotation, fix missing relationships, and transform your entity design from **Grade: C** to **Grade: A**.

---

## Part 1: Entity Basics

### What is a JPA Entity?

A **JPA entity** is a Java class that represents a database table.

**Your simplest entity:**

```java
// BookcaseEntity.java:5-12
@Entity
@Table(name = "bookcases")
public class BookcaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookcaseId;
    private String bookcaseLabel;
    private int shelfCapacity;
```

**Mapping:**

| Java (Entity) | Database (Table) |
|---------------|------------------|
| `@Entity` class | Table |
| `@Table(name = "bookcases")` | Table name = `bookcases` |
| `@Id` field | Primary key column |
| `private Long bookcaseId` | Column `bookcase_id` (snake_case) |
| `private String bookcaseLabel` | Column `bookcase_label` |
| `private int shelfCapacity` | Column `shelf_capacity` |

**Naming convention:** Java uses `camelCase`, database uses `snake_case`. Hibernate auto-converts.

---

### Essential Annotations

#### 1. `@Entity` - Marks a Class as a JPA Entity

**Your code:**
```java
// BookEntity.java:10
@Entity
@Table(name = "books")
public class BookEntity {
```

**What it does:**
- Tells Hibernate "this class maps to a database table"
- Makes the class **managed** by the EntityManager
- Enables CRUD operations through repositories

**Without `@Entity`:**
```
IllegalArgumentException: Not a managed type: class BookEntity
```

**All your entities have this** (except CatalogEntity - BUG!).

---

#### 2. `@Table` - Specifies Table Name

**Your code:**
```java
// BookEntity.java:11
@Table(name = "books")
```

**What it does:**
- Maps entity to table named `"books"` (not `"book_entity"`)
- Optional if table name matches class name

**Examples from Bibby:**
- `@Table(name = "books")` - Table: `books`, Class: `BookEntity`
- `@Table(name = "authors")` - Table: `authors`, Class: `AuthorEntity`
- `@Table(name = "shelves")` - Table: `shelves`, Class: `ShelfEntity`
- `@Table(name = "bookcases")` - Table: `bookcases`, Class: `BookcaseEntity`

**Good practice:** Always specify `@Table(name = "...")` for clarity.

---

#### 3. `@Id` - Marks Primary Key

**Your code:**
```java
// BookEntity.java:14-16
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long bookId;
```

**What it does:**
- Marks `bookId` as the **primary key**
- Required for every entity (exactly one `@Id` per entity)

**Without `@Id`:**
```
MappingException: No identifier specified for entity: BookEntity
```

**All your entities have this correctly** (except CatalogEntity).

---

#### 4. `@GeneratedValue` - ID Generation Strategy

**Your code:**
```java
// BookEntity.java:15
@GeneratedValue(strategy = GenerationType.AUTO)
```

**What it does:**
- Tells Hibernate to **automatically generate** ID values
- You don't manually set `bookId` - Hibernate does it

**4 strategies available:**

| Strategy | How it works | Best for | Bibby uses? |
|----------|--------------|----------|-------------|
| `AUTO` | Hibernate chooses (SEQUENCE for PostgreSQL) | Most databases | ✅ Yes (all entities) |
| `IDENTITY` | Database auto-increment (MySQL `AUTO_INCREMENT`) | MySQL, SQL Server | No |
| `SEQUENCE` | Database sequence object (PostgreSQL `SERIAL`) | PostgreSQL, Oracle | No (AUTO picks this) |
| `TABLE` | Separate table for ID generation (slow) | Legacy systems | No |

**Why AUTO is good for PostgreSQL:**
- PostgreSQL has native SEQUENCE support
- AUTO detects PostgreSQL and uses SEQUENCE
- Efficient and safe

**What Hibernate generates:**
```sql
CREATE SEQUENCE books_seq START WITH 1 INCREMENT BY 50;

INSERT INTO books (title, ...) VALUES (?, ...);
-- bookId = nextval('books_seq')
```

**All your entities use `GenerationType.AUTO`** - Perfect choice for PostgreSQL.

---

### No-Arg Constructor Requirement

**Your code:**
```java
// BookEntity.java:30-31
public BookEntity() {
}
```

**Why required:**
- Hibernate uses **reflection** to create entity instances
- Reflection calls the no-arg constructor
- Constructor can be `protected` (JPA requires package-private or better)

**Without no-arg constructor:**
```
InstantiationException: No default constructor for entity: BookEntity
```

**All your entities have this correctly.**

---

## Part 2: Your Entity Inventory

Let's analyze all 5 entities in detail.

### Entity 1: BookEntity

**File:** BookEntity.java (136 lines)

**Annotations:**
```java
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    // 12 simple fields:
    private String title;
    private String isbn;
    private String publisher;
    private int publicationYear;
    private String genre;
    private int edition;
    private String description;
    private Long shelfId;              // ❌ Should be @ManyToOne ShelfEntity
    private Integer checkoutCount;
    private String bookStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // 1 relationship field:
    @ManyToMany
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<AuthorEntity> authors = new HashSet<>();
}
```

**Fields:** 13 total (12 simple + 1 relationship)

**Relationships:**
- ✅ `@ManyToMany` with `AuthorEntity` (CORRECT)
- ❌ `Long shelfId` should be `@ManyToOne ShelfEntity` (WRONG)

**Issues:**
1. **shelfId as Long instead of @ManyToOne** - Blocks nested queries
2. **No equals()/hashCode()** - HashSet/HashMap issues
3. **Exposed mutable Set** - `getAuthors()` returns internal Set

**Grade:** C+ (relationship issue is critical)

---

### Entity 2: AuthorEntity

**File:** AuthorEntity.java (78 lines)

**Annotations:**
```java
@Entity
@Table(name = "authors")
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long authorId;

    private String firstName;
    private String lastName;
    private String fullName;  // ❌ Derived field (should be getter method)

    @ManyToMany(mappedBy = "authors")
    private Set<BookEntity> books = new HashSet<>();
}
```

**Fields:** 4 total (3 simple + 1 relationship)

**Relationships:**
- ✅ `@ManyToMany(mappedBy = "authors")` with `BookEntity` (CORRECT)

**Issues:**
1. **fullName is stored** - Should be calculated getter (covered in Section 7)
2. **No equals()/hashCode()** - HashSet/HashMap issues
3. **mappedBy side has Set<BookEntity>** - Bidirectional consistency risk

**Grade:** B- (relationship correct, but derived field issue)

---

### Entity 3: ShelfEntity

**File:** ShelfEntity.java (44 lines)

**Annotations:**
```java
@Entity
@Table(name = "shelves")
public class ShelfEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long shelfId;

    private String shelfLabel;
    private Long bookcaseId;     // ❌ Should be @ManyToOne BookcaseEntity
    private int shelfPosition;
}
```

**Fields:** 4 total (all simple, no relationships)

**Relationships:**
- ❌ `Long bookcaseId` should be `@ManyToOne BookcaseEntity` (WRONG)
- ❌ Missing `@OneToMany List<BookEntity> books` (WRONG)

**Issues:**
1. **bookcaseId as Long instead of @ManyToOne** - Blocks nested queries
2. **No relationship to books** - Can't navigate Shelf → Books
3. **No equals()/hashCode()** - HashSet/HashMap issues

**Grade:** D (multiple missing relationships)

---

### Entity 4: BookcaseEntity

**File:** BookcaseEntity.java (54 lines)

**Annotations:**
```java
@Entity
@Table(name = "bookcases")
public class BookcaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookcaseId;

    private String bookcaseLabel;
    private int shelfCapacity;
}
```

**Fields:** 3 total (all simple, no relationships)

**Relationships:**
- ❌ Missing `@OneToMany List<ShelfEntity> shelves` (WRONG)

**Issues:**
1. **No relationship to shelves** - Can't navigate Bookcase → Shelves
2. **No equals()/hashCode()** - HashSet/HashMap issues

**Grade:** D (missing relationship)

---

### Entity 5: CatalogEntity

**File:** CatalogEntity.java (4 lines)

**Full file:**
```java
package com.penrose.bibby.library.catalog;

public class CatalogEntity {
}
```

**Critical issues:**
1. **No `@Entity` annotation** - Not a JPA entity!
2. **No `@Id` field** - No primary key!
3. **No fields at all** - Empty class!
4. **Has a repository (CatalogRepository)** - But repository is also empty!

**Grade:** F (not a JPA entity, unusable)

**Question:** What is CatalogEntity supposed to represent?
- Unused placeholder?
- Work in progress?
- Should be deleted?

**Recommendation:** Either complete it or delete it (and CatalogRepository).

---

## Part 3: Relationship Mappings

### The Entity Relationship Diagram (ERD)

**Your intended domain model:**

```
┌──────────────┐
│  Bookcase    │
│  - id        │
│  - label     │
└──────┬───────┘
       │ 1
       │
       │ N (one bookcase has many shelves)
       │
┌──────▼───────┐
│    Shelf     │
│  - id        │
│  - label     │
└──────┬───────┘
       │ 1
       │
       │ N (one shelf has many books)
       │
┌──────▼───────┐         ┌──────────────┐
│     Book     │ N ←──→ N│    Author    │
│  - id        │         │  - id        │
│  - title     │         │  - firstName │
└──────────────┘         │  - lastName  │
                         └──────────────┘
```

**Relationships:**
1. **Bookcase → Shelf:** One-to-Many (`@OneToMany` / `@ManyToOne`)
2. **Shelf → Book:** One-to-Many (`@OneToMany` / `@ManyToOne`)
3. **Book ↔ Author:** Many-to-Many (`@ManyToMany` - already correct!)

---

### Relationship Types

#### 1. `@ManyToOne` - The "Owning" Side

**Definition:** Many entities reference one entity.

**Examples:**
- Many **books** belong to one **shelf**
- Many **shelves** belong to one **bookcase**
- Many **employees** work for one **department**

**Your missing code (should be in BookEntity.java):**

```java
// CURRENT (line 24):
private Long shelfId;  // ❌ Plain foreign key

// SHOULD BE:
@ManyToOne
@JoinColumn(name = "shelf_id")
private ShelfEntity shelf;  // ✅ Object reference
```

**What `@ManyToOne` does:**
- Creates foreign key constraint in database
- Allows navigation: `book.getShelf().getShelfLabel()`
- Enables nested queries: `findByShelf_BookcaseId(Long id)`

**What `@JoinColumn` does:**
- Specifies foreign key column name (`shelf_id`)
- Optional (defaults to `shelf_shelfId`) but recommended for clarity

**Generated SQL:**
```sql
CREATE TABLE books (
    book_id BIGINT PRIMARY KEY,
    title VARCHAR(255),
    shelf_id BIGINT,  -- Foreign key column
    FOREIGN KEY (shelf_id) REFERENCES shelves(shelf_id)
);
```

---

#### 2. `@OneToMany` - The "Inverse" Side

**Definition:** One entity has a collection of many entities.

**Examples:**
- One **shelf** has many **books**
- One **bookcase** has many **shelves**
- One **department** has many **employees**

**Your missing code (should be in ShelfEntity.java):**

```java
// ShelfEntity currently has no relationship to books

// SHOULD ADD:
@OneToMany(mappedBy = "shelf")
private List<BookEntity> books = new ArrayList<>();
```

**What `@OneToMany` does:**
- Creates **bidirectional** relationship (Shelf ↔ Book)
- Allows navigation: `shelf.getBooks()` returns all books on this shelf
- Does NOT create foreign key (foreign key is on `@ManyToOne` side)

**What `mappedBy` does:**
- Tells Hibernate: "I don't own this relationship"
- Points to field name on owning side: `mappedBy = "shelf"` → `BookEntity.shelf`
- Prevents duplicate foreign keys

**Important:** `@OneToMany` is **optional**. You can have just `@ManyToOne` (unidirectional). But bidirectional is more convenient.

---

#### 3. `@ManyToMany` - Both Sides Equal

**Definition:** Many entities relate to many entities (requires join table).

**Examples:**
- Many **books** have many **authors**
- Many **students** enroll in many **courses**
- Many **users** belong to many **groups**

**Your correct code:**

```java
// BookEntity.java:38-44 (OWNING SIDE)
@ManyToMany
@JoinTable(
    name = "book_authors",              // Join table name
    joinColumns = @JoinColumn(name = "book_id"),        // FK to books table
    inverseJoinColumns = @JoinColumn(name = "author_id") // FK to authors table
)
private Set<AuthorEntity> authors = new HashSet<>();

// AuthorEntity.java:41-42 (INVERSE SIDE)
@ManyToMany(mappedBy = "authors")
private Set<BookEntity> books = new HashSet<>();
```

**Generated SQL:**
```sql
-- Join table (middle table):
CREATE TABLE book_authors (
    book_id BIGINT,
    author_id BIGINT,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id) REFERENCES books(book_id),
    FOREIGN KEY (author_id) REFERENCES authors(author_id)
);
```

**Key points:**
- **One side uses `@JoinTable`** (owning side) - BookEntity
- **Other side uses `mappedBy`** (inverse side) - AuthorEntity
- **Join table has no entity class** - Hibernate manages it automatically
- **Use `Set` not `List`** - No duplicates, better for many-to-many

**Your @ManyToMany is PERFECT.** This is textbook correct.

---

### Bidirectional vs Unidirectional

**Unidirectional:** Only one side knows about the relationship.

```java
// BookEntity.java (unidirectional @ManyToOne):
@ManyToOne
private ShelfEntity shelf;

// ShelfEntity.java (no reference to books):
// (nothing)
```

**Navigation:**
- ✅ `book.getShelf()` - Works
- ❌ `shelf.getBooks()` - Doesn't exist

---

**Bidirectional:** Both sides know about the relationship.

```java
// BookEntity.java (owning side):
@ManyToOne
@JoinColumn(name = "shelf_id")
private ShelfEntity shelf;

// ShelfEntity.java (inverse side):
@OneToMany(mappedBy = "shelf")
private List<BookEntity> books;
```

**Navigation:**
- ✅ `book.getShelf()` - Works
- ✅ `shelf.getBooks()` - Works

**When to use bidirectional:**
- You need to navigate **both ways** (Book → Shelf AND Shelf → Books)
- Convenient but requires **synchronization** (keep both sides consistent)

---

## Part 4: Fixing Bibby's Relationships

### Issue 1: BookEntity.shelfId Should Be @ManyToOne

**Current code (WRONG):**

```java
// BookEntity.java:24
private Long shelfId;

public Long getShelfId() {
    return shelfId;
}

public void setShelfId(Long shelfId) {
    this.shelfId = shelfId;
}
```

**Problems:**
1. ❌ Can't navigate: `book.getShelf().getShelfLabel()`
2. ❌ Can't write queries: `findByShelf_BookcaseId(Long id)`
3. ❌ No foreign key constraint (data integrity risk)
4. ❌ Manual ID management (error-prone)

---

**Fixed code (CORRECT):**

```java
// BookEntity.java:24 - REPLACE shelfId with:
@ManyToOne
@JoinColumn(name = "shelf_id")
private ShelfEntity shelf;

public ShelfEntity getShelf() {
    return shelf;
}

public void setShelf(ShelfEntity shelf) {
    this.shelf = shelf;
}
```

**Benefits:**
1. ✅ Navigate: `book.getShelf().getShelfLabel()`
2. ✅ Nested queries: `findByShelf_BookcaseId(Long id)`
3. ✅ Foreign key constraint enforced by database
4. ✅ Object-oriented design (work with objects, not IDs)

---

**Migration impact:**

```java
// BEFORE (using Long):
BookEntity book = new BookEntity();
book.setShelfId(123L);  // Set ID directly
bookRepository.save(book);

// AFTER (using @ManyToOne):
ShelfEntity shelf = shelfRepository.findById(123L).orElseThrow();
BookEntity book = new BookEntity();
book.setShelf(shelf);  // Set object reference
bookRepository.save(book);
```

**Database change:** None! The `shelf_id` column already exists. Hibernate just treats it as a foreign key now.

---

### Issue 2: ShelfEntity.bookcaseId Should Be @ManyToOne

**Current code (WRONG):**

```java
// ShelfEntity.java:13
private Long bookcaseId;
```

**Fixed code (CORRECT):**

```java
// ShelfEntity.java:13 - REPLACE bookcaseId with:
@ManyToOne
@JoinColumn(name = "bookcase_id")
private BookcaseEntity bookcase;

public BookcaseEntity getBookcase() {
    return bookcase;
}

public void setBookcase(BookcaseEntity bookcase) {
    this.bookcase = bookcase;
}
```

**Benefits:**
- Navigate: `shelf.getBookcase().getBookcaseLabel()`
- Nested queries: `findByBookcase_BookcaseLabel(String label)`

---

### Issue 3: Add @OneToMany Back-References (Optional but Recommended)

**Add to ShelfEntity:**

```java
// ShelfEntity.java - ADD THIS FIELD:
@OneToMany(mappedBy = "shelf")
private List<BookEntity> books = new ArrayList<>();

public List<BookEntity> getBooks() {
    return Collections.unmodifiableList(books);  // Defensive copy
}
```

**Benefits:**
- Navigate: `shelf.getBooks()` returns all books on this shelf
- Convenient for displaying shelf contents

---

**Add to BookcaseEntity:**

```java
// BookcaseEntity.java - ADD THIS FIELD:
@OneToMany(mappedBy = "bookcase")
private List<ShelfEntity> shelves = new ArrayList<>();

public List<ShelfEntity> getShelves() {
    return Collections.unmodifiableList(shelves);  // Defensive copy
}
```

**Benefits:**
- Navigate: `bookcase.getShelves()` returns all shelves in this bookcase
- Convenient for displaying bookcase structure

---

### The Complete Fixed Entity Model

**After all fixes, your relationships will be:**

```
┌──────────────────────┐
│  BookcaseEntity      │
│  @OneToMany shelves  │◀────────────┐
└──────────────────────┘             │
                                     │ @ManyToOne bookcase
                                     │
┌──────────────────────┐             │
│  ShelfEntity         │─────────────┘
│  @OneToMany books    │◀────────────┐
└──────────────────────┘             │
                                     │ @ManyToOne shelf
                                     │
┌──────────────────────┐             │
│  BookEntity          │─────────────┘
│  @ManyToMany authors │◀────────────┐
└──────────────────────┘             │ @ManyToMany(mappedBy)
                                     │
┌──────────────────────┐             │
│  AuthorEntity        │─────────────┘
│  @ManyToMany books   │
└──────────────────────┘
```

**All relationships properly mapped!**

---

## Part 5: equals() and hashCode() for Entities

**Critical question:** How does Java know if two `BookEntity` objects represent the same book?

**Answer:** It doesn't. By default, Java uses **object identity** (memory address):

```java
BookEntity book1 = bookRepository.findById(1L).get();
BookEntity book2 = bookRepository.findById(1L).get();  // Same book from database

System.out.println(book1 == book2);         // false (different objects in memory)
System.out.println(book1.equals(book2));    // false (no equals() override)

Set<BookEntity> books = new HashSet<>();
books.add(book1);
books.add(book2);  // Adds as DUPLICATE! (HashSet thinks they're different)
System.out.println(books.size());  // 2 (WRONG! Should be 1)
```

**Problem:** None of your entities override `equals()` or `hashCode()`. This causes bugs with:
- `HashSet<BookEntity>` (duplicates)
- `HashMap<BookEntity, ?>` (can't find values)
- `List.contains(book)` (returns false even when present)

---

### The JPA equals()/hashCode() Challenge

**Naive approach (DON'T DO THIS):**

```java
// BookEntity.java - WRONG APPROACH:
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BookEntity)) return false;
    BookEntity that = (BookEntity) o;
    return Objects.equals(bookId, that.bookId);  // ❌ DANGEROUS!
}

@Override
public int hashCode() {
    return Objects.hash(bookId);  // ❌ DANGEROUS!
}
```

**Why this is WRONG:**

```java
// Create new book (not saved yet):
BookEntity book = new BookEntity("1984", authors);
System.out.println(book.getBookId());  // null (not saved to database yet)

Set<BookEntity> books = new HashSet<>();
books.add(book);  // hashCode() calculated using null ID

bookRepository.save(book);  // Now bookId = 42 (assigned by database)
System.out.println(book.getBookId());  // 42

System.out.println(books.contains(book));  // FALSE! (hashCode changed!)
```

**Problem:** `hashCode()` must **never change** after object is added to a collection. But `bookId` changes from `null` to `42` after saving.

---

### The Solution: Business Key Equality

**Use natural/business keys** instead of database ID:

```java
// BookEntity.java - CORRECT APPROACH:
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BookEntity)) return false;
    BookEntity that = (BookEntity) o;

    // Use business key (isbn) instead of ID:
    return isbn != null && Objects.equals(isbn, that.isbn);
}

@Override
public int hashCode() {
    // Use a constant or business key:
    return Objects.hash(isbn);
}
```

**Business key:** Natural unique identifier (ISBN for books, email for users, SSN for employees).

**Why this works:**
- `isbn` is set in constructor (never null for valid books)
- `isbn` never changes (immutable natural key)
- `hashCode()` stable across save operations

---

### Alternative: UUID-Based Equality

**If no natural key exists:**

```java
// BookEntity.java - UUID APPROACH:
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;  // Generated immediately in constructor

@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BookEntity)) return false;
    BookEntity that = (BookEntity) o;
    return Objects.equals(id, that.id);
}

@Override
public int hashCode() {
    return Objects.hash(id);  // UUID generated before adding to collections
}
```

**Why this works:**
- UUID generated in constructor (never null)
- UUID immutable (never changes)

---

### Recommendation for Bibby

**BookEntity:**
```java
// Use ISBN as business key:
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BookEntity)) return false;
    BookEntity that = (BookEntity) o;
    return isbn != null && Objects.equals(isbn, that.isbn);
}

@Override
public int hashCode() {
    return Objects.hash(isbn);
}
```

**AuthorEntity:**
```java
// Use firstName + lastName as business key:
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AuthorEntity)) return false;
    AuthorEntity that = (AuthorEntity) o;
    return Objects.equals(firstName, that.firstName) &&
           Objects.equals(lastName, that.lastName);
}

@Override
public int hashCode() {
    return Objects.hash(firstName, lastName);
}
```

**ShelfEntity and BookcaseEntity:**
```java
// Use label as business key (assuming unique):
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ShelfEntity)) return false;
    ShelfEntity that = (ShelfEntity) o;
    return Objects.equals(shelfLabel, that.shelfLabel);
}

@Override
public int hashCode() {
    return Objects.hash(shelfLabel);
}
```

---

## Part 6: Cascade Operations

**Question:** When you delete a `BookcaseEntity`, should Hibernate also delete all its `shelves`?

**Answer:** Depends on `cascade` settings.

### Cascade Types

```java
@OneToMany(mappedBy = "bookcase", cascade = CascadeType.ALL)
private List<ShelfEntity> shelves;
```

**Cascade options:**

| Cascade Type | What it does | Example |
|--------------|--------------|---------|
| `PERSIST` | Save children when parent saved | Save bookcase → saves shelves |
| `MERGE` | Update children when parent updated | Update bookcase → updates shelves |
| `REMOVE` | Delete children when parent deleted | Delete bookcase → deletes shelves |
| `REFRESH` | Reload children when parent reloaded | Refresh bookcase → refreshes shelves |
| `DETACH` | Detach children when parent detached | Detach bookcase → detaches shelves |
| `ALL` | All of the above | All operations cascade |

---

### Cascade Recommendations for Bibby

**Bookcase → Shelf (Strong ownership):**
```java
// BookcaseEntity.java:
@OneToMany(mappedBy = "bookcase", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ShelfEntity> shelves;
```

**Explanation:**
- `cascade = CascadeType.ALL` - Deleting bookcase deletes shelves
- `orphanRemoval = true` - Removing shelf from list deletes it from database

**Use when:** Parent **owns** children (strong lifecycle coupling).

---

**Shelf → Book (Weak ownership):**
```java
// ShelfEntity.java:
@OneToMany(mappedBy = "shelf")  // NO CASCADE
private List<BookEntity> books;
```

**Explanation:**
- No cascade - Deleting shelf does NOT delete books
- Books are valuable domain objects (shouldn't be deleted with shelf)

**Use when:** Parent **references** children (weak coupling).

---

**Book ↔ Author (No ownership):**
```java
// BookEntity.java:
@ManyToMany  // NO CASCADE
private Set<AuthorEntity> authors;
```

**Explanation:**
- No cascade - Deleting book does NOT delete authors
- Authors exist independently of books

**Use when:** Many-to-many relationships (no ownership).

---

## Part 7: Fetch Strategies (@ManyToOne is EAGER by default!)

**Performance trap:**

```java
// Find all books:
List<BookEntity> books = bookRepository.findAll();

// If shelf is EAGER (default for @ManyToOne):
// SELECT * FROM books;
// SELECT * FROM shelves WHERE shelf_id = 1;
// SELECT * FROM shelves WHERE shelf_id = 2;
// SELECT * FROM shelves WHERE shelf_id = 3;
// ... (N+1 query problem!)
```

**Problem:** Fetching 100 books triggers 1 + 100 = **101 queries**!

---

### Fetch Types

| Fetch Type | When loaded | Default for |
|------------|-------------|-------------|
| `EAGER` | Immediately with parent | `@ManyToOne`, `@OneToOne` |
| `LAZY` | Only when accessed | `@OneToMany`, `@ManyToMany` |

---

### Fix: Make @ManyToOne LAZY

```java
// BookEntity.java - ADD fetch = FetchType.LAZY:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "shelf_id")
private ShelfEntity shelf;
```

**Result:**
```java
List<BookEntity> books = bookRepository.findAll();
// SELECT * FROM books;  (1 query only!)

BookEntity book = books.get(0);
book.getShelf().getShelfLabel();  // NOW it fetches shelf (lazy load)
// SELECT * FROM shelves WHERE shelf_id = 1;
```

---

### When to Use Eager vs Lazy

**Use EAGER when:**
- You **always** need the related entity (99% of the time)
- Single object fetch (`findById()`)

**Use LAZY when:**
- You **sometimes** need the related entity
- Fetching collections (`findAll()`, `findByShelfId()`)
- Performance matters

**Recommendation for Bibby:**

```java
// BookEntity.java - LAZY (often fetch many books at once):
@ManyToOne(fetch = FetchType.LAZY)
private ShelfEntity shelf;

// ShelfEntity.java - LAZY (often fetch many shelves at once):
@ManyToOne(fetch = FetchType.LAZY)
private BookcaseEntity bookcase;

// AuthorEntity/BookEntity - LAZY (default, already correct):
@ManyToMany  // Default fetch = LAZY
private Set<AuthorEntity> authors;
```

---

### Solving N+1 with JOIN FETCH

**If you need shelf data when fetching books:**

```java
// BookRepository.java - ADD THIS QUERY:
@Query("SELECT b FROM BookEntity b JOIN FETCH b.shelf WHERE b.shelfId = :shelfId")
List<BookEntity> findByShelfIdWithShelf(@Param("shelfId") Long shelfId);
```

**Result:**
```sql
-- Single query with JOIN (no N+1 problem):
SELECT b.*, s.*
FROM books b
JOIN shelves s ON b.shelf_id = s.shelf_id
WHERE b.shelf_id = 42;
```

**Use when:** You know you'll need the relationship (avoids lazy load queries).

---

## Part 8: Real-World Entity Design - The Complete BookEntity

**Here's what BookEntity should look like after all fixes:**

```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.shelf.ShelfEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "books")
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long bookId;

    private String title;
    private String isbn;
    private String publisher;
    private int publicationYear;
    private String genre;
    private int edition;
    private String description;
    private Integer checkoutCount;
    private String bookStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // ✅ FIXED: @ManyToOne instead of Long shelfId
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private ShelfEntity shelf;

    // ✅ CORRECT: @ManyToMany with join table
    @ManyToMany
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<AuthorEntity> authors = new HashSet<>();

    // ✅ REQUIRED: No-arg constructor
    protected BookEntity() {
    }

    // ✅ RECOMMENDED: Business constructor
    public BookEntity(String title, String isbn, Set<AuthorEntity> authors) {
        this.title = title;
        this.isbn = isbn;
        this.authors = authors;
        this.createdAt = LocalDate.now();
    }

    // ✅ FIXED: equals() using business key (isbn)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookEntity)) return false;
        BookEntity that = (BookEntity) o;
        return isbn != null && Objects.equals(isbn, that.isbn);
    }

    // ✅ FIXED: hashCode() using business key (isbn)
    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }

    // Getters and setters (omitted for brevity)
    // ...
}
```

**Changes made:**
1. ✅ `Long shelfId` → `ShelfEntity shelf` with `@ManyToOne`
2. ✅ Added `fetch = FetchType.LAZY` to avoid N+1
3. ✅ Added `equals()` using `isbn` business key
4. ✅ Added `hashCode()` using `isbn`
5. ✅ No-arg constructor is `protected` (JPA best practice)
6. ✅ Business constructor for creating valid books

---

## Summary: Your Entity Design - Before & After

### Before (Current State):

| Entity | Grade | Issues |
|--------|-------|--------|
| BookEntity | C+ | shelfId as Long, no equals/hashCode |
| AuthorEntity | B- | fullName stored, no equals/hashCode |
| ShelfEntity | D | bookcaseId as Long, no relationships to books, no equals/hashCode |
| BookcaseEntity | D | No relationships, no equals/hashCode |
| CatalogEntity | F | Not even a JPA entity! |

**Overall Grade: D**

---

### After (All Fixes Applied):

| Entity | Fixes | Grade |
|--------|-------|-------|
| BookEntity | ✅ @ManyToOne shelf, ✅ equals/hashCode, ✅ LAZY fetch | A |
| AuthorEntity | ✅ equals/hashCode, (fullName fixed in Section 7) | A- |
| ShelfEntity | ✅ @ManyToOne bookcase, ✅ @OneToMany books, ✅ equals/hashCode, ✅ LAZY fetch | A |
| BookcaseEntity | ✅ @OneToMany shelves, ✅ equals/hashCode | A |
| CatalogEntity | ✅ Delete or implement properly | N/A |

**Overall Grade: A**

---

## Action Items

### Priority 1: Fix Relationship Mappings (CRITICAL)

**1. BookEntity - Change shelfId to @ManyToOne:**

```java
// BookEntity.java:24 - DELETE THIS:
private Long shelfId;
public Long getShelfId() { return shelfId; }
public void setShelfId(Long shelfId) { this.shelfId = shelfId; }

// REPLACE WITH:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "shelf_id")
private ShelfEntity shelf;

public ShelfEntity getShelf() {
    return shelf;
}

public void setShelf(ShelfEntity shelf) {
    this.shelf = shelf;
}
```

**Impact:** Enables nested queries, object navigation, type safety.

---

**2. ShelfEntity - Change bookcaseId to @ManyToOne:**

```java
// ShelfEntity.java:13 - DELETE THIS:
private Long bookcaseId;
public Long getBookcaseId() { return bookcaseId; }
public void setBookcaseId(Long bookcaseId) { this.bookcaseId = bookcaseId; }

// REPLACE WITH:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "bookcase_id")
private BookcaseEntity bookcase;

public BookcaseEntity getBookcase() {
    return bookcase;
}

public void setBookcase(BookcaseEntity bookcase) {
    this.bookcase = bookcase;
}
```

---

**3. ShelfEntity - Add @OneToMany books:**

```java
// ShelfEntity.java - ADD THIS FIELD:
@OneToMany(mappedBy = "shelf")
private List<BookEntity> books = new ArrayList<>();

public List<BookEntity> getBooks() {
    return Collections.unmodifiableList(books);
}
```

---

**4. BookcaseEntity - Add @OneToMany shelves:**

```java
// BookcaseEntity.java - ADD THIS FIELD:
@OneToMany(mappedBy = "bookcase", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ShelfEntity> shelves = new ArrayList<>();

public List<ShelfEntity> getShelves() {
    return Collections.unmodifiableList(shelves);
}
```

---

### Priority 2: Add equals()/hashCode() to All Entities

**BookEntity (use isbn):**
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BookEntity)) return false;
    BookEntity that = (BookEntity) o;
    return isbn != null && Objects.equals(isbn, that.isbn);
}

@Override
public int hashCode() {
    return Objects.hash(isbn);
}
```

**AuthorEntity (use firstName + lastName):**
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AuthorEntity)) return false;
    AuthorEntity that = (AuthorEntity) o;
    return Objects.equals(firstName, that.firstName) &&
           Objects.equals(lastName, that.lastName);
}

@Override
public int hashCode() {
    return Objects.hash(firstName, lastName);
}
```

**ShelfEntity (use shelfLabel + bookcaseId):**
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ShelfEntity)) return false;
    ShelfEntity that = (ShelfEntity) o;
    return Objects.equals(shelfLabel, that.shelfLabel) &&
           Objects.equals(bookcase, that.bookcase);
}

@Override
public int hashCode() {
    return Objects.hash(shelfLabel, bookcase);
}
```

**BookcaseEntity (use bookcaseLabel):**
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BookcaseEntity)) return false;
    BookcaseEntity that = (BookcaseEntity) o;
    return Objects.equals(bookcaseLabel, that.bookcaseLabel);
}

@Override
public int hashCode() {
    return Objects.hash(bookcaseLabel);
}
```

---

### Priority 3: Delete or Fix CatalogEntity

**Option 1: Delete (if unused):**
```bash
# Delete files:
rm src/main/java/com/penrose/bibby/library/catalog/CatalogEntity.java
rm src/main/java/com/penrose/bibby/library/catalog/CatalogRepository.java
```

**Option 2: Implement (if needed):**
```java
@Entity
@Table(name = "catalogs")
public class CatalogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long catalogId;

    private String catalogName;

    // Add fields, getters, setters, equals, hashCode
}
```

---

### Priority 4: Update Service/Controller Code

**After changing to @ManyToOne, update code that uses shelfId:**

```java
// BookService.java - BEFORE:
book.setShelfId(123L);

// AFTER:
ShelfEntity shelf = shelfRepository.findById(123L)
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
book.setShelf(shelf);
```

**Search for usages:**
```bash
grep -r "setShelfId" src/
grep -r "getShelfId" src/
grep -r "setBookcaseId" src/
grep -r "getBookcaseId" src/
```

---

## What You Learned

1. **Entity basics:** `@Entity`, `@Table`, `@Id`, `@GeneratedValue`

2. **Relationship mappings:**
   - `@ManyToOne` - Owning side (foreign key), enables navigation
   - `@OneToMany` - Inverse side (mappedBy), convenience
   - `@ManyToMany` - Join table required, both sides equal

3. **Bibby's critical issues:**
   - BookEntity.shelfId should be @ManyToOne
   - ShelfEntity.bookcaseId should be @ManyToOne
   - Missing @OneToMany back-references
   - No equals()/hashCode() implementations

4. **equals()/hashCode() for JPA:**
   - Never use database ID (changes after save)
   - Use business keys (isbn, email, etc.)
   - Must be stable (never change after adding to collections)

5. **Fetch strategies:**
   - EAGER: Load immediately (default for @ManyToOne)
   - LAZY: Load on access (default for @OneToMany)
   - Use LAZY for @ManyToOne to avoid N+1 queries

6. **Cascade operations:**
   - CascadeType.ALL for strong ownership (Bookcase → Shelf)
   - No cascade for weak relationships (Shelf → Book)

---

**Next:** Section 27 - DTO Pattern & Layer Boundaries will explore how to keep entities internal and expose DTOs to controllers/CLI, preventing entity exposure issues.

**Your entities are the foundation of Bibby.** These fixes transform them from **Grade D to Grade A**, enabling powerful queries, type-safe navigation, and proper object-oriented design.

You're building excellent fundamentals. Keep going!

---

*Section 26 complete. 26 of 33 sections finished (79%).*
*Next up: Section 27 - DTO Pattern & Layer Boundaries*
