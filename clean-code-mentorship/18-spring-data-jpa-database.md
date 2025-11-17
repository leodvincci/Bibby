# Section 18: Spring Data JPA & Database Best Practices
## Clean Code + Spring Framework Mentorship

**Focus:** Entity design, relationships, repositories, transactions, and database optimization

**Estimated Time:** 4-5 hours to read and understand; 16-20 hours to implement recommendations

---

## Overview

Your JPA implementation shows you understand the basics—entities, repositories, relationships. But there are **critical bugs** and **performance issues** that would cause production problems.

This section will teach you professional database design with Spring Data JPA.

---

## Your Current Database Implementation

### Entity Structure

**BookEntity.java** (137 lines)
```java
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
    private Long shelfId;  // ⚠️ Storing ID instead of relationship
    private Integer checkoutCount;
    private String bookStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    @ManyToMany
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<AuthorEntity> authors = new HashSet<>();

    public void setAuthors(AuthorEntity authors) {  // ⚠️ Misleading name
        this.authors.add(authors);
    }
}
```

**AuthorEntity.java** (78 lines)
```java
@Entity
@Table(name = "authors")
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long authorId;
    private String firstName;
    private String lastName;
    private String fullName;  // ⚠️ Computed value stored in DB

    @ManyToMany(mappedBy = "authors")
    private Set<BookEntity> books = new HashSet<>();

    public AuthorEntity(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = String.format("%s %s", firstName, lastName);
    }
}
```

**BookRepository.java**
```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    BookEntity findBookEntityByTitle(String title);  // ⚠️ Duplicate
    BookEntity findByTitle(String title);            // ⚠️ Duplicate
    BookEntity findByTitleIgnoreCase(String title);  // ⚠️ Similar

    List<BookEntity> findByTitleContaining(String title);
    List<BookEntity> findByShelfId(Long id);
    List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);

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
    BookDetailView getBookDetailView(Long bookId);  // ⚠️ Native SQL
}
```

**BookService.java**
```java
@Service
public class BookService {

    @Transactional
    public void createNewBook(BookRequestDTO bookRequestDTO) {
        String firstName = bookRequestDTO.firstName();
        String lastName = bookRequestDTO.lastName();
        String title = bookRequestDTO.title();
        BookEntity bookEntity = bookRepository.findByTitle(title);  // ⚠️ Bug!
        AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(firstName, lastName);

        if (authorEntity == null) {
            authorEntity = new AuthorEntity(firstName, lastName);
            authorRepository.save(authorEntity);
        }

        if (bookEntity == null) {
            bookEntity = new BookEntity();
            bookEntity.setTitle(title);
        }
        bookEntity.setAuthors(authorEntity);  // ⚠️ Always adds, even if book exists
        bookRepository.save(bookEntity);
    }

    public BookEntity findBookByTitle(String title){
        Optional<BookEntity> bookEntity = Optional.ofNullable(
            bookRepository.findByTitleIgnoreCase(title)
        );
        List<BookEntity> bookEntities = bookRepository.findByTitleContaining(title);
        for(BookEntity b : bookEntities){
            System.out.println(b.getTitle());  // ⚠️ Debug code
        }

        if(bookEntity.isEmpty()){
            return null;  // ⚠️ Should throw exception
        }
        return bookEntity.get();
    }

    public void checkOutBook(BookEntity bookEntity){  // ⚠️ No @Transactional
        if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
            bookEntity.setBookStatus("CHECKED_OUT");
            bookRepository.save(bookEntity);
        }
    }
}
```

---

## Critical Issues Found

### 🚨 SEVERITY: CRITICAL (Production Bugs)

#### 1. **Duplicate Book Creation Bug** ⚠️ DATA CORRUPTION
**Location:** BookService.java:23-41

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    String title = bookRequestDTO.title();
    BookEntity bookEntity = bookRepository.findByTitle(title);
    AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(firstName, lastName);

    if (authorEntity == null) {
        authorEntity = new AuthorEntity(firstName, lastName);
        authorRepository.save(authorEntity);
    }

    if (bookEntity == null) {              // Line 35
        bookEntity = new BookEntity();
        bookEntity.setTitle(title);
    }
    bookEntity.setAuthors(authorEntity);   // Line 39 - ALWAYS EXECUTES!
    bookRepository.save(bookEntity);       // Line 40 - ALWAYS EXECUTES!
}
```

**The Bug:**
1. If book ALREADY exists (line 35 check fails)
2. You still add the author again (line 39)
3. You still save the book again (line 40)

**Result:**
- Same book gets saved multiple times
- Same author gets added to book multiple times
- Database has duplicate data

**Correct Implementation:**
```java
@Transactional
public Book create

