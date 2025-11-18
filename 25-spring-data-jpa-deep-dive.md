# Section 25: Spring Data JPA Deep Dive
## The Magic Behind Repository Interfaces

**Discovery:** You never write SQL - Spring Data generates it for you
**Concept:** Query methods, projections, @Query, property expressions
**Time:** 60 min read

---

## What You'll Learn

By the end of this section, you'll understand:

- **CRITICAL:** How Spring Data creates repository implementations (you write interfaces, Spring writes classes)
- Query method naming conventions (findBy, getBy, countBy, deleteBy)
- **BIBBY ANALYSIS:** 15 query methods across 5 repositories
- Projections for DTOs (BookSummary, ShelfSummary, BookDetailView)
- Custom queries with @Query (JPQL vs native SQL)
- Property path expressions (findByBooks_BookId navigates relationships)
- Return types (Optional, List, single entity, void)
- How Spring Data translates method names to SQL
- When to use query methods vs @Query vs native queries

Every concept is explained using **Bibby's actual repository methods**.

---

## The Problem: Writing Repository Implementations

**Without Spring Data JPA:**

```java
@Repository
public class BookRepositoryImpl {

    @PersistenceContext
    private EntityManager entityManager;

    public BookEntity findByTitle(String title) {
        return entityManager.createQuery(
            "SELECT b FROM BookEntity b WHERE b.title = :title",
            BookEntity.class
        )
        .setParameter("title", title)
        .getSingleResult();
    }

    public List<BookEntity> findByTitleContaining(String title) {
        return entityManager.createQuery(
            "SELECT b FROM BookEntity b WHERE b.title LIKE :title",
            BookEntity.class
        )
        .setParameter("title", "%" + title + "%")
        .getResultList();
    }

    public List<BookEntity> findByShelfId(Long shelfId) {
        return entityManager.createQuery(
            "SELECT b FROM BookEntity b WHERE b.shelfId = :shelfId",
            BookEntity.class
        )
        .setParameter("shelfId", shelfId)
        .getResultList();
    }

    // ... 50 more methods of boilerplate CRUD
}
```

**Problems:**
- 200+ lines of repetitive boilerplate
- Easy to make mistakes in JPQL syntax
- Need to write implementations for all 5 repositories
- Must manually handle Optional wrapping
- Pagination and sorting require more boilerplate

---

## The Solution: Spring Data JPA Repository Magic

