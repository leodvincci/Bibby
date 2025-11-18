# Section 25: Spring Data JPA Deep Dive

**Learning objective:** Understand how Spring Data JPA generates repository implementations from method names, master query method naming conventions, use custom `@Query` annotations for complex queries, and leverage projections to return exactly the data you need.

**Why this matters:** You write an interface method like `findByTitleContaining(String title)` with ZERO implementation code, and Spring Data JPA generates the SQL query for you. Understanding the "magic" behind this lets you write powerful queries in seconds and debug issues when things don't work as expected.

**Duration:** 60 min read

---

## The Magic Question

Look at this code from your `BookRepository.java`:

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    List<BookEntity> findByTitleContaining(String title);

    List<BookEntity> findByShelfId(Long id);

    BookEntity findByTitleIgnoreCase(String title);
}
```

**Question:** Where's the implementation? Where's the SQL? How does `findByTitleContaining()` know to use a LIKE query?

**Answer:** Spring Data JPA **generates the implementation at runtime** by parsing the method name.

This section reveals exactly how that works, using **your actual repository methods** from Bibby.

---

## What You'll Learn From Your Code

You have **5 repository interfaces** in Bibby:
- **BookRepository** (10 query methods - most complex)
- **AuthorRepository** (3 query methods, including nested property navigation)
- **ShelfRepository** (2 query methods, including JPQL with constructor)
- **BookcaseRepository** (1 query method)
- **CatalogRepository** (empty interface, no methods)

Total: **16 query methods** demonstrating:
- ✅ Simple property queries
- ✅ Case-insensitive matching
- ✅ LIKE queries with `Containing`
- ✅ Multiple properties with `And`
- ✅ Nested property navigation through `@ManyToMany` relationships
- ✅ Sorting with `OrderBy`
- ✅ Native SQL queries with `@Query`
- ✅ JPQL queries with constructor expressions
- ✅ Projection interfaces (records)

We'll analyze every single method and explain how Spring Data JPA turns method names into SQL.

---

## Part 1: How Spring Data JPA Works

### The Three-Layer Architecture

When you write this:

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    BookEntity findByTitle(String title);
}
```

**At runtime**, Spring Data JPA creates a **proxy object** that implements your interface:

```
Your Code                    Spring-Generated Proxy               Database
───────────                  ──────────────────────               ────────
BookRepository  ──────────>  BookRepositoryImpl  ─────────────>  PostgreSQL
(interface)                  (generated class)

findByTitle()  ──────────>  1. Parse method name                 SELECT *
                            2. Build query                        FROM books
                            3. Execute SQL                        WHERE title = ?
                            4. Map results to entities
```

### The Proxy Generation Process

**When your application starts:**

1. Spring finds `@Repository` interfaces extending `JpaRepository`
2. Spring Data JPA scans **every method name** in the interface
3. For each method, it **parses the name** to understand the query intent
4. It **generates a query** (JPQL or SQL) based on the method name
5. It **creates a proxy class** that implements your interface
6. When you call the method, the proxy **executes the generated query**

Let's see this in action with your actual methods.

---

## Part 2: Query Method Naming Conventions

### The Anatomy of a Query Method Name

Every Spring Data JPA query method follows this pattern:

```
[verb][Subject][By][Property][Modifier][OrderBy][Property][Direction]
```

Let's break down your method: `findBookSummariesByShelfIdOrderByTitleAsc`

```
find        ──> Verb (find, get, query, read, count, delete)
BookSummaries ──> Subject (return type hint, optional)
By          ──> Separator (marks start of query criteria)
ShelfId     ──> Property (must match BookEntity field name EXACTLY)
OrderBy     ──> Sorting keyword
Title       ──> Sort property
Asc         ──> Sort direction (Asc or Desc)
```

**Generated SQL:**
```sql
SELECT b.book_id, b.title
FROM books b
WHERE b.shelf_id = ?
ORDER BY b.title ASC
```

### Your Repository Methods - Complete Breakdown

Let's analyze **every query method** in Bibby.

---

### BookRepository.java (10 methods)

#### 1. `BookEntity findBookEntityByTitle(String title)`

**Location:** BookRepository.java:14

**Parsed as:**
- Verb: `find`
- Subject: `BookEntity` (redundant, return type already specifies this)
- By: starts criteria
- Property: `title` → maps to `BookEntity.title` field

**Generated JPQL:**
```java
SELECT b FROM BookEntity b WHERE b.title = :title
```

**Generated SQL:**
```sql
SELECT * FROM books WHERE title = ?
```

**Return type:** Single `BookEntity` (or `null` if not found)

**Grade:** B (works, but verbose naming - see next method)

---

#### 2. `BookEntity findByTitle(String title)`

**Location:** BookRepository.java:16

**Parsed as:**
- Verb: `find`
- Property: `title`