NewBook(BookRequestDTO request) {
    // Find or create author
    AuthorEntity author = authorRepository
        .findByFirstNameAndLastName(request.firstName(), request.lastName())
        .orElseGet(() -> {
            AuthorEntity newAuthor = new AuthorEntity(
                request.firstName(),
                request.lastName()
            );
            return authorRepository.save(newAuthor);
        });

    // Check if book already exists
    Optional<BookEntity> existingBook = bookRepository.findByTitleAndAuthor(
        request.title(),
        author
    );

    if (existingBook.isPresent()) {
        throw new DuplicateBookException(
            "Book '" + request.title() + "' by " +
            author.getFullName() + " already exists"
        );
    }

    // Create new book
    BookEntity book = new BookEntity();
    book.setTitle(request.title());
    book.setIsbn(request.isbn());
    book.addAuthor(author);  // Uses proper bidirectional method
    book.setBookStatus(BookStatus.AVAILABLE);
    book.setCheckoutCount(0);

    return bookRepository.save(book);
}
```

---

#### 2. **Missing Relationship Mapping** ⚠️ N+1 QUERY PROBLEM
**Location:** BookEntity.java:24

```java
private Long shelfId;  // ❌ Storing foreign key as primitive
```

**Problems:**
1. You lose JPA relationship benefits
2. Can't navigate: `book.getShelf().getLabel()`
3. Manual joins required
4. No cascade operations
5. No referential integrity enforcement by JPA

**Correct Implementation:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "shelf_id")
private ShelfEntity shelf;

// Getter returns entity, not ID
public ShelfEntity getShelf() {
    return shelf;
}

// If you still need ID for queries:
public Long getShelfId() {
    return shelf != null ? shelf.getShelfId() : null;
}
```

**Why LAZY fetch?**
- EAGER loads shelf every time you load book
- If you load 100 books → 100 additional shelf queries (N+1 problem)
- LAZY only loads when you call `book.getShelf()`

---

#### 3. **Missing Fetch Strategy** ⚠️ N+1 QUERY PROBLEM
**Location:** BookEntity.java:38-44

```java
@ManyToMany  // ❌ Default is LAZY, but not explicit
@JoinTable(...)
private Set<AuthorEntity> authors = new HashSet<>();
```

**The N+1 Problem:**
```java
// Controller loads 100 books
List<BookEntity> books = bookRepository.findAll();

// Later, you iterate and access authors
for (BookEntity book : books) {
    System.out.println(book.getAuthors());  // 💥 1 query PER BOOK
}
// Result: 1 query for books + 100 queries for authors = 101 total queries!
```

**Solution 1: Entity Graph**
```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    @EntityGraph(attributePaths = {"authors"})
    List<BookEntity> findAll();

    @EntityGraph(attributePaths = {"authors", "shelf"})
    Optional<BookEntity> findById(Long id);
}
// Result: 1 query with JOIN
```

**Solution 2: JPQL with JOIN FETCH**
```java
@Query("SELECT b FROM BookEntity b LEFT JOIN FETCH b.authors WHERE b.id = :id")
Optional<BookEntity> findByIdWithAuthors(@Param("id") Long id);

@Query("SELECT DISTINCT b FROM BookEntity b LEFT JOIN FETCH b.authors")
List<BookEntity> findAllWithAuthors();
```