**With Spring Data JPA (Bibby's approach):**

```java
// src/main/java/com/penrose/bibby/library/book/BookRepository.java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    BookEntity findByTitle(String title);

    List<BookEntity> findByTitleContaining(String title);

    List<BookEntity> findByShelfId(Long shelfId);
}
```

**That's it.** Spring Data:
1. **Generates the implementation class** at runtime
2. **Parses method names** to create SQL queries
3. **Handles parameter binding** automatically
4. **Wraps results** in Optional/List as needed
5. **Provides 20+ built-in methods** (save, findById, deleteById, findAll, etc.)

---

## How Spring Data JPA Works (The Magic Explained)

### Step 1: You Extend JpaRepository

```java
public interface BookRepository extends JpaRepository<BookEntity, Long> {
    //                                                   ^          ^
    //                                                   Entity    ID Type
}
```

**What you get for free (20+ methods):**

```java
// CREATE
<S extends BookEntity> S save(S entity)
<S extends BookEntity> List<S> saveAll(Iterable<S> entities)

// READ
Optional<BookEntity> findById(Long id)
boolean existsById(Long id)
List<BookEntity> findAll()
List<BookEntity> findAllById(Iterable<Long> ids)
long count()

// UPDATE
<S extends BookEntity> S save(S entity)  // same as CREATE (upsert)

// DELETE
void deleteById(Long id)
void delete(BookEntity entity)
void deleteAll(Iterable<? extends BookEntity> entities)
void deleteAll()

// PAGINATION
Page<BookEntity> findAll(Pageable pageable)
List<BookEntity> findAll(Sort sort)

// BATCH OPERATIONS
<S extends BookEntity> List<S> saveAllAndFlush(Iterable<S> entities)
void flush()
void deleteAllInBatch(Iterable<BookEntity> entities)
```

**Where is this code?**

Spring Data creates a **proxy implementation class** at runtime called:
```
SimpleJpaRepository<BookEntity, Long>
```

You never see the source code. It's generated dynamically and injected into the Spring context as a bean.

---

### Step 2: Spring Scans for Repository Interfaces

```java
// Application.java (Bibby's main class)
@SpringBootApplication  // Includes @EnableJpaRepositories
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**What happens at startup:**

1. **Component Scanning:** Spring finds all `interface` types extending `JpaRepository`
2. **Proxy Creation:** Spring creates a **proxy implementation** for each interface
3. **Bean Registration:** Proxy is registered in ApplicationContext as `bookRepository` bean
4. **Dependency Injection:** Services can `@Autowired` the repository interface

**Log output (if you enable debug logging):**

```
Creating shared instance of singleton bean 'bookRepository'
Finished creation of instance of bean 'bookRepository'
```

**Behind the scenes:**

```java
// Spring creates this dynamically (you never write this):
public class BookRepositoryImpl implements BookRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // All 20+ JpaRepository methods implemented here
    // Plus your custom query methods (parsed from method names)
}
```

---

### Step 3: Spring Parses Query Method Names

**Your method:**

```java
List<BookEntity> findByTitleContaining(String title);
```

**Spring Data parsing:**

```
findByTitleContaining
│    │ │     │
│    │ │     └─ Keyword: "Containing" → SQL LIKE '%value%'
│    │ └─────── Property: "Title" → BookEntity.title field
│    └─────────── Connector: "By" → WHERE clause
└──────────────── Action: "find" → SELECT query
```

**Generated JPQL:**

```sql
SELECT b FROM BookEntity b WHERE b.title LIKE :title
```

**Generated SQL (PostgreSQL):**

```sql
SELECT * FROM books WHERE title LIKE '%Harry Potter%'
```

**Parameters:**
- Spring automatically binds method parameter `title` to `:title` placeholder
- For `Containing` keyword, Spring wraps value with `%`: `"%" + title + "%"`

---

## Bibby's Repository Analysis

**Total Repositories:** 5
**Total Query Methods:** 15 (excluding inherited JpaRepository methods)
**Query Method Breakdown:**

| Repository | Query Methods | Custom @Query | Projections |
|------------|---------------|---------------|-------------|
| BookRepository | 6 | 1 (native SQL) | 2 (BookSummary, BookDetailView) |
| AuthorRepository | 3 | 0 | 0 |
| ShelfRepository | 1 | 1 (JPQL) | 1 (ShelfSummary) |
| BookcaseRepository | ? | ? | ? |
| CatalogRepository | ? | ? | ? |

---

## Query Methods: Deep Dive

### Anatomy of a Query Method

**Pattern:**
```
[Action][Subject]By[Property][Keyword][OrderBy][Property][Direction]
```

**Examples from Bibby:**

```java
// BookRepository.java

// 1. Simple equality
BookEntity findByTitle(String title);
//         ^    ^  ^       ^
//         |    |  |       └─ Parameter name (doesn't matter)
//         |    |  └───────── Property name (MUST match BookEntity field)
//         |    └──────────── "By" separator
//         └───────────────── Action (find, get, query, read, count, delete)

// 2. Case-insensitive
BookEntity findByTitleIgnoreCase(String title);
//                    ^
//                    └─ Keyword modifying comparison

// 3. Partial match
List<BookEntity> findByTitleContaining(String title);
//   ^                        ^
//   |                        └─ LIKE '%value%'
//   └───────────────────────── Returns multiple results

// 4. Foreign key lookup
List<BookEntity> findByShelfId(Long id);
//                       ^
//                       └─ Navigates to shelfId field