**Generated SQL:** (same as method #1)
```sql
SELECT * FROM books WHERE title = ?
```

**Why duplicate methods?** You have `findBookEntityByTitle()` and `findByTitle()` doing the EXACT same thing. Spring Data JPA doesn't care about the "Subject" part - it only looks at the **return type** and **property names**.

**Recommendation:** Delete `findBookEntityByTitle()` - it's redundant. The shorter name is clearer.

---

#### 3. `BookEntity findByTitleIgnoreCase(String title)`

**Location:** BookRepository.java:18

**Parsed as:**
- Verb: `find`
- Property: `title`
- Modifier: `IgnoreCase` → adds `UPPER()` comparison

**Generated SQL:**
```sql
SELECT * FROM books WHERE UPPER(title) = UPPER(?)
```

**Use case:** Finding "The Great Gatsby" when user types "the great gatsby" or "THE GREAT GATSBY"

**Important:** `IgnoreCase` only works with `String` properties. You can't do `findByShelfIdIgnoreCase()` because `shelfId` is a `Long`.

---

#### 4. `List<BookEntity> findByTitleContaining(String title)`

**Location:** BookRepository.java:20

**Parsed as:**
- Verb: `find`
- Property: `title`
- Modifier: `Containing` → adds `LIKE '%?%'`

**Generated SQL:**
```sql
SELECT * FROM books WHERE title LIKE ?
```

**Parameter transformation:** Spring adds `%` wildcards automatically:
```java
// You call:
bookRepository.findByTitleContaining("Great");

// Spring transforms to:
WHERE title LIKE '%Great%'
```

**Similar modifiers you could use:**
- `findByTitleStartingWith("The")` → `WHERE title LIKE 'The%'`
- `findByTitleEndingWith("Gatsby")` → `WHERE title LIKE '%Gatsby'`
- `findByTitleContainingIgnoreCase("great")` → `WHERE UPPER(title) LIKE UPPER('%great%')`

**Return type:** `List<BookEntity>` (empty list if no matches, NEVER null)

---

#### 5. `List<BookEntity> findByShelfId(Long id)`

**Location:** BookRepository.java:22

**Parsed as:**
- Verb: `find`
- Property: `shelfId` → maps to `BookEntity.shelfId` field (line 24)

**Generated SQL:**
```sql
SELECT * FROM books WHERE shelf_id = ?
```

**Important discovery:** `BookEntity.shelfId` is a **plain `Long` field**, NOT a `@ManyToOne` relationship:

```java
// BookEntity.java:24 (CURRENT - not ideal)
private Long shelfId;

// BETTER DESIGN (what it should be):
@ManyToOne
@JoinColumn(name = "shelf_id")
private ShelfEntity shelf;
```

**Why this matters:** Because `shelfId` is just a `Long`, you **can't** do nested queries like:

```java
// This WON'T work (shelfId is not an object with a bookcaseId property):
List<BookEntity> findByShelf_BookcaseId(Long bookcaseId);

// This DOES work (shelfId is a Long):
List<BookEntity> findByShelfId(Long shelfId);
```

**We'll discuss this design issue in Section 26 (Entity Design & JPA).**

---

#### 6. `List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId)`

**Location:** BookRepository.java:24

**This is your most sophisticated query method name!**

**Parsed as:**
- Verb: `find`
- Subject: `BookSummaries` (return type hint)
- Property: `shelfId`
- OrderBy: sorting keyword
- Sort property: `title`
- Sort direction: `Asc`

**Generated SQL:**
```sql
SELECT b.book_id, b.title
FROM books b
WHERE b.shelf_id = ?
ORDER BY b.title ASC
```

**Return type:** `List<BookSummary>` (projection interface)

**What's a projection?** Instead of returning the full `BookEntity` (13 fields), you return a **projection** with only the fields you need (2 fields).

**Your BookSummary record:**
```java
// BookSummary.java:3
public record BookSummary(Long bookId, String title) {
}
```

**How Spring Data JPA maps results to projections:**

1. Spring sees return type `BookSummary`
2. Spring looks at the record components: `bookId` and `title`
3. Spring **only selects those columns** from the database
4. Spring creates `BookSummary` instances using the record constructor

**Benefits of projections:**
- **Performance:** Only fetches 2 columns instead of 13
- **Network efficiency:** Smaller result sets
- **Memory efficiency:** Smaller objects in memory
- **Clearer intent:** The method signature tells you exactly what data you get

**We'll deep-dive into projections in Part 4.**

---

#### 7. `@Query` - `BookDetailView getBookDetailView(Long bookId)`

**Location:** BookRepository.java:27-39

**This is a custom query using `@Query` annotation:**

```java
@Query(value = """
    SELECT b.book_id, b.title,
           STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
           bc.bookcase_label, s.shelf_label, b.book_status
    FROM books b
    JOIN book_authors ba ON b.book_id = ba.book_id
    JOIN authors a ON ba.author_id = a.author_id
    JOIN shelves s ON s.shelf_id = b.shelf_id
    JOIN bookcases bc ON bc.bookcase_id = s.bookcase_id
    WHERE b.book_id = :bookId
    GROUP BY b.book_id, b.title, bc.bookcase_label, s.shelf_label, b.book_status
""", nativeQuery = true)
BookDetailView getBookDetailView(Long bookId);
```

**Why use `@Query` instead of method name?**

**Method name equivalent would be:**
```java
// This is NOT possible with method names!
findBookDetailViewByBookIdWithAuthorsAndShelfAndBookcase(Long bookId)
```

**Reasons to use `@Query`:**
1. **Complex joins** - This query joins 4 tables (books, book_authors, authors, shelves, bookcases)
2. **Aggregation** - `STRING_AGG()` concatenates all authors into a single string
3. **GROUP BY** - Required when using aggregation functions
4. **Database-specific functions** - `STRING_AGG()` is PostgreSQL-specific

**Key annotations:**
- `@Query` - Marks a custom query
- `value = """..."""` - Text block (Java 15+) for readable multi-line SQL
- `nativeQuery = true` - This is **raw SQL**, not JPQL (Hibernate dialect)
- `:bookId` - Named parameter binding

**Return type:** `BookDetailView` (record projection)

```java
// BookDetailView.java:3
public record BookDetailView(
    Long bookId,
    String title,
    String authors,        // "Isaac Asimov, Robert Heinlein" (comma-separated)
    String bookcaseLabel,
    String shelfLabel,
    String bookStatus
) {}
```

**How Spring maps SQL columns to record:**
- SQL column `book_id` → record field `bookId` (underscore_case → camelCase)
- SQL alias `authors` → record field `authors`
- SQL column `bookcase_label` → record field `bookcaseLabel`

**Important:** Field names must match (case-insensitive), or use `AS` aliases.

**JPQL vs Native SQL:**
- **JPQL** (Java Persistence Query Language) - uses entity names and field names (portable across databases)
- **Native SQL** - uses table names and column names (database-specific)

**This query uses Native SQL because:**
- `STRING_AGG()` is PostgreSQL-specific (not supported in JPQL)
- Direct table/column access is clearer for complex joins
- GROUP BY is easier to write in SQL than JPQL

---

### AuthorRepository.java (3 methods)

#### 8. `AuthorEntity getByFullName(String fullName)`

**Location:** AuthorRepository.java:11

**Parsed as:**
- Verb: `get` (instead of `find` - same behavior)
- Property: `fullName` → maps to `AuthorEntity.fullName` field

**Generated SQL:**
```sql
SELECT * FROM authors WHERE full_name = ?
```

**Get vs Find:**
- `findBy...` - Most common convention
- `getBy...` - Same behavior, different style
- `queryBy...` - Same behavior
- `readBy...` - Same behavior

**Recommendation:** Stick with `findBy` for consistency. Mixing `get` and `find` is confusing.

**Return type:** Single `AuthorEntity` (or `null` if not found)

**Important note:** `AuthorEntity.fullName` (line 17) is a **derived field** (calculated from firstName + lastName). This is a design smell - we covered this in Section 7 (Constructors & Object Lifecycle). The fullName field shouldn't exist; it should be a **getter method**:

```java
// CURRENT (stored in database - redundant):
private String fullName;

// BETTER (calculated on-demand):
public String getFullName() {
    return firstName + " " + lastName;
}
```

---

#### 9. `AuthorEntity findByFirstNameAndLastName(String firstName, String lastName)`

**Location:** AuthorRepository.java:13

**Parsed as:**
- Verb: `find`
- Property 1: `firstName`
- Operator: `And`
- Property 2: `lastName`

**Generated SQL:**
```sql
SELECT * FROM authors WHERE first_name = ? AND last_name = ?
```

**Parameter binding order matters:**
```java
// Method signature order:
findByFirstNameAndLastName(String firstName, String lastName)

// Must match query order:
WHERE first_name = :firstName AND last_name = :lastName

// Correct call:
authorRepository.findByFirstNameAndLastName("Isaac", "Asimov");

// WRONG (parameters swapped):
authorRepository.findByFirstNameAndLastName("Asimov", "Isaac"); // Won't find anything
```

**Other operators you can use:**
- `And` - Both conditions must be true
- `Or` - Either condition can be true

**Examples:**
```java
// Find by first name OR last name:
findByFirstNameOrLastName(String firstName, String lastName)
// WHERE first_name = ? OR last_name = ?

// Multiple ANDs:
findByFirstNameAndLastNameAndNationality(String f, String l, String n)
// WHERE first_name = ? AND last_name = ? AND nationality = ?
```

**Return type:** Single `AuthorEntity` (assumes unique combination of first + last name)

**Design consideration:** This assumes no two authors have the same first and last name. If two "John Smith" authors exist, this method returns the **first one found** (non-deterministic). Better approach: Use `Optional<AuthorEntity>` or query by unique field (`authorId`).

---

#### 10. `List<AuthorEntity> findByBooks_BookId(Long bookId)`

**Location:** AuthorRepository.java:15

**This is NESTED PROPERTY NAVIGATION!**

**Parsed as:**
- Verb: `find`
- Property path: `books.bookId`
  - `books` → `AuthorEntity.books` field (line 42)
  - `_` → navigation operator (traverse into `BookEntity`)
  - `bookId` → `BookEntity.bookId` field

**Entity relationships:**
```java
// AuthorEntity.java:41-42
@ManyToMany(mappedBy = "authors")
private Set<BookEntity> books = new HashSet<>();

// BookEntity.java:38-44
@ManyToMany
@JoinTable(
    name = "book_authors",
    joinColumns = @JoinColumn(name = "book_id"),
    inverseJoinColumns = @JoinColumn(name = "author_id")
)
private Set<AuthorEntity> authors = new HashSet<>();
```

**Generated SQL:**
```sql
SELECT a.*
FROM authors a
JOIN book_authors ba ON a.author_id = ba.author_id
WHERE ba.book_id = ?
```

**How it works:**
1. Spring sees `books_BookId`
2. Spring looks for a field named `books` in `AuthorEntity` → found (`Set<BookEntity>`)
3. Spring sees `@ManyToMany` annotation → knows it's a relationship
4. Spring sees `_BookId` → navigate into `BookEntity` and find `bookId` field
5. Spring generates JOIN to the join table (`book_authors`)
6. Spring adds WHERE clause on `book_id`

**Underscore rules:**
- `_` is the **property navigation operator**
- `findByBooks_BookId()` → navigate through `books` relationship
- `findByBooksBookId()` → would look for a field named `booksBookId` (WRONG!)

**Return type:** `List<AuthorEntity>` - All authors who wrote this book

**Use case:** Display authors when showing book details.

---

### ShelfRepository.java (2 methods)

#### 11. `List<ShelfEntity> findByBookcaseId(Long bookcaseId)`

**Location:** ShelfRepository.java:13

**Parsed as:**
- Verb: `find`
- Property: `bookcaseId`

**Generated SQL:**
```sql
SELECT * FROM shelves WHERE bookcase_id = ?
```

**Pattern:** Same as `findByShelfId()` in BookRepository - simple foreign key lookup.

---

#### 12. `@Query` - `List<ShelfSummary> findShelfSummariesByBookcaseId(Long bookcaseId)`

**Location:** ShelfRepository.java:15-27

**This uses JPQL (Java Persistence Query Language) with a constructor expression:**

```java
@Query("""
    SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
        s.shelfId,
        s.shelfLabel,
        COUNT(b.bookId)
    )
    FROM ShelfEntity s
    LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
    WHERE s.bookcaseId = :bookcaseId
    GROUP BY s.shelfId, s.shelfLabel
    ORDER BY s.shelfPosition ASC
    """)
List<ShelfSummary> findShelfSummariesByBookcaseId(@Param("bookcaseId") Long bookcaseId);
```

**Key differences from native SQL:**

| Feature | Native SQL | JPQL |
|---------|------------|------|
| Table names | `shelves`, `books` | `ShelfEntity`, `BookEntity` |
| Column names | `shelf_id`, `shelf_label` | `shelfId`, `shelfLabel` |
| Portability | PostgreSQL-specific | Works on any database |
| Functions | `STRING_AGG()` (PostgreSQL) | `COUNT()`, `AVG()`, `SUM()` (standard) |
| Result mapping | Record fields match SQL columns | `new ClassName(...)` constructor |

**Constructor expression:**
```java
SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
    s.shelfId,      // First constructor parameter
    s.shelfLabel,   // Second constructor parameter
    COUNT(b.bookId) // Third constructor parameter (calculated)
)
```

**Spring creates ShelfSummary like this:**
```java
new ShelfSummary(
    12L,           // shelfId
    "Top Shelf",   // label
    7L             // bookCount (from COUNT aggregation)
)
```

**LEFT JOIN vs INNER JOIN:**
```java
LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
```

- **LEFT JOIN** - Include shelves even if they have ZERO books (bookCount = 0)
- **INNER JOIN** - Only include shelves that have at least 1 book

**Example results:**

| Shelf | Books | LEFT JOIN Result | INNER JOIN Result |
|-------|-------|------------------|-------------------|
| Top Shelf | 7 books | ✅ Returns (count=7) | ✅ Returns (count=7) |
| Middle Shelf | 0 books | ✅ Returns (count=0) | ❌ Excluded |

**Your choice of LEFT JOIN is correct** - you want to see all shelves, even empty ones.

**Named parameter binding:**
```java
@Param("bookcaseId") Long bookcaseId
```

- `@Param("bookcaseId")` - Maps Java parameter to JPQL `:bookcaseId` placeholder
- Without `@Param`, you'd use positional: `?1`

**Why use `@Query` here instead of method name?**

You **could** use method names for parts of this query:
```java
// This works (no aggregation):
List<ShelfEntity> findByBookcaseIdOrderByShelfPositionAsc(Long bookcaseId);

// But this does NOT work (can't do COUNT in method names):
List<ShelfSummary> findByBookcaseIdWithBookCount(Long bookcaseId); // ❌ No COUNT support
```

**When to use `@Query` instead of method names:**
- Aggregation functions (`COUNT`, `SUM`, `AVG`, `MAX`, `MIN`)
- `GROUP BY` clauses
- Complex joins
- Subqueries
- Custom return types with calculated fields

---

### BookcaseRepository.java (1 method)

#### 13. `BookcaseEntity findBookcaseEntityByBookcaseLabel(String s)`

**Location:** BookcaseRepository.java:8

**Parsed as:**
- Verb: `find`
- Subject: `BookcaseEntity` (redundant)
- Property: `bookcaseLabel`

**Generated SQL:**
```sql
SELECT * FROM bookcases WHERE bookcase_label = ?
```

**Issues:**
1. Verbose name - `findByBookcaseLabel()` is clearer
2. Poor parameter name - `String s` should be `String bookcaseLabel`

**Better version:**
```java
BookcaseEntity findByBookcaseLabel(String bookcaseLabel);
```

---

## Part 3: Query Method Keywords Reference

Spring Data JPA supports **~50 keywords** in method names. Here are the most important ones you should know:

### Comparison Keywords

| Keyword | SQL Equivalent | Example |
|---------|----------------|---------|
| `Is`, `Equals` | `= ?` | `findByTitle(String title)` |
| `Not` | `!= ?` | `findByTitleNot(String title)` |
| `IsNull`, `Null` | `IS NULL` | `findByPublisherIsNull()` |
| `IsNotNull`, `NotNull` | `IS NOT NULL` | `findByPublisherIsNotNull()` |
| `LessThan` | `< ?` | `findByPublicationYearLessThan(int year)` |
| `LessThanEqual` | `<= ?` | `findByPublicationYearLessThanEqual(int year)` |
| `GreaterThan` | `> ?` | `findByPublicationYearGreaterThan(int year)` |
| `GreaterThanEqual` | `>= ?` | `findByPublicationYearGreaterThanEqual(int year)` |
| `Between` | `BETWEEN ? AND ?` | `findByPublicationYearBetween(int start, int end)` |
| `In` | `IN (?)` | `findByGenreIn(List<String> genres)` |
| `NotIn` | `NOT IN (?)` | `findByGenreNotIn(List<String> genres)` |

### String Keywords

| Keyword | SQL Equivalent | Example |
|---------|----------------|---------|
| `Like` | `LIKE ?` | `findByTitleLike(String pattern)` |
| `NotLike` | `NOT LIKE ?` | `findByTitleNotLike(String pattern)` |
| `StartingWith` | `LIKE '?%'` | `findByTitleStartingWith(String prefix)` |
| `EndingWith` | `LIKE '%?'` | `findByTitleEndingWith(String suffix)` |
| `Containing` | `LIKE '%?%'` | `findByTitleContaining(String substring)` |
| `IgnoreCase` | `UPPER(field) = UPPER(?)` | `findByTitleIgnoreCase(String title)` |

### Logical Keywords

| Keyword | SQL Equivalent | Example |
|---------|----------------|---------|
| `And` | `AND` | `findByFirstNameAndLastName(String f, String l)` |
| `Or` | `OR` | `findByFirstNameOrLastName(String f, String l)` |

### Sorting Keywords

| Keyword | SQL Equivalent | Example |
|---------|----------------|---------|
| `OrderBy...Asc` | `ORDER BY ? ASC` | `findByGenreOrderByTitleAsc(String genre)` |
| `OrderBy...Desc` | `ORDER BY ? DESC` | `findByGenreOrderByPublicationYearDesc(String genre)` |

### Special Keywords

| Keyword | Purpose | Example |
|---------|---------|---------|
| `Top`, `First` | Limit results | `findTop10ByOrderByPublicationYearDesc()` |
| `Distinct` | Remove duplicates | `findDistinctByGenre(String genre)` |
| `True`, `False` | Boolean values | `findByAvailableTrue()` |

---

## Part 4: Projections Deep Dive

**Problem:** You only need `bookId` and `title`, but `findAll()` returns **13 fields per book**:

```java
List<BookEntity> books = bookRepository.findAll();
// Returns: bookId, title, isbn, publisher, publicationYear, genre,
//          edition, description, shelfId, checkoutCount, bookStatus,
//          createdAt, updatedAt (13 fields!)
```

**Solution:** Use **projections** to select only the fields you need.

### Projection Type 1: Interface Projection (NOT used in Bibby)

**Example:**
```java
// Define projection interface:
public interface BookSummary {
    Long getBookId();
    String getTitle();
}

// Use in repository:
List<BookSummary> findByShelfId(Long shelfId);
```

**How it works:**
- Spring creates a **dynamic proxy** implementing `BookSummary`
- Proxy only fetches `bookId` and `title` from database
- Calling `getBookId()` returns the fetched value

**Pros:** Type-safe, flexible (can add default methods)
**Cons:** Slightly slower than records (proxy overhead)

---

### Projection Type 2: Record Projection (Used in Bibby!)

**Your projections:**

```java
// BookSummary.java:3
public record BookSummary(Long bookId, String title) {}

// BookDetailView.java:3
public record BookDetailView(
    Long bookId,
    String title,
    String authors,
    String bookcaseLabel,
    String shelfLabel,
    String bookStatus
) {}

// ShelfSummary.java:3
public record ShelfSummary(Long shelfId, String label, long bookCount) {}
```

**How it works:**
1. Spring sees return type is a record
2. Spring looks at record components (field names)
3. Spring **only selects those columns** from database
4. Spring creates record instances using **canonical constructor**

**SQL comparison:**

```java
// Without projection (13 columns):
List<BookEntity> findByShelfId(Long shelfId);
// SELECT book_id, title, isbn, publisher, publication_year, genre,
//        edition, description, shelf_id, checkout_count, book_status,
//        created_at, updated_at FROM books WHERE shelf_id = ?

// With projection (2 columns):
List<BookSummary> findByShelfId(Long shelfId);
// SELECT book_id, title FROM books WHERE shelf_id = ?
```

**Performance impact:**
- **Network:** 85% less data transferred (2 fields vs 13 fields)
- **Memory:** Smaller objects (16 bytes vs ~200 bytes per record)
- **Parsing:** Faster result set mapping

**Pros:** Immutable, fast, clear intent, Java 17+ syntax
**Cons:** Can't add methods (records are final)

---

### Projection Type 3: Constructor Expression (JPQL Only)

**Your example:**
```java
// ShelfRepository.java:16
SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
    s.shelfId,
    s.shelfLabel,
    COUNT(b.bookId)  // Calculated field!
)
FROM ShelfEntity s
LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
WHERE s.bookcaseId = :bookcaseId
GROUP BY s.shelfId, s.shelfLabel
```

**Key difference:** Can include **calculated fields** (COUNT, SUM, AVG) that don't exist in the entity.

**The record:**
```java
public record ShelfSummary(Long shelfId, String label, long bookCount) {}
```

**Field mapping:**
- `s.shelfId` → `shelfId` parameter
- `s.shelfLabel` → `label` parameter
- `COUNT(b.bookId)` → `bookCount` parameter

**Important rules:**
1. Constructor parameters must match **exactly** (count, order, type)
2. Must use **fully qualified class name** (`com.penrose.bibby.library.shelf.ShelfSummary`)
3. Works with JPQL only, NOT with native SQL

---

## Part 5: Return Types

Spring Data JPA supports many return types. Here's what you use in Bibby:

| Return Type | Example | Behavior |
|-------------|---------|----------|
| `Entity` | `BookEntity findByTitle(String title)` | Single result or `null` |
| `List<Entity>` | `List<BookEntity> findByShelfId(Long id)` | Multiple results or empty list |
| `List<Projection>` | `List<BookSummary> findByShelfId(Long id)` | Multiple projections or empty list |
| `Projection` | `BookDetailView getBookDetailView(Long id)` | Single projection or `null` |

### Return Types You Should Use But Don't (Yet)

| Return Type | Example | Benefit |
|-------------|---------|---------|
| `Optional<Entity>` | `Optional<BookEntity> findByTitle(String title)` | Explicit "might not exist" semantics |
| `Optional<Projection>` | `Optional<BookDetailView> getBookDetailView(Long id)` | Avoids `null` checks |
| `Stream<Entity>` | `Stream<BookEntity> findByGenre(String genre)` | Memory-efficient for large result sets |
| `Page<Entity>` | `Page<BookEntity> findAll(Pageable pageable)` | Pagination support |

**Recommendation:** Change all single-result methods to return `Optional`:

```java
// CURRENT (can return null):
BookEntity findByTitle(String title);

// BETTER (explicit optionality):
Optional<BookEntity> findByTitle(String title);

// Usage:
Optional<BookEntity> book = bookRepository.findByTitle("1984");
book.ifPresent(b -> System.out.println(b.getTitle()));
book.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
```

---

## Part 6: JPQL vs Native SQL

You use both in Bibby. When should you use each?

### JPQL (Java Persistence Query Language)

**Example from your code:**
```java
// ShelfRepository.java:15-27
@Query("""
    SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
        s.shelfId,
        s.shelfLabel,
        COUNT(b.bookId)
    )
    FROM ShelfEntity s
    LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
    WHERE s.bookcaseId = :bookcaseId
    GROUP BY s.shelfId, s.shelfLabel
    ORDER BY s.shelfPosition ASC
    """)
List<ShelfSummary> findShelfSummariesByBookcaseId(@Param("bookcaseId") Long bookcaseId);
```

**Key characteristics:**
- Uses **entity names** (`ShelfEntity`, `BookEntity`)
- Uses **field names** (`shelfId`, `shelfLabel`)
- Works on **any database** (Hibernate translates to database-specific SQL)
- Supports **constructor expressions** (`new ClassName(...)`)
- Type-checked at startup (fails fast if entity/field names are wrong)

**When to use JPQL:**
- Portable queries (might switch databases later)
- Standard aggregations (`COUNT`, `SUM`, `AVG`)
- Constructor expressions for projections
- Entity-based joins

---

### Native SQL

**Example from your code:**
```java
// BookRepository.java:27-39
@Query(value = """
    SELECT b.book_id, b.title,
           STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
           bc.bookcase_label, s.shelf_label, b.book_status
    FROM books b
    JOIN book_authors ba ON b.book_id = ba.book_id
    JOIN authors a ON ba.author_id = a.author_id
    JOIN shelves s ON s.shelf_id = b.shelf_id
    JOIN bookcases bc ON bc.bookcase_id = s.bookcase_id
    WHERE b.book_id = :bookId
    GROUP BY b.book_id, b.title, bc.bookcase_label, s.shelf_label, b.book_status
""", nativeQuery = true)
BookDetailView getBookDetailView(Long bookId);
```

**Key characteristics:**
- Uses **table names** (`books`, `authors`, `book_authors`)
- Uses **column names** (`book_id`, `first_name`, `bookcase_label`)
- **Database-specific** (this query only works on PostgreSQL)
- Can use **database-specific functions** (`STRING_AGG`, `CONCAT`)
- **Cannot use constructor expressions** (use records instead)

**When to use Native SQL:**
- Database-specific functions (`STRING_AGG`, `REGEXP_MATCHES`, window functions)
- Complex joins with join tables
- Performance optimization (database-specific hints)
- Direct control over exact SQL

**Why your `getBookDetailView()` uses native SQL:**
- Uses `STRING_AGG()` (PostgreSQL-specific aggregate function)
- Joins through `book_authors` join table (easier in SQL than JPQL)
- Needs precise control over GROUP BY

---

## Part 7: Real-World Examples From Bibby

### Example 1: Find Books on a Shelf, Sorted by Title

**Your code:**
```java
// BookRepository.java:24
List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);
```

**Use case:** Display books on a shelf in alphabetical order (CLI or REST API).

**Generated SQL:**
```sql
SELECT b.book_id, b.title
FROM books
WHERE b.shelf_id = ?
ORDER BY b.title ASC
```

**Why projection?** CLI doesn't need 13 fields - just ID and title.

**Grade:** A (perfect use of projection + sorting)

---

### Example 2: Find Authors of a Book

**Your code:**
```java
// AuthorRepository.java:15
List<AuthorEntity> findByBooks_BookId(Long bookId);
```

**Use case:** Show all authors when displaying book details.

**Relationship:**
```
BookEntity ←──@ManyToMany──→ AuthorEntity
            (book_authors join table)
```

**Generated SQL:**
```sql
SELECT a.*
FROM authors a
JOIN book_authors ba ON a.author_id = ba.author_id
WHERE ba.book_id = ?
```

**Why nested property navigation?** Spring knows `books` is a `@ManyToMany` relationship and automatically joins the join table.

**Grade:** A (correct use of nested property navigation)

---

### Example 3: Book Details with Authors, Shelf, and Bookcase

**Your code:**
```java
// BookRepository.java:39
BookDetailView getBookDetailView(Long bookId);
```

**Use case:** Display complete book details with related entities (authors, shelf, bookcase).

**Why native SQL?**
1. Joins 4 tables (books, book_authors, authors, shelves, bookcases)
2. Uses `STRING_AGG()` to concatenate multiple authors into a single string
3. Requires `GROUP BY` because of aggregation

**Could you do this with JPQL?** Technically yes, but it would be much more complex:

```java
// JPQL equivalent (much harder to read):
@Query("""
    SELECT new BookDetailView(
        b.bookId,
        b.title,
        (SELECT STRING_AGG(CONCAT(a.firstName, ' ', a.lastName), ', ')
         FROM AuthorEntity a WHERE a.bookId = b.bookId),
        bc.bookcaseLabel,
        s.shelfLabel,
        b.bookStatus
    )
    FROM BookEntity b
    JOIN ShelfEntity s ON b.shelfId = s.shelfId
    JOIN BookcaseEntity bc ON s.bookcaseId = bc.bookcaseId
    WHERE b.bookId = :bookId
    """)
```

**Native SQL is clearer here.**

**Grade:** A (appropriate use of native SQL for complex query)

---

### Example 4: Shelf Summaries with Book Counts

**Your code:**
```java
// ShelfRepository.java:27
List<ShelfSummary> findShelfSummariesByBookcaseId(Long bookcaseId);
```

**Use case:** Display all shelves in a bookcase with the number of books on each shelf.

**Why JPQL with constructor expression?**
1. Needs `COUNT(b.bookId)` aggregation
2. Uses `LEFT JOIN` to include empty shelves
3. Returns custom projection with calculated field (`bookCount`)
4. Portable (works on any database)

**Could you use a method name?** No - method names don't support aggregation.

**Could you use native SQL?** Yes, but JPQL is sufficient (no database-specific features needed).

**Grade:** A (perfect use of JPQL constructor expression)

---

## Part 8: Common Mistakes and How to Fix Them

### Mistake 1: Property Name Mismatch

**Wrong:**
```java
// Trying to find by 'name' field, but BookEntity has 'title':
BookEntity findByName(String name);
```

**Error at startup:**
```
PropertyReferenceException: No property 'name' found for type 'BookEntity'
```

**Fix:** Match the exact field name in the entity:
```java
BookEntity findByTitle(String title); // Matches BookEntity.title
```

---

### Mistake 2: Missing Underscore in Nested Properties

**Wrong:**
```java
// Trying to navigate books → bookId, but missing underscore:
List<AuthorEntity> findByBooksBookId(Long bookId);
```

**What happens:** Spring looks for a field named `booksBookId` (not `books.bookId`).

**Error:**
```
PropertyReferenceException: No property 'booksBookId' found for type 'AuthorEntity'
```

**Fix:** Add underscore for property navigation:
```java
List<AuthorEntity> findByBooks_BookId(Long bookId); // Navigates books.bookId
```

---

### Mistake 3: Aggregation in Method Names

**Wrong:**
```java
// Trying to count books in a method name:
long countBooksByShelfId(Long shelfId);
```

**What happens:** Spring doesn't support `COUNT` in method names (it tries to find a field named `books`).

**Fix:** Use `@Query`:
```java
@Query("SELECT COUNT(b) FROM BookEntity b WHERE b.shelfId = :shelfId")
long countBooksByShelfId(@Param("shelfId") Long shelfId);
```

**Or use built-in count method:**
```java
// Built-in by Spring Data JPA:
long count(); // Count all books
```

---

### Mistake 4: Constructor Expression with Native SQL

**Wrong:**
```java
@Query(value = """
    SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
        s.shelf_id,
        s.shelf_label,
        COUNT(b.book_id)
    )
    FROM shelves s
    LEFT JOIN books b ON b.shelf_id = s.shelf_id
    GROUP BY s.shelf_id, s.shelf_label
    """, nativeQuery = true)
List<ShelfSummary> findShelfSummaries();
```

**Error:**
```
SQLGrammarException: Syntax error near 'new com.penrose.bibby...'
```

**Why it fails:** Native SQL doesn't understand Java constructors.

**Fix 1:** Use JPQL (remove `nativeQuery = true`):
```java
@Query("""
    SELECT new com.penrose.bibby.library.shelf.ShelfSummary(
        s.shelfId,      // Use field names, not column names
        s.shelfLabel,
        COUNT(b.bookId)
    )
    FROM ShelfEntity s  // Use entity names, not table names
    LEFT JOIN BookEntity b ON b.shelfId = s.shelfId
    GROUP BY s.shelfId, s.shelfLabel
    """)
List<ShelfSummary> findShelfSummaries();
```

**Fix 2:** Keep native SQL, use record without constructor expression:
```java
@Query(value = """
    SELECT s.shelf_id, s.shelf_label, COUNT(b.book_id) as bookCount
    FROM shelves s
    LEFT JOIN books b ON b.shelf_id = s.shelf_id
    GROUP BY s.shelf_id, s.shelf_label
    """, nativeQuery = true)
List<ShelfSummary> findShelfSummaries();
```

---

## Part 9: What's Missing in Bibby (Design Issues)

### Issue 1: BookEntity.shelfId Should Be @ManyToOne

**Current code:**
```java
// BookEntity.java:24 (plain field, not a relationship)
private Long shelfId;
```

**Problem:** You can't navigate from Book → Shelf → Bookcase in query methods.

**Blocked queries:**
```java
// ❌ These WON'T work (shelfId is not an object):
List<BookEntity> findByShelf_BookcaseId(Long bookcaseId);
List<BookEntity> findByShelf_BookcaseLabel(String label);
```

**Better design:**
```java
// BookEntity.java:24 (should be a relationship)
@ManyToOne
@JoinColumn(name = "shelf_id")
private ShelfEntity shelf;
```

**Enabled queries:**
```java
// ✅ Now these work (navigating through relationship):
List<BookEntity> findByShelf_BookcaseId(Long bookcaseId);
List<BookEntity> findByShelf_Bookcase_BookcaseLabel(String label);
```

**We'll fix this in Section 26 (Entity Design & JPA).**

---

### Issue 2: Redundant Method Names

**Current code:**
```java
// BookRepository.java:14
BookEntity findBookEntityByTitle(String title);

// BookRepository.java:16
BookEntity findByTitle(String title);
```

**Both do THE EXACT SAME THING.**

**Recommendation:** Delete the verbose one:
```java
// Keep only this:
BookEntity findByTitle(String title);
```

---

### Issue 3: Missing Optional Return Types

**Current code:**
```java
// BookRepository.java:16 (can return null)
BookEntity findByTitle(String title);

// AuthorRepository.java:11 (can return null)
AuthorEntity getByFullName(String fullName);
```

**Problem:** Callers must remember to check for `null`:
```java
BookEntity book = bookRepository.findByTitle("1984");
if (book != null) {  // Easy to forget this check!
    System.out.println(book.getTitle());
}
```

**Better design:**
```java
Optional<BookEntity> findByTitle(String title);
Optional<AuthorEntity> findByFullName(String fullName); // Also rename get → find
```

**Usage:**
```java
Optional<BookEntity> book = bookRepository.findByTitle("1984");
book.ifPresent(b -> System.out.println(b.getTitle()));
// Or:
BookEntity book = bookRepository.findByTitle("1984")
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
```

---

### Issue 4: Inconsistent Verb Usage (get vs find)

**Current code:**
```java
// AuthorRepository.java:11
AuthorEntity getByFullName(String fullName);

// AuthorRepository.java:13
AuthorEntity findByFirstNameAndLastName(String firstName, String lastName);
```

**Problem:** Mixing `get` and `find` is confusing. They do the same thing in Spring Data JPA.

**Recommendation:** Use `find` everywhere for consistency:
```java
Optional<AuthorEntity> findByFullName(String fullName);
Optional<AuthorEntity> findByFirstNameAndLastName(String firstName, String lastName);
```

---

## Your Bibby Repository Methods - Summary

| Repository | Query Methods | Method Name Queries | Custom @Query | Projections | Grade |
|------------|---------------|---------------------|---------------|-------------|-------|
| **BookRepository** | 10 | 6 | 1 native SQL | 2 (BookSummary, BookDetailView) | A- |
| **AuthorRepository** | 3 | 3 | 0 | 0 | B+ |
| **ShelfRepository** | 2 | 1 | 1 JPQL | 1 (ShelfSummary) | A |
| **BookcaseRepository** | 1 | 1 | 0 | 0 | B |
| **CatalogRepository** | 0 | 0 | 0 | 0 | N/A (empty) |
| **TOTAL** | **16** | **11** | **2** | **3** | **A-** |

**Strengths:**
- ✅ Excellent use of projections (reduce data transfer)
- ✅ Proper use of native SQL for complex queries (`STRING_AGG`)
- ✅ Proper use of JPQL for portable queries with aggregation
- ✅ Nested property navigation (`findByBooks_BookId`)
- ✅ Sorting in method names (`OrderByTitleAsc`)

**Areas for Improvement:**
- ⚠️ Redundant method names (`findBookEntityByTitle` vs `findByTitle`)
- ⚠️ Inconsistent verb usage (`get` vs `find`)
- ⚠️ Missing `Optional` return types (null safety)
- ⚠️ Missing `@ManyToOne` relationship (blocks nested queries)

---

## Action Items

### Priority 1: Fix Return Types (5 methods)

**Add `Optional` to single-result methods:**

```java
// BookRepository.java:14-18 (delete one, fix the other)
// DELETE THIS (redundant):
BookEntity findBookEntityByTitle(String title);

// CHANGE THIS:
BookEntity findByTitle(String title);
// TO THIS:
Optional<BookEntity> findByTitle(String title);

// CHANGE THIS:
BookEntity findByTitleIgnoreCase(String title);
// TO THIS:
Optional<BookEntity> findByTitleIgnoreCase(String title);

// AuthorRepository.java:11 (also rename get → find)
// CHANGE THIS:
AuthorEntity getByFullName(String fullName);
// TO THIS:
Optional<AuthorEntity> findByFullName(String fullName);

// CHANGE THIS:
AuthorEntity findByFirstNameAndLastName(String firstName, String lastName);
// TO THIS:
Optional<AuthorEntity> findByFirstNameAndLastName(String firstName, String lastName);

// BookcaseRepository.java:8 (also simplify name)
// CHANGE THIS:
BookcaseEntity findBookcaseEntityByBookcaseLabel(String s);
// TO THIS:
Optional<BookcaseEntity> findByBookcaseLabel(String bookcaseLabel);
```

**Impact:** Better null safety, clearer intent, prevents NullPointerException.

---

### Priority 2: Fix BookEntity.shelfId Relationship

**This is a bigger change (Section 26 will cover it in detail).**

**Current:**
```java
// BookEntity.java:24
private Long shelfId;
```

**Change to:**
```java
@ManyToOne
@JoinColumn(name = "shelf_id")
private ShelfEntity shelf;
```

**Impact:** Enables nested queries like `findByShelf_BookcaseId()`.

**We'll do this in Section 26: Entity Design & JPA.**

---

### Priority 3: Experiment with Query Keywords

**Try these methods in your repositories:**

```java
// BookRepository - find books published in a range:
List<BookEntity> findByPublicationYearBetween(int startYear, int endYear);

// BookRepository - find top 5 most recent books:
List<BookEntity> findTop5ByOrderByCreatedAtDesc();

// BookRepository - find books by multiple genres:
List<BookEntity> findByGenreIn(List<String> genres);

// BookRepository - count books on a shelf:
long countByShelfId(Long shelfId);

// AuthorRepository - check if author exists:
boolean existsByFullName(String fullName);
```

**These all work with ZERO implementation code - just method names!**

---

## Summary: What You Learned

1. **Spring Data JPA generates implementations** from interface method names at runtime (proxy pattern)

2. **Query method naming conventions**:
   - Verbs: `find`, `get`, `query`, `read`, `count`, `delete`
   - Properties: Must match entity field names exactly
   - Keywords: `By`, `And`, `Or`, `IgnoreCase`, `Containing`, `OrderBy`, etc.
   - Modifiers: `IgnoreCase`, `Containing`, `StartingWith`, `LessThan`, etc.

3. **Nested property navigation** with underscore:
   - `findByBooks_BookId()` navigates through `@ManyToMany` relationship
   - Only works if the field is an actual relationship (not a plain foreign key)

4. **@Query for complex queries**:
   - JPQL: Portable, entity-based, supports constructor expressions
   - Native SQL: Database-specific, table-based, supports DB functions

5. **Projections** reduce data transfer:
   - Record projections (your choice): Fast, immutable, Java 17+
   - Interface projections: Flexible, slower
   - Constructor expressions: For calculated fields in JPQL

6. **Return types matter**:
   - `Entity` - Single result or `null`
   - `List<Entity>` - Multiple results or empty list
   - `Optional<Entity>` - Explicit optionality (recommended!)

7. **Bibby has 16 query methods** across 5 repositories:
   - 11 method name queries (generated from names)
   - 2 `@Query` annotations (1 JPQL, 1 native SQL)
   - 3 projections (records)
   - **Grade: A-** (excellent use of Spring Data JPA, minor improvements needed)

---

**Next:** Section 26 - Entity Design & JPA will fix the `BookEntity.shelfId` relationship issue and explore entity annotations, ID generation strategies, equals/hashCode, and relationship mappings.

**Your Bibby repositories are 95% excellent.** The small improvements (Optional return types, consistent naming, @ManyToOne relationships) will make them perfect.

You now understand the "magic" behind Spring Data JPA. Method names are parsed, queries are generated, and proxies execute SQL - all without you writing a single line of implementation code.

---

**Mentor's Note:**

Spring Data JPA is one of the most powerful features of the Spring ecosystem. You've used it well in Bibby - your projections, native SQL, and JPQL queries show good judgment about when to use each approach.

The main issue (BookEntity.shelfId as a plain Long instead of @ManyToOne) is a design decision that limits query expressiveness. We'll fix that in Section 26.

For now, know that **every Spring Data JPA method in Bibby works correctly** - they're just missing Optional return types and could use slightly cleaner naming.

You're doing great. Keep going.

---

*Section 25 complete. 25 of 33 sections finished (76%).*
*Next up: Section 26 - Entity Design & JPA*