**Solution 3: Projection (when you don't need full entity)**
```java
// Already exists in your code - GOOD!
List<BookSummary> findBookSummariesByShelfIdOrderByTitleAsc(Long shelfId);
```

---

#### 4. **No Optimistic Locking** ⚠️ LOST UPDATE PROBLEM
**Location:** All entities

**The Problem:**
```java
// Thread 1
BookEntity book1 = bookRepository.findById(1L).get();
book1.setCheckoutCount(5);

// Thread 2 (simultaneously)
BookEntity book2 = bookRepository.findById(1L).get();
book2.setCheckoutCount(10);

// Thread 1 saves
bookRepository.save(book1);  // Sets count to 5

// Thread 2 saves
bookRepository.save(book2);  // Overwrites with 10!
// Thread 1's update is LOST
```

**Solution: Add @Version**
```java
@Entity
@Table(name = "books")
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @Version  // ✅ Optimistic locking
    private Long version;

    // Other fields...
}
```

**How it works:**
```java
// Thread 1
BookEntity book1 = bookRepository.findById(1L).get();  // version = 1
book1.setCheckoutCount(5);

// Thread 2
BookEntity book2 = bookRepository.findById(1L).get();  // version = 1
book2.setCheckoutCount(10);

// Thread 1 saves
bookRepository.save(book1);  // version becomes 2 ✅

// Thread 2 tries to save
bookRepository.save(book2);  // 💥 OptimisticLockException!
// Version mismatch: expected 1, but DB has 2
```

**Handle the exception:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
        OptimisticLockException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                409,
                "The resource was modified by another user. Please refresh and try again.",
                null,
                Instant.now()
            ));
    }
}
```

---

#### 5. **Missing Transaction Annotations** ⚠️ DATA INCONSISTENCY
**Location:** BookService.java:56-62

```java
public void checkOutBook(BookEntity bookEntity){  // ❌ No @Transactional
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**Problem:**
Without `@Transactional`, each repository call is its own transaction. If multiple updates are needed, partial failures can occur.

**Correct:**
```java
@Transactional
public void checkOutBook(Long bookId) {
    BookEntity book = bookRepository.findById(bookId)
        .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

    if (book.getBookStatus() == BookStatus.CHECKED_OUT) {
        throw new BusinessException("Book is already checked out");
    }

    book.setBookStatus(BookStatus.CHECKED_OUT);
    book.setCheckoutCount(book.getCheckoutCount() + 1);
    // No explicit save needed - managed entity auto-saves at transaction end
}
```

**Transaction Best Practices:**
- `@Transactional` on **service layer methods**, not repository
- Read-only transactions for queries:
  ```java
  @Transactional(readOnly = true)
  public List<Book> findAllBooks() {
      return bookRepository.findAll();
  }
  ```
- Custom timeout for long operations:
  ```java
  @Transactional(timeout = 30)  // 30 seconds
  public void importBooksFromFile(File file) {
      // ...
  }
  ```

---

### 🔶 SEVERITY: HIGH (Performance Issues)

#### 6. **GenerationType.AUTO** ⚠️ SUBOPTIMAL PERFORMANCE
**Location:** All entities

```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long bookId;
```

**Problem:**
- `AUTO` lets Hibernate choose strategy
- Often picks `TABLE` strategy (separate sequence table)
- Slower than database-native sequences

**Correct:**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)  // For PostgreSQL, MySQL
private Long bookId;
```

Or with custom sequence:
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
@SequenceGenerator(name = "book_seq", sequenceName = "books_book_id_seq", allocationSize = 1)
private Long bookId;
```

**Performance Impact:**
- `IDENTITY`: Single INSERT statement
- `SEQUENCE`: Single INSERT + one sequence call (can be batched)
- `TABLE`: Locks sequence table (slow under concurrency)

---

#### 7. **Stored Computed Values** ⚠️ DENORMALIZATION WITHOUT REASON
**Location:** AuthorEntity.java:17

```java
private String fullName;  // ❌ Computed from firstName + lastName

public AuthorEntity(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.fullName = String.format("%s %s", firstName, lastName);
}
```

**Problems:**
1. Violates DRY (data duplication)
2. Can become inconsistent if firstName/lastName change
3. Wastes database space
4. Adds extra field to UPDATE statements

**Solution 1: Computed Property (Recommended)**
```java
@Entity
@Table(name = "authors")
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long authorId;

    private String firstName;
    private String lastName;

    // No fullName field!

    @Transient  // Not stored in DB
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

**Solution 2: Database-Generated Column (if needed for performance)**
```java
@Formula("CONCAT(first_name, ' ', last_name)")
private String fullName;
```

---

#### 8. **Missing Indexes** ⚠️ SLOW QUERIES
**Location:** All entities

**Current State:**
Only IDs are indexed (primary keys). No indexes on frequently queried fields.

**What Happens:**
```sql
-- Your repository method:
findByTitle("Clean Code")