// 5. Ordering
List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);
//                                              ^        ^
//                                              |        └─ Sort direction
//                                              └────────── Sort property
```

---

### Query Method Keywords (34 Total)

**Most Common in Bibby:**

| Keyword | SQL Equivalent | Example |
|---------|----------------|---------|
| `findBy` | `SELECT ... WHERE` | `findByTitle("1984")` |
| `And` | `AND` | `findByFirstNameAndLastName("George", "Orwell")` |
| `Or` | `OR` | `findByTitleOrAuthor("1984", "Orwell")` |
| `Containing` | `LIKE '%value%'` | `findByTitleContaining("Harry")` |
| `IgnoreCase` | `LOWER(col) = LOWER(val)` | `findByTitleIgnoreCase("LOTR")` |
| `OrderBy...Asc` | `ORDER BY col ASC` | `findByShelfIdOrderByTitleAsc(5)` |
| `OrderBy...Desc` | `ORDER BY col DESC` | `findByShelfIdOrderByTitleDesc(5)` |

**Less Common (but available):**

| Keyword | SQL Equivalent | Example |
|---------|----------------|---------|
| `StartingWith` | `LIKE 'value%'` | `findByTitleStartingWith("The")` |
| `EndingWith` | `LIKE '%value'` | `findByTitleEndingWith("Ring")` |
| `Between` | `BETWEEN x AND y` | `findByYearBetween(1900, 2000)` |
| `LessThan` | `<` | `findByPageCountLessThan(300)` |
| `GreaterThan` | `>` | `findByPageCountGreaterThan(500)` |
| `IsNull` | `IS NULL` | `findByShelfIdIsNull()` |
| `IsNotNull` | `IS NOT NULL` | `findByShelfIdIsNotNull()` |
| `In` | `IN (...)` | `findByStatusIn(List.of("AVAILABLE", "CHECKED_OUT"))` |
| `NotIn` | `NOT IN (...)` | `findByStatusNotIn(List.of("LOST"))` |
| `True` | `= true` | `findByActiveTrue()` |
| `False` | `= false` | `findByActiveFalse()` |
| `countBy` | `SELECT COUNT(*)` | `countByShelfId(5L)` |
| `deleteBy` | `DELETE FROM` | `deleteByStatus("ARCHIVED")` |
| `existsBy` | `SELECT COUNT(*) > 0` | `existsByTitle("1984")` |

**Full list:** https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html

---

### Property Path Expressions (Navigating Relationships)

**Bibby example:**

```java
// AuthorRepository.java:15
List<AuthorEntity> findByBooks_BookId(Long bookId);
//                         ^     ^
//                         |     └─ Navigate to BookEntity.bookId
//                         └─────── Through AuthorEntity.books collection
```

**What this does:**

```java
@Entity
public class AuthorEntity {

    @ManyToMany
    private Set<BookEntity> books;  // ← Spring navigates this relationship
}
```

**Generated JPQL:**

```sql
SELECT a FROM AuthorEntity a
JOIN a.books b
WHERE b.bookId = :bookId
```

**Generated SQL (PostgreSQL):**

```sql
SELECT a.*
FROM authors a
JOIN book_authors ba ON a.author_id = ba.author_id
JOIN books b ON ba.book_id = b.book_id
WHERE b.book_id = ?
```

**Syntax:**
- Use `_` (underscore) to navigate nested properties
- `books_bookId` = `books.bookId`
- Alternative: `findByBooksBookId` (camelCase, no underscore)

**More complex example (3 levels deep):**

```java
// Hypothetical: Find books by bookcase label
List<BookEntity> findByShelf_Bookcase_BookcaseLabel(String label);
//                       ^      ^         ^
//                       |      |         └─ BookcaseEntity.bookcaseLabel
//                       |      └─────────── ShelfEntity.bookcase
//                       └────────────────── BookEntity.shelf (if you had ShelfEntity relationship)
```

**⚠️ Warning:** Deep property paths (3+ levels) can generate inefficient SQL with multiple JOINs. Consider using `@Query` for complex navigation.

---

## Return Types (What Can Query Methods Return?)

### 1. Single Entity

```java
BookEntity findByTitle(String title);
```

**Behavior:**
- Returns `null` if not found
- Throws `IncorrectResultSizeDataAccessException` if multiple results found

**⚠️ Problem:** Unsafe! Can cause `NullPointerException`.

---

### 2. Optional<Entity> (BEST PRACTICE)

```java
Optional<BookEntity> findByTitle(String title);
```

**Behavior:**
- Returns `Optional.empty()` if not found
- Returns `Optional.of(entity)` if found
- Throws exception if multiple results

**✅ Preferred:** Forces null-safety.

**Usage:**

```java
Optional<BookEntity> book = bookRepository.findByTitle("1984");

// Safe handling
book.ifPresent(b -> System.out.println(b.getTitle()));

// Or throw custom exception
BookEntity b = book.orElseThrow(() ->
    new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found")
);
```

---

### 3. List<Entity>

```java
List<BookEntity> findByShelfId(Long shelfId);
```

**Behavior:**
- Returns empty list `[]` if no results (never `null`)
- Returns all matching results

**Safe:** No null checks needed.

---

### 4. Projections (DTOs)

```java
List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);
```

**Behavior:**
- Spring Data creates projection instances
- Only selects fields needed for projection (more efficient)
- Can return record, interface, or class

---

### 5. Primitive Types

```java
long countByShelfId(Long shelfId);
boolean existsByTitle(String title);
```

**Behavior:**
- `count*` returns `long` (never null, minimum 0)
- `exists*` returns `boolean` (true/false)

---

### 6. void (Delete Methods)

```java
void deleteByStatus(String status);
```

**Behavior:**
- Executes DELETE query
- Returns nothing
- Transactional (will rollback on exception)

---

## Projections: Reducing Data Transfer

### Problem: Fetching Too Much Data

**Inefficient:**

```java
// BookService.java
List<BookEntity> books = bookRepository.findByShelfId(shelfId);

// You only need title, but you fetch EVERYTHING:
// - bookId, title, authors (Set<AuthorEntity>), shelf (ShelfEntity),
//   pageCount, publisher, publishedYear, isbn, genre, status, coverUrl, notes

return books.stream()
    .map(BookEntity::getTitle)  // Only using title!
    .toList();
```

**SQL:**

```sql
-- Fetches ALL columns (wasteful)
SELECT * FROM books WHERE shelf_id = 5

-- Plus N+1 queries for authors (lazy loading)
SELECT * FROM book_authors WHERE book_id = 1
SELECT * FROM authors WHERE author_id = ...
```

---

### Solution: Projections (DTOs)

**Efficient:**

```java
// BookRepository.java:24
List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);
```

**Projection record:**

```java
// BookSummary.java
public record BookSummary(Long bookId, String title) {
}
```

**Generated SQL:**

```sql
-- Only selects needed columns
SELECT b.book_id, b.title
FROM books b
WHERE b.shelf_id = 5
ORDER BY b.title ASC
```

**Benefits:**
- ✅ Less data transferred from database
- ✅ Faster query execution
- ✅ No N+1 query problem (no lazy loading)
- ✅ No entity lifecycle overhead
- ✅ Immutable DTOs (safer than entities)

---

### Projection Types

#### 1. Record-Based Projection (Bibby's Choice) ✅

```java
// BookSummary.java
public record BookSummary(Long bookId, String title) {
}
```

**Advantages:**
- Immutable (thread-safe)
- Compact syntax
- Works with query methods automatically

**Spring Data matching:**
- Matches record component names to entity properties
- `bookId` → `BookEntity.bookId`
- `title` → `BookEntity.title`

---

#### 2. Interface-Based Projection

```java
public interface BookSummary {
    Long getBookId();
    String getTitle();
}
```

**Advantages:**
- Spring creates proxy implementation
- Can add default methods

**Example with derived property:**

```java
public interface BookInfo {
    String getTitle();
    int getPageCount();