-- Actual SQL:
SELECT * FROM books WHERE title = 'Clean Code';
-- 💥 Full table scan of 100,000 books!
```

**Solution: Add Indexes**
```java
@Entity
@Table(
    name = "books",
    indexes = {
        @Index(name = "idx_book_title", columnList = "title"),
        @Index(name = "idx_book_isbn", columnList = "isbn", unique = true),
        @Index(name = "idx_book_status", columnList = "book_status"),
        @Index(name = "idx_book_shelf", columnList = "shelf_id")
    }
)
public class BookEntity {
    // fields...
}
```

**Author indexes:**
```java
@Entity
@Table(
    name = "authors",
    indexes = {
        @Index(name = "idx_author_name", columnList = "first_name, last_name"),
        @Index(name = "idx_author_last_name", columnList = "last_name")
    }
)
public class AuthorEntity {
    // fields...
}
```

**When to Add Indexes:**
- Foreign keys
- Fields in WHERE clauses
- Fields in ORDER BY
- Fields in JOIN conditions
- Unique constraints (like ISBN)

**When NOT to Index:**
- Rarely queried fields
- Small tables (< 1000 rows)
- Fields that change frequently (indexes slow down writes)

---

#### 9. **Missing Constraints** ⚠️ DATA INTEGRITY
**Location:** All entities

**Current:**
```java
private String isbn;  // No constraints!
```

**Problems:**
- ISBN can be null when it shouldn't be
- ISBN can be 10,000 characters
- ISBN can be duplicate
- publicationYear can be -1 or 3000

**Solution: Add Column Constraints**
```java
@Entity
@Table(name = "books")
public class BookEntity {

    @Column(nullable = false, length = 500)
    private String title;

    @Column(unique = true, length = 17)  // ISBN-13 format
    private String isbn;

    @Column(length = 200)
    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;  // Can be null for unpublished

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private Genre genre;

    @Column(name = "book_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BookStatus status = BookStatus.AVAILABLE;

    @Column(name = "checkout_count", nullable = false)
    private Integer checkoutCount = 0;

    @Column(columnDefinition = "TEXT")
    private String description;  // Can be long
}
```

**Enum Mapping:**
```java
@Enumerated(EnumType.STRING)  // ✅ Stores "AVAILABLE", "CHECKED_OUT"
private BookStatus status;

// vs

@Enumerated(EnumType.ORDINAL)  // ❌ Stores 0, 1, 2 (breaks if you reorder enum)
private BookStatus status;
```

---

#### 10. **Manual Timestamp Management** ⚠️ REINVENTING THE WHEEL
**Location:** BookEntity.java:27-28

```java
private LocalDate createdAt;
private LocalDate updatedAt;
```

**Problems:**
1. You must manually set these in service layer
2. Easy to forget
3. Not automatically updated
4. Uses LocalDate (should be LocalDateTime for precision)

**Professional Solution: JPA Auditing**

**Step 1: Enable Auditing**
```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

**Step 2: Create Base Entity**
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Optional: Track who created/modified
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    // Getters
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
```

**Step 3: Extend Base Entity**
```java
@Entity
@Table(name = "books")
public class BookEntity extends AuditableEntity {
    // No more createdAt/updatedAt fields!
    // They're inherited from AuditableEntity

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    private String title;
    // ...
}
```

**Bonus: Track Current User**
```java
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        // In real app, get from SecurityContextHolder
        // For now, return system user
        return Optional.of("SYSTEM");
    }
}

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaConfig {
}
```

Now Spring automatically populates:
- `createdAt` on first save
- `updatedAt` on every save
- `createdBy` / `updatedBy` from `AuditorAware`

---

### 🔷 SEVERITY: MEDIUM (Code Quality Issues)

#### 11. **Duplicate Repository Methods**
**Location:** BookRepository.java:14-18

```java
BookEntity findBookEntityByTitle(String title);
BookEntity findByTitle(String title);
BookEntity findByTitleIgnoreCase(String title);
```

**Problems:**
- Three methods that do almost the same thing
- Confusing for developers
- Which one should you use?

**Solution: Keep One Clear Method**
```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    // Remove: findBookEntityByTitle (redundant prefix)
    // Remove: findByTitle (case-sensitive, rarely needed)
    // Keep:
    Optional<BookEntity> findByTitleIgnoreCase(String title);

    // For partial matches:
    Page<BookEntity> findByTitleContainingIgnoreCase(
        String title,
        Pageable pageable
    );

    // For exact match with author:
    @Query("""
        SELECT b FROM BookEntity b
        JOIN b.authors a
        WHERE LOWER(b.title) = LOWER(:title)
        AND LOWER(a.firstName) = LOWER(:firstName)
        AND LOWER(a.lastName) = LOWER(:lastName)
    """)
    Optional<BookEntity> findByTitleAndAuthor(
        @Param("title") String title,
        @Param("firstName") String firstName,
        @Param("lastName") String lastName
    );
}
```

---

#### 12. **Native SQL Query** ⚠️ PORTABILITY ISSUE
**Location:** BookRepository.java:27-39

```java
@Query(value = """
    SELECT b.book_id, b.title,
           STRING_AGG(CONCAT(a.first_name, ' ', a.last_name), ', ') as authors,
           bc.bookcase_label, s.shelf_label, b.book_status
    FROM books b
    JOIN book_authors ba ON b.book_id = ba.book_id
    ...
""", nativeQuery = true)
BookDetailView getBookDetailView(Long bookId);
```

**Problems:**
1. **PostgreSQL-specific:** `STRING_AGG` doesn't exist in MySQL
2. **Breaks portability:** Can't switch databases
3. **No type safety:** Column names are strings
4. **Harder to test:** Requires real database

**When Native SQL is OK:**
- Database-specific functions (full-text search, JSON queries)
- Complex aggregations not supported by JPQL
- Performance-critical queries

**Better: JPQL with Projection**
```java
// Create projection interface
public interface BookDetailProjection {
    Long getBookId();
    String getTitle();
    String getBookStatus();
    ShelfInfo getShelf();