    // Derived property (not in database)
    default String getSummary() {
        return title() + " (" + pageCount() + " pages)";
    }
}
```

---

#### 3. Class-Based Projection (Constructor Expression)

**Used in ShelfRepository:**

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

**ShelfSummary record:**

```java
// ShelfSummary.java
public record ShelfSummary(Long shelfId, String label, long bookCount) {
}
```

**Key:** `new com.penrose.bibby.library.shelf.ShelfSummary(...)` constructor call in JPQL

**Use when:**
- You need aggregations (COUNT, SUM, AVG)
- You need JOINs across multiple tables
- Query method names get too complex

---

## Custom Queries with @Query

### When Query Methods Aren't Enough

**Query method limitations:**

❌ Can't handle complex JOINs
❌ Can't do aggregations (SUM, AVG, GROUP BY)
❌ Can't use subqueries
❌ Method names get ridiculously long
❌ Can't use database-specific features

**Solution:** `@Query` annotation

---

### JPQL Queries (Recommended)

**Bibby example:**

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

**JPQL Features:**
- Works with **entity names** (`ShelfEntity`), not table names (`shelves`)
- Works with **entity properties** (`s.shelfId`), not column names (`shelf_id`)
- Database-agnostic (same query works for PostgreSQL, MySQL, Oracle)
- Type-safe (Hibernate validates at startup)
- Supports constructor expressions (`new ShelfSummary(...)`)

**Parameter binding:**
- `@Param("bookcaseId")` matches `:bookcaseId` in query
- Alternative: Use `?1`, `?2` positional parameters (not recommended)

**Text blocks (Java 15+):**
- `"""` for multi-line strings
- Preserves formatting and indentation
- No need to escape `"` quotes
- No need for `\n` line breaks

---

### Native SQL Queries (Use Sparingly)

**Bibby example:**

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

**Native SQL Features:**
- Uses **table names** (`books`), not entity names
- Uses **column names** (`book_id`), not property names
- Database-specific syntax (PostgreSQL `STRING_AGG`, `CONCAT`)
- More powerful (full SQL capabilities)

**Return type:**

```java
// BookDetailView.java
public record BookDetailView(
    Long bookId,
    String title,
    String authors,  // ← Aggregated string from STRING_AGG
    String bookcaseLabel,
    String shelfLabel,
    String bookStatus
) {}
```

**How Spring Data maps results:**

```java
// Column names MUST match record component names (case-insensitive)
book_id       → bookId        ✅
title         → title         ✅
authors       → authors       ✅ (alias in SELECT)
bookcase_label→ bookcaseLabel ✅
shelf_label   → shelfLabel    ✅
book_status   → bookStatus    ✅
```

**⚠️ Drawbacks:**
- Database-specific (won't work on MySQL without changes)
- No type checking at compile time
- Harder to refactor (no IDE support)
- Breaks database portability

**✅ When to use native SQL:**
- Database-specific features (PostgreSQL arrays, JSON functions, full-text search)
- Complex aggregations (STRING_AGG, window functions)
- Performance optimization (query hints, index hints)
- Working with legacy database schemas

---

## Query Method vs @Query vs Native Query

**Decision tree:**

```
Do you need complex JOINs or aggregations?
│
├─ NO → Use query method
│       Example: findByTitle(String title)
│
└─ YES → Do you need database-specific features?
         │
         ├─ NO → Use JPQL @Query
         │       Example: SELECT new DTO(...) FROM Entity e JOIN ...
         │
         └─ YES → Use native SQL @Query
                 Example: SELECT ... STRING_AGG(...) FROM books ...
```

---

## Bibby Repository Breakdown

### BookRepository (Most Complex)

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    // 1. Duplicate methods (same functionality, different names)
    BookEntity findBookEntityByTitle(String title);  // ❌ Redundant
    BookEntity findByTitle(String title);             // ✅ Simpler
    // ⚠️ ISSUE: Both return null (not Optional)

    // 2. Case-insensitive search
    BookEntity findByTitleIgnoreCase(String title);
    // Generates: WHERE LOWER(title) = LOWER(:title)

    // 3. Partial match
    List<BookEntity> findByTitleContaining(String title);
    // Generates: WHERE title LIKE '%value%'

    // 4. Foreign key lookup
    List<BookEntity> findByShelfId(Long id);
    // Generates: WHERE shelf_id = :id

    // 5. Projection with ordering
    List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);
    // Generates: SELECT book_id, title FROM books WHERE shelf_id = :shelfId ORDER BY title ASC

    // 6. Complex native query with aggregation
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
}
```

**Issues:**
- ❌ Duplicate methods (`findBookEntityByTitle` vs `findByTitle`)
- ❌ Methods return `null` instead of `Optional` (unsafe)
- ✅ Good use of projections (BookSummary)
- ✅ Good use of native query for complex aggregation

---

### AuthorRepository (Simple)

```java
@Repository
public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {

    // 1. Get by derived property (computed fullName)
    AuthorEntity getByFullName(String fullName);
    // ⚠️ ISSUE: fullName is derived (firstName + lastName), not stored in DB
    //          This will FAIL at runtime!

    // 2. Composite property search
    AuthorEntity findByFirstNameAndLastName(String firstName, String lastName);
    // Generates: WHERE first_name = :firstName AND last_name = :lastName
    // ✅ Correct approach

    // 3. Property path expression (navigate relationship)
    List<AuthorEntity> findByBooks_BookId(Long bookId);
    // Generates: JOIN authors.books WHERE books.bookId = :bookId
}
```

**Issues:**
- ❌ **CRITICAL BUG:** `getByFullName` won't work (`fullName` is not a column)
- ❌ Methods return `null` instead of `Optional`
- ✅ Good use of composite properties
- ✅ Good use of property path expressions

---

### ShelfRepository (Advanced)

```java
@Repository
public interface ShelfRepository extends JpaRepository<ShelfEntity, Long> {

    // 1. Simple foreign key lookup
    List<ShelfEntity> findByBookcaseId(Long bookcaseId);
    // Generates: WHERE bookcase_id = :bookcaseId

    // 2. JPQL query with aggregation and constructor expression
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
}
```

**Analysis:**
- ✅ Excellent use of JPQL for aggregation
- ✅ Constructor expression for type-safe projections
- ✅ LEFT JOIN to include shelves with no books
- ✅ GROUP BY for correct aggregation
- ✅ `@Param` annotation for clarity

---

## Common Pitfalls in Bibby

### 1. Duplicate Query Methods

**Problem:**

```java
// BookRepository.java
BookEntity findBookEntityByTitle(String title);  // Line 14
BookEntity findByTitle(String title);             // Line 16
```

**Both generate the same SQL:**

```sql
SELECT * FROM books WHERE title = :title
```

**Solution:** Delete `findBookEntityByTitle` (unnecessarily verbose).

---

### 2. Returning null Instead of Optional

**Problem:**

```java
BookEntity findByTitle(String title);  // Returns null if not found
```

**Unsafe usage:**

```java
BookEntity book = bookRepository.findByTitle("Nonexistent");
book.getTitle();  // 💥 NullPointerException
```

**Solution:**

```java
Optional<BookEntity> findByTitle(String title);
```

**Safe usage:**

```java
Optional<BookEntity> book = bookRepository.findByTitle("Nonexistent");
book.ifPresent(b -> System.out.println(b.getTitle()));  // ✅ Safe
```

---

### 3. Querying Derived Properties

**Problem:**

```java
// AuthorRepository.java:11
AuthorEntity getByFullName(String fullName);
```

**What happens at runtime:**

```
QueryCreationException: No property 'fullName' found for type 'AuthorEntity'
```

**Why:** `fullName` is a **derived property** (getter that computes `firstName + " " + lastName`), not a database column.

**Solution:**

```java
// Use actual columns
AuthorEntity findByFirstNameAndLastName(String firstName, String lastName);
```

**Or use @Query:**

```java
@Query("SELECT a FROM AuthorEntity a WHERE CONCAT(a.firstName, ' ', a.lastName) = :fullName")
Optional<AuthorEntity> findByFullName(@Param("fullName") String fullName);
```

---

### 4. N+1 Query Problem (Not Using Projections)

**Problem:**

```java
// Fetch all books
List<BookEntity> books = bookRepository.findByShelfId(shelfId);

// Access authors (lazy-loaded)
for (BookEntity book : books) {
    book.getAuthors().size();  // ← Triggers separate query for EACH book
}
```

**SQL executed:**

```sql
-- 1 query to get books
SELECT * FROM books WHERE shelf_id = 5

-- N queries to get authors (N = number of books)
SELECT * FROM book_authors WHERE book_id = 1
SELECT * FROM authors WHERE author_id IN (...)
SELECT * FROM book_authors WHERE book_id = 2
SELECT * FROM authors WHERE author_id IN (...)
-- ... repeated N times
```

**Solution 1: Use projections (if you don't need authors)**

```java
List<BookSummary> books = bookRepository.findBookSummariesByShelfIdOrderByTitleAsc(shelfId);
// Only 1 query, no lazy loading
```

**Solution 2: Use JOIN FETCH (if you need full entities)**

```java
@Query("SELECT b FROM BookEntity b LEFT JOIN FETCH b.authors WHERE b.shelfId = :shelfId")
List<BookEntity> findByShelfIdWithAuthors(@Param("shelfId") Long shelfId);
```

---

## Best Practices

### ✅ DO

1. **Return `Optional` for single results**
   ```java
   Optional<BookEntity> findByTitle(String title);
   ```

2. **Use projections for read-only queries**
   ```java
   List<BookSummary> findBookSummariesByShelfId(Long shelfId);
   ```

3. **Use JPQL for complex queries (database-agnostic)**
   ```java
   @Query("SELECT b FROM BookEntity b LEFT JOIN FETCH b.authors WHERE b.shelfId = :shelfId")
   List<BookEntity> findByShelfIdWithAuthors(@Param("shelfId") Long shelfId);
   ```

4. **Use native SQL only when necessary**
   - Database-specific features (JSON, arrays, full-text search)
   - Complex aggregations (STRING_AGG, window functions)
   - Performance-critical queries (index hints)

5. **Use `@Param` for readability**
   ```java
   @Query("SELECT b FROM BookEntity b WHERE b.title = :title")
   Optional<BookEntity> findByTitle(@Param("title") String title);
   ```

6. **Prefer query methods for simple queries**
   ```java
   List<BookEntity> findByShelfId(Long shelfId);
   // Simpler than @Query for basic WHERE clauses
   ```

---

### ❌ DON'T

1. **Don't return `null` (use `Optional`)**
   ```java
   BookEntity findByTitle(String title);  // ❌ Can return null
   ```

2. **Don't create duplicate methods**
   ```java
   BookEntity findBookEntityByTitle(String title);  // ❌ Redundant
   BookEntity findByTitle(String title);
   ```

3. **Don't query derived properties**
   ```java
   AuthorEntity getByFullName(String fullName);  // ❌ fullName not in DB
   ```

4. **Don't use native SQL by default**
   - Breaks database portability
   - No compile-time type checking
   - Harder to refactor

5. **Don't fetch full entities when you only need a few fields**
   ```java
   List<BookEntity> books = findAll();  // ❌ Fetches everything
   List<BookSummary> books = findAllProjectedBy();  // ✅ Only bookId + title
   ```

---

## What's Next

**Section 26: Entity Design & JPA**
- Entity annotations (@Entity, @Table, @Column)
- Relationships (@ManyToMany, @ManyToOne, @OneToMany)
- ID generation strategies (@GeneratedValue)
- `equals()` and `hashCode()` (critical for collections)
- Bidirectional relationships (and their pitfalls)
- Lazy vs Eager fetching
- JPA entity lifecycle

**Coming Up:**
- Section 27: DTO Pattern & Layer Boundaries
- Section 28: Validation
- Section 29: Transaction Management
- Section 30: Spring Shell Commands

---

## Summary

**Spring Data JPA Key Concepts:**

1. **Repository interfaces** → Spring creates implementations at runtime
2. **JpaRepository** provides 20+ built-in CRUD methods
3. **Query methods** derive SQL from method names (`findByTitleContaining`)
4. **Projections** reduce data transfer (records, interfaces, classes)
5. **@Query** handles complex queries (JPQL or native SQL)
6. **Property expressions** navigate relationships (`findByBooks_BookId`)
7. **Return types** affect null-safety (prefer `Optional`)

**Bibby Stats:**
- 5 repositories
- 15 custom query methods
- 3 projections (BookSummary, ShelfSummary, BookDetailView)
- 1 native SQL query
- 1 JPQL query with constructor expression

**Critical Issues:**
- ❌ Methods return `null` (should use `Optional`)
- ❌ Duplicate methods (`findBookEntityByTitle`)
- ❌ Querying derived property (`getByFullName`)

**Next Step:** Learn how to properly design entities and manage JPA relationships in Section 26.

---

*Section 25 complete. You now understand how Spring Data JPA generates repository implementations and translates method names to SQL queries.*