    interface ShelfInfo {
        String getShelfLabel();
        BookcaseInfo getBookcase();
    }

    interface BookcaseInfo {
        String getBookcaseLabel();
    }
}

// JPQL query (database-agnostic)
@Query("""
    SELECT b.bookId as bookId,
           b.title as title,
           b.status as bookStatus,
           b.shelf as shelf
    FROM BookEntity b
    LEFT JOIN FETCH b.shelf s
    LEFT JOIN FETCH s.bookcase
    WHERE b.bookId = :bookId
""")
Optional<BookDetailProjection> findBookDetailById(@Param("bookId") Long bookId);
```

---

#### 13. **Misleading Method Name**
**Location:** BookEntity.java:54-56

```java
public void setAuthors(AuthorEntity authors) {  // ❌ Singular parameter name
    this.authors.add(authors);  // Actually ADDS, doesn't SET
}
```

**Problem:**
Method named `setAuthors` but actually adds to the collection.

**Solution: Proper Bidirectional Helpers**
```java
// In BookEntity
public void addAuthor(AuthorEntity author) {
    authors.add(author);
    author.getBooks().add(this);  // Maintain both sides
}

public void removeAuthor(AuthorEntity author) {
    authors.remove(author);
    author.getBooks().remove(this);  // Maintain both sides
}

// In AuthorEntity
public void addBook(BookEntity book) {
    books.add(book);
    book.getAuthors().add(this);
}

public void removeBook(BookEntity book) {
    books.remove(book);
    book.getAuthors().remove(this);
}
```

**Why maintain both sides?**
```java
// Without bidirectional maintenance:
book.getAuthors().add(author);  // Added to book
author.getBooks();  // ❌ Doesn't contain book! Inconsistent!

// With proper helper:
book.addAuthor(author);  // Adds to both sides
author.getBooks();  // ✅ Contains book
```

---

## Professional Entity Design

Here's how your entities should look:

**BookEntity.java (Refactored)**
```java
package com.penrose.bibby.library.book;

import com.penrose.bibby.library.author.AuthorEntity;
import com.penrose.bibby.library.shelf.ShelfEntity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "books",
    indexes = {
        @Index(name = "idx_book_title", columnList = "title"),
        @Index(name = "idx_book_isbn", columnList = "isbn"),
        @Index(name = "idx_book_status", columnList = "book_status")
    }
)
public class BookEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(unique = true, length = 17)
    private String isbn;

    @Column(length = 200)
    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private Genre genre;

    @Column(nullable = false)
    private Integer edition = 1;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private ShelfEntity shelf;

    @Column(name = "checkout_count", nullable = false)
    private Integer checkoutCount = 0;

    @Column(name = "book_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BookStatus status = BookStatus.AVAILABLE;

    @Version
    private Long version;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<AuthorEntity> authors = new HashSet<>();

    // Constructors
    public BookEntity() {
    }

    public BookEntity(String title, String isbn) {
        this.title = title;
        this.isbn = isbn;
    }

    // Bidirectional helper methods
    public void addAuthor(AuthorEntity author) {
        authors.add(author);
        author.getBooks().add(this);
    }

    public void removeAuthor(AuthorEntity author) {
        authors.remove(author);
        author.getBooks().remove(this);
    }

    public void assignToShelf(ShelfEntity shelf) {
        this.shelf = shelf;
        shelf.getBooks().add(this);
    }

    // Business logic
    public void checkOut() {
        if (this.status == BookStatus.CHECKED_OUT) {
            throw new BusinessException("Book is already checked out");
        }
        this.status = BookStatus.CHECKED_OUT;
        this.checkoutCount++;
    }

    public void checkIn() {
        if (this.status != BookStatus.CHECKED_OUT) {
            throw new BusinessException("Book is not checked out");
        }
        this.status = BookStatus.AVAILABLE;
    }

    // Getters and setters (generated by IDE)
    public Long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // ... other getters/setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookEntity)) return false;
        BookEntity that = (BookEntity) o;
        return bookId != null && bookId.equals(that.bookId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

**Key Improvements:**
1. ✅ Extends `AuditableEntity` (timestamps)
2. ✅ Proper `@Column` annotations with constraints
3. ✅ `@Version` for optimistic locking
4. ✅ Indexes on frequently queried fields
5. ✅ `@ManyToOne` for shelf (not Long shelfId)
6. ✅ Explicit `LAZY` fetch
7. ✅ Bidirectional helper methods
8. ✅ Business logic in entity (`checkOut()`, `checkIn()`)
9. ✅ Proper `equals()` and `hashCode()`
10. ✅ Default values for status and checkoutCount

---

## Repository Best Practices

### Query Method Naming

Spring Data JPA generates queries from method names:

```java
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    // Find by single field
    Optional<BookEntity> findByIsbn(String isbn);

    // Case-insensitive
    Optional<BookEntity> findByTitleIgnoreCase(String title);

    // Multiple conditions (AND)
    List<BookEntity> findByGenreAndStatus(Genre genre, BookStatus status);

    // OR condition
    List<BookEntity> findByGenreOrPublisher(Genre genre, String publisher);

    // Partial match
    List<BookEntity> findByTitleContainingIgnoreCase(String titleFragment);

    // Ordering
    List<BookEntity> findByGenreOrderByTitleAsc(Genre genre);

    // Pagination
    Page<BookEntity> findByStatus(BookStatus status, Pageable pageable);

    // Top N results
    List<BookEntity> findTop10ByStatusOrderByCheckoutCountDesc(BookStatus status);

    // Count
    long countByStatus(BookStatus status);

    // Exists
    boolean existsByIsbn(String isbn);

    // Delete
    void deleteByStatus(BookStatus status);  // Be careful with this!
}
```

### Custom JPQL Queries

When method names get too complex:

```java
@Query("""
    SELECT b FROM BookEntity b
    JOIN b.authors a
    WHERE LOWER(a.lastName) = LOWER(:lastName)
    AND b.status = :status
    ORDER BY b.title
""")
List<BookEntity> findByAuthorLastNameAndStatus(
    @Param("lastName") String lastName,
    @Param("status") BookStatus status
);

// With pagination
@Query("""
    SELECT b FROM BookEntity b
    WHERE b.publicationYear BETWEEN :startYear AND :endYear
    AND b.genre = :genre
""")
Page<BookEntity> findByYearRangeAndGenre(
    @Param("startYear") int startYear,
    @Param("endYear") int endYear,
    @Param("genre") Genre genre,
    Pageable pageable
);
```

### Projections for Performance

```java
// Interface projection
public interface BookSummary {
    Long getBookId();
    String getTitle();
    String getIsbn();
}

List<BookSummary> findByStatus(BookStatus status);

// Class projection (DTO)
@Query("""
    SELECT new com.penrose.bibby.dto.BookDTO(
        b.bookId, b.title, b.isbn, b.status
    )
    FROM BookEntity b
    WHERE b.shelf.shelfId = :shelfId
""")
List<BookDTO> findBookDTOsByShelfId(@Param("shelfId") Long shelfId);
```

---

## Transaction Management

### When to Use @Transactional

**Rule of Thumb:**
- `@Transactional` on **service methods**, not repositories
- Read-only for queries
- Default (read-write) for updates

```java
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    // Read-only transaction (optimization)
    @Transactional(readOnly = true)
    public List<Book> findAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    // Write transaction
    @Transactional
    public Book createBook(BookRequestDTO request) {
        // Multiple DB operations in one transaction
        AuthorEntity author = findOrCreateAuthor(request);
        BookEntity book = new BookEntity(request.title(), request.isbn());
        book.addAuthor(author);
        return bookRepository.save(book);
    }

    @Transactional
    public void checkOutBook(Long bookId) {
        BookEntity book = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        book.checkOut();  // Changes status, increments count
        // No explicit save needed - dirty checking saves at transaction end
    }

    // Custom transaction settings
    @Transactional(
        timeout = 30,  // 30 seconds max
        rollbackFor = Exception.class  // Rollback on any exception
    )
    public void importBooksFromCsv(File file) {
        // Long-running operation
    }
}
```

### Dirty Checking (Automatic Save)

Inside a transaction, JPA tracks entity changes:

```java
@Transactional
public void updateBook(Long bookId, String newTitle) {
    BookEntity book = bookRepository.findById(bookId).get();
    book.setTitle(newTitle);
    // No save() needed! JPA detects change and updates DB at transaction end
}
```

### Propagation

```java
@Transactional(propagation = Propagation.REQUIRED)  // Default: join existing or create new
public void method1() { }

@Transactional(propagation = Propagation.REQUIRES_NEW)  // Always create new transaction
public void method2() { }

@Transactional(propagation = Propagation.MANDATORY)  // Must be called within transaction
public void method3() { }
```

---

## Database Migration

### Why You Need It

Currently, JPA auto-generates your schema:
```properties
spring.jpa.hibernate.ddl-auto=update  # ⚠️ Dangerous in production
```

**Problems:**
1. No version control of schema changes
2. Can't rollback changes
3. No audit trail
4. Different schemas in dev/prod (if they diverge)

### Solution: Flyway or Liquibase

**Add Flyway:**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**Configure:**
```properties
spring.jpa.hibernate.ddl-auto=validate  # Don't auto-create, just validate
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

**Create migration:**
`src/main/resources/db/migration/V1__create_books_table.sql`
```sql
CREATE TABLE books (
    book_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    isbn VARCHAR(17) UNIQUE,
    publisher VARCHAR(200),
    publication_year INTEGER,
    genre VARCHAR(50),
    edition INTEGER NOT NULL DEFAULT 1,
    description TEXT,
    shelf_id BIGINT,
    checkout_count INTEGER NOT NULL DEFAULT 0,
    book_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_book_title ON books(title);
CREATE INDEX idx_book_isbn ON books(isbn);
CREATE INDEX idx_book_status ON books(book_status);
```

`V2__create_authors_table.sql`
```sql
CREATE TABLE authors (
    author_id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_author_name ON authors(first_name, last_name);

CREATE TABLE book_authors (
    book_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES authors(author_id) ON DELETE CASCADE
);
```

**Benefits:**
- ✅ Version-controlled schema
- ✅ Repeatable deployments
- ✅ Rollback capability
- ✅ Same schema across all environments
- ✅ Audit trail of all changes

---

## Action Items

### 🚨 Critical (Do First - 6-8 hours)

1. **Fix Duplicate Book Bug**
   - [ ] Refactor `createNewBook()` to check for existing book before saving
   - [ ] Add unique constraint on (title + author) combination
   - [ ] Add `DuplicateBookException`
   - **File:** BookService.java:23-41

2. **Add Optimistic Locking**
   - [ ] Add `@Version` field to all entities
   - [ ] Add `OptimisticLockException` handler in `GlobalExceptionHandler`
   - **Files:** BookEntity.java, AuthorEntity.java, ShelfEntity.java, BookcaseEntity.java

3. **Fix Relationship Mapping**
   - [ ] Change `shelfId` to `@ManyToOne ShelfEntity shelf`
   - [ ] Update queries that use `shelfId`
   - **File:** BookEntity.java:24

4. **Add Missing @Transactional**
   - [ ] Add `@Transactional` to `checkOutBook`, `checkInBook`, `updateBook`
   - [ ] Add `@Transactional(readOnly = true)` to all read methods
   - **File:** BookService.java

### 🔶 High Priority (This Week - 6-8 hours)

5. **Implement JPA Auditing**
   - [ ] Create `AuditableEntity` base class
   - [ ] Enable `@EnableJpaAuditing`
   - [ ] Make entities extend `AuditableEntity`
   - [ ] Remove manual `createdAt`/`updatedAt` management

6. **Add Column Constraints**
   - [ ] Add `@Column` annotations with `nullable`, `length`, `unique`
   - [ ] Change enums from String fields to `@Enumerated(EnumType.STRING)`
   - [ ] Add validation annotations
   - **Files:** All entity classes

7. **Add Database Indexes**
   - [ ] Add `@Index` to `@Table` annotations
   - [ ] Index: book title, ISBN, status
   - [ ] Index: author firstName + lastName
   - [ ] Index: shelf bookcaseId

8. **Fix Repository Methods**
   - [ ] Remove duplicate methods (`findBookEntityByTitle`, `findByTitle`)
   - [ ] Keep `findByTitleIgnoreCase`
   - [ ] Add `Optional<>` return types
   - **File:** BookRepository.java

### 🔷 Medium Priority (This Month - 6-8 hours)

9. **Add Bidirectional Helpers**
   - [ ] Create `addAuthor()` / `removeAuthor()` in BookEntity
   - [ ] Create `addBook()` / `removeBook()` in AuthorEntity
   - [ ] Remove misleading `setAuthors(AuthorEntity)` method

10. **Replace Native Query with JPQL**
    - [ ] Convert `getBookDetailView` native query to JPQL
    - [ ] Create projection interface
    - [ ] Test with PostgreSQL and H2
    - **File:** BookRepository.java:27-39

11. **Setup Database Migrations**
    - [ ] Add Flyway dependency
    - [ ] Create `V1__` migration for existing schema
    - [ ] Change `ddl-auto` to `validate`
    - [ ] Create migrations for future schema changes

12. **Remove Computed Field Storage**
    - [ ] Remove `fullName` from AuthorEntity
    - [ ] Add `@Transient getFullName()` method
    - [ ] Update queries that use `fullName`
    - **File:** AuthorEntity.java:17

### ⚪ Low Priority (Nice to Have)

13. **Advanced Features**
    - [ ] Add soft delete support (`@Where(clause = "deleted = false")`)
    - [ ] Add full-text search for book titles
    - [ ] Add multi-tenancy support (if needed)
    - [ ] Add query result caching

---

## Testing JPA Code

### Repository Tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    void findByIsbn_whenExists_returnsBook() {
        // Given
        AuthorEntity author = new AuthorEntity("Robert", "Martin");
        authorRepository.save(author);

        BookEntity book = new BookEntity("Clean Code", "978-0132350884");
        book.addAuthor(author);
        bookRepository.save(book);

        // When
        Optional<BookEntity> found = bookRepository.findByIsbn("978-0132350884");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void findByTitleContaining_returnsMatchingBooks() {
        // Given
        createBook("Clean Code");
        createBook("Clean Architecture");
        createBook("Dirty Code");

        // When
        List<BookEntity> results = bookRepository.findByTitleContainingIgnoreCase("clean");

        // Then
        assertThat(results).hasSize(2);
    }
}
```

### Service Tests

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void createNewBook_whenAuthorExists_reusesAuthor() {
        // Given
        BookRequestDTO request = new BookRequestDTO(
            "Clean Code",
            "Robert",
            "Martin"
        );

        AuthorEntity existingAuthor = new AuthorEntity("Robert", "Martin");
        when(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .thenReturn(Optional.of(existingAuthor));

        // When
        bookService.createNewBook(request);

        // Then
        verify(authorRepository, never()).save(any());  // Didn't create new author
        verify(bookRepository).save(argThat(book ->
            book.getAuthors().contains(existingAuthor)
        ));
    }
}
```

---

## Summary

### Your Current State
- ❌ Duplicate book creation bug
- ❌ Missing optimistic locking
- ❌ N+1 query problems
- ❌ No database constraints
- ❌ Manual timestamp management
- ❌ Native SQL queries
- ❌ No migration strategy

### After This Section
- ✅ Proper entity relationships
- ✅ Optimistic locking with `@Version`
- ✅ Efficient queries with fetch strategies
- ✅ Column constraints and indexes
- ✅ Automatic auditing
- ✅ JPQL queries for portability
- ✅ Flyway migrations

---

## Resources

### Official Docs
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [JPA 2.2 Specification](https://download.oracle.com/otn-pub/jcp/persistence-2_2-mrel-spec/JavaPersistence.pdf)
- [Hibernate User Guide](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html)

### Must-Read Articles
- [Vlad Mihalcea's Blog](https://vladmihalcea.com/) - JPA/Hibernate expert
- [Baeldung Spring Data JPA](https://www.baeldung.com/spring-data-jpa-tutorial)

### Books
- **"High-Performance Java Persistence"** by Vlad Mihalcea (MUST READ)
- **"Pro JPA 2"** by Mike Keith

---

## Mentor's Note

Leo, your JPA code has a **critical bug** in `createNewBook()` that creates duplicate books. This needs to be fixed immediately before you add more books to your database.

Beyond that bug, your biggest issues are:
1. Performance (N+1 queries, missing indexes)
2. Data integrity (no constraints, no locking)
3. Maintainability (no migrations, manual timestamps)

**The good news:** You're using Spring Data JPA correctly for basic operations. You just need to level up to production-grade usage.

**Action plan:**
1. Fix the duplicate bug TODAY (30 minutes)
2. Add `@Version` to all entities (1 hour)
3. Fix relationship mappings (2 hours)
4. Everything else over the next 2 weeks

After implementing these recommendations, your database layer will be solid enough for production use.

---

**Next Section:** Spring Boot + Spring Shell Architecture

**Last Updated:** 2025-11-17
**Status:** Complete ✅
