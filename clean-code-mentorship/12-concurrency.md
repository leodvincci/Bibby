# Section 12: Concurrency
**Clean Code Principle:** *"Concurrency is hard. Keep concurrent code simple, isolated, and well-tested."*

---

## 📚 Principles Overview

### What is Concurrency?

**Concurrency** is when multiple things happen at the same time. In software, this means multiple threads or processes executing simultaneously.

Uncle Bob says: **"Concurrency is a decoupling strategy. It helps us separate *what* gets done from *when* it gets done."**

But concurrency is also **dangerous**:
- Race conditions (two threads modifying the same data)
- Deadlocks (threads waiting for each other forever)
- Memory visibility issues (one thread's changes not visible to another)
- Non-deterministic bugs (works 99% of the time, fails randomly)

### Concurrency vs. Parallelism

**Concurrency:** Dealing with multiple things at once (structure)
**Parallelism:** Doing multiple things at once (execution)

```
# Concurrency (Single-Core CPU)
Thread 1: |--Task A--|  |--Task A--|
Thread 2:    |--Task B--|  |--Task B--|
Time:     0----1----2----3----4----5

# Parallelism (Multi-Core CPU)
Core 1:   |--Task A--------------|
Core 2:   |--Task B--------------|
Time:     0----1----2----3----4----5
```

---

## 🚫 Your Bibby Application: Concurrency is (Mostly) Not Relevant

**Good news:** Your Bibby CLI application is **single-threaded**. Spring Shell processes one command at a time. You don't have concurrency issues.

**When concurrency DOES matter for you:**
1. When you add a web API (multiple users = multiple threads)
2. Background tasks (`@Async` methods)
3. Scheduled jobs (`@Scheduled` methods)
4. Message queue consumers
5. Database transactions (even in single-threaded apps!)

**Current status:**
- ✅ No `@Async` methods
- ✅ No thread pools or executors
- ✅ No `synchronized` blocks
- ✅ Spring Shell is single-threaded
- ⚠️ **But:** You DO use `@Transactional` (database concurrency)

---

## 🔒 Transactions: Concurrency with the Database

Even single-threaded applications deal with concurrency: **database transactions**.

### What is a Transaction?

A **transaction** is a group of database operations that must all succeed or all fail together.

**ACID Properties:**
- **Atomicity:** All or nothing (no partial updates)
- **Consistency:** Database stays in a valid state
- **Isolation:** Concurrent transactions don't interfere
- **Durability:** Committed changes survive crashes

### Your @Transactional Usage

**Location:** `BookService.java:22-41`

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
        authorRepository.save(authorEntity);  // Database write #1
    }

    if (bookEntity == null) {
        bookEntity = new BookEntity();
        bookEntity.setTitle(title);
    }
    bookEntity.setAuthors(authorEntity);
    bookRepository.save(bookEntity);  // Database write #2
}
```

**Why @Transactional is needed:**
If `authorRepository.save()` succeeds but `bookRepository.save()` fails, you'd have an orphaned author in the database. The `@Transactional` annotation ensures both saves succeed or both are rolled back.

**This is GOOD!** You're using transactions where they matter.

---

## 🔍 Your Code Analysis

### Issue #1: Missing @Transactional on checkOutBook() (🟡 MEDIUM)

**Location:** `BookService.java:56-62`

```java
// ❌ NO @Transactional!
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);  // Database write without transaction!
    }
}
```

**Why this is a problem:**

Imagine two users try to check out the same book simultaneously (won't happen in CLI, but WILL happen if you add a web API):

```
Time  Thread 1                          Thread 2
0     Read book (status: AVAILABLE)
1                                       Read book (status: AVAILABLE)
2     Check if not CHECKED_OUT (true)
3                                       Check if not CHECKED_OUT (true)
4     Set status = CHECKED_OUT
5                                       Set status = CHECKED_OUT
6     Save to DB
7                                       Save to DB

Result: Both users think they checked out the book!
```

**Without `@Transactional`:**
- No database-level locking
- Race condition possible (in multi-threaded scenarios)
- Could lead to double-checkout

**Better:**
```java
@Transactional
public void checkOutBook(BookEntity bookEntity) {
    if (!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**Even better (defensive):**
```java
@Transactional
public void checkOutBook(Long bookId) {
    BookEntity book = bookRepository.findById(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    if (book.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
        throw new BookAlreadyCheckedOutException(bookId);
    }

    book.setBookStatus(BookStatus.CHECKED_OUT.toString());
    book.setCheckoutDate(LocalDateTime.now());
    bookRepository.save(book);
}
```

---

### Issue #2: Missing @Transactional on checkInBook() (🟡 MEDIUM)

**Location:** `BookService.java:73-77`

```java
// ❌ NO @Transactional!
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);  // Database read
    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());  // ❌ Can crash if null!
    updateBook(bookEntity);  // Database write
}
```

**Multiple problems:**
1. **No transaction** - Read and write aren't atomic
2. **Null pointer risk** - If book not found, crashes on line 75
3. **Two database calls** not wrapped in transaction

**What could go wrong:**
- Book is deleted between `findBookByTitle()` and `updateBook()`
- `findBookByTitle()` returns null → crashes
- Database update fails → inconsistent state

**Fixed version:**
```java
@Transactional
public void checkInBook(String bookTitle) {
    BookEntity book = bookRepository.findByTitleIgnoreCase(bookTitle)
        .orElseThrow(() -> new BookNotFoundException(bookTitle));

    if (book.getBookStatus().equals(BookStatus.AVAILABLE.toString())) {
        throw new BookAlreadyAvailableException(bookTitle);
    }

    book.setBookStatus(BookStatus.AVAILABLE.toString());
    book.setCheckinDate(LocalDateTime.now());
    bookRepository.save(book);
}
```

---

### Issue #3: Read-Modify-Write Without Transaction (🟡 MEDIUM)

**Location:** `BookService.java:65-67`

```java
// ❌ NO @Transactional!
public void updateBook(BookEntity bookEntity){
    bookRepository.save(bookEntity);
}
```

**This method is called from:**
- `checkInBook()` - No transaction
- `BookCommands.addToShelf()` - No transaction

**Pattern:** Read book → Modify in memory → Call updateBook()

**Problem:** If two threads do this simultaneously, one update can be lost:

```
Thread 1: Read book (shelf = A)
Thread 2: Read book (shelf = A)
Thread 1: Modify (shelf = B)
Thread 2: Modify (shelf = C)
Thread 1: Save (shelf = B)  ← Written to DB
Thread 2: Save (shelf = C)  ← Overwrites Thread 1's change!

Result: Thread 1's update is lost
```

**Fix:** Add `@Transactional` to calling methods, not to `updateBook()` itself.

---

### Issue #4: createNewBook() Has Race Condition (🟡 MEDIUM)

**Location:** `BookService.java:22-41`

```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    // ...
    BookEntity bookEntity = bookRepository.findByTitle(title);  // Check if exists
    AuthorEntity authorEntity = authorRepository.findByFirstNameAndLastName(firstName, lastName);

    if (authorEntity == null) {
        authorEntity = new AuthorEntity(firstName, lastName);
        authorRepository.save(authorEntity);  // ❌ Could create duplicate!
    }

    if (bookEntity == null) {
        bookEntity = new BookEntity();
        bookEntity.setTitle(title);
    }
    bookEntity.setAuthors(authorEntity);
    bookRepository.save(bookEntity);  // ❌ Could create duplicate!
}
```

**Race condition:**
```
Thread 1: Check if "Clean Code" exists → No
Thread 2: Check if "Clean Code" exists → No
Thread 1: Create "Clean Code" → Saved
Thread 2: Create "Clean Code" → ERROR! Duplicate title!
```

**Why @Transactional doesn't prevent this:**
By default, Spring uses **READ_COMMITTED** isolation level, which doesn't prevent this race condition.

**Solutions:**

**Option 1: Unique constraint in database** (BEST)
```java
@Entity
@Table(name = "books", uniqueConstraints = {
    @UniqueConstraint(columnNames = "title")
})
public class BookEntity {
    // ...
}
```

Then handle the exception:
```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    try {
        // ... existing code
    } catch (DataIntegrityViolationException e) {
        throw new BookAlreadyExistsException(title);
    }
}
```

**Option 2: Higher isolation level**
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void createNewBook(BookRequestDTO bookRequestDTO) {
    // ... existing code
}
```

**Performance cost:** Serializable isolation is slow (locks more aggressively).

**Recommendation:** Use Option 1 (unique constraint) for best balance of correctness and performance.

---

### Issue #5: No Transaction Boundaries for Multi-Step Operations

**Location:** `BookCommands.java:214-254` (addToShelf method)

```java
// ❌ Multiple database operations without transaction!
public void addToShelf(){
    // ... ComponentFlow to get bookTitle and shelfId ...

    BookEntity bookEnt = bookService.findBookByTitle(title);  // DB read #1
    if(bookEnt == null){
        System.out.println("Book Not Found In Library");
    } else {
        Long shelfId = Long.parseLong(res.getContext().get("bookshelf",String.class));
        bookEnt.setShelfId(shelfId);
        bookService.updateBook(bookEnt);  // DB write #1 (no transaction!)
        System.out.println("Added Book To the Shelf!");
    }
}
```

**Problem:** Read and write aren't atomic. Book could be deleted between read and write.

**Better:** Move logic to service method with `@Transactional`:

```java
// BookService.java
@Transactional
public void moveBookToShelf(String bookTitle, Long shelfId) {
    BookEntity book = bookRepository.findByTitleIgnoreCase(bookTitle)
        .orElseThrow(() -> new BookNotFoundException(bookTitle));

    ShelfEntity shelf = shelfRepository.findById(shelfId)
        .orElseThrow(() -> new ShelfNotFoundException(shelfId));

    book.setShelfId(shelfId);
    bookRepository.save(book);
}

// BookCommands.java
public void addToShelf() {
    String title = promptService.promptText("What book are you shelving?");
    Long shelfId = promptService.promptSelection("Choose a shelf", shelfOptions());

    try {
        bookService.moveBookToShelf(title, shelfId);  // ✅ Transaction in service
        console.success("Added book to the shelf!");
    } catch (BookNotFoundException e) {
        console.error("Book not found in library");
    }
}
```

---

## 🛡️ Concurrency Principles (For Future Web API)

When you add a REST API to Bibby, you'll need to think about concurrency. Here are the principles:

### Principle 1: Keep Concurrent Code Small

**Bad:** Entire class is synchronized
```java
@Service
public synchronized class BookService {  // ❌ Everything locked!
    // All methods synchronized implicitly
}
```

**Good:** Only critical sections synchronized
```java
@Service
public class BookService {
    private final Map<String, BookEntity> cache = new ConcurrentHashMap<>();

    public BookEntity getBook(String title) {  // ✅ No lock needed
        return cache.get(title);
    }

    @Transactional  // ✅ Database provides concurrency control
    public void checkOutBook(Long bookId) {
        // ...
    }
}
```

---

### Principle 2: Use Thread-Safe Collections

**Unsafe:**
```java
private Map<String, BookEntity> cache = new HashMap<>();  // ❌ Not thread-safe!

public void addToCache(String key, BookEntity value) {
    cache.put(key, value);  // ❌ Race condition!
}
```

**Safe:**
```java
private Map<String, BookEntity> cache = new ConcurrentHashMap<>();  // ✅ Thread-safe!

public void addToCache(String key, BookEntity value) {
    cache.put(key, value);  // ✅ Safe
}
```

---

### Principle 3: Immutability is Your Friend

**Mutable (unsafe):**
```java
public class BookDTO {
    private String title;  // ❌ Mutable field

    public void setTitle(String title) {
        this.title = title;  // ❌ Can be modified by multiple threads
    }
}
```

**Immutable (safe):**
```java
public record BookDTO(String title, String author) {  // ✅ Immutable!
    // No setters - can't be modified after creation
}

// Or with class:
public final class BookDTO {
    private final String title;  // ✅ final
    private final String author;  // ✅ final

    public BookDTO(String title, String author) {
        this.title = title;
        this.author = author;
    }

    // Only getters, no setters
}
```

**Immutable objects are inherently thread-safe** because they can't be modified.

---

### Principle 4: Avoid Shared State

**Bad:** Shared mutable state
```java
@RestController
public class BookController {
    private int requestCount = 0;  // ❌ Shared mutable state!

    @GetMapping("/books")
    public List<Book> getBooks() {
        requestCount++;  // ❌ RACE CONDITION! Multiple threads increment simultaneously
        return bookService.getAllBooks();
    }
}
```

**Good:** No shared state (use metrics library)
```java
@RestController
public class BookController {
    private final Counter requestCounter;  // ✅ Thread-safe metrics

    public BookController(MeterRegistry registry) {
        this.requestCounter = registry.counter("books.requests");
    }

    @GetMapping("/books")
    public List<Book> getBooks() {
        requestCounter.increment();  // ✅ Thread-safe
        return bookService.getAllBooks();
    }
}
```

---

### Principle 5: Let Spring Manage Transactions

**Don't:**
```java
@Service
public class BookService {
    private final EntityManager em;

    public void createBook(Book book) {
        EntityTransaction tx = em.getTransaction();  // ❌ Manual transaction management
        try {
            tx.begin();
            em.persist(book);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }
}
```

**Do:**
```java
@Service
public class BookService {
    private final BookRepository bookRepository;

    @Transactional  // ✅ Spring manages transactions
    public void createBook(Book book) {
        bookRepository.save(book);
    }
}
```

Spring handles:
- Transaction begin/commit/rollback
- Connection management
- Thread-local transaction context

---

## 🛠️ Refactoring Examples

### Example 1: Add @Transactional to checkOutBook()

**BEFORE:**
```java
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);
    }
}
```

**AFTER:**
```java
@Transactional
public void checkOutBook(Long bookId) {
    BookEntity book = bookRepository.findById(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    if (book.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
        throw new BookAlreadyCheckedOutException(bookId);
    }

    book.setBookStatus(BookStatus.CHECKED_OUT.toString());
    book.setCheckoutDate(LocalDateTime.now());
    bookRepository.save(book);
}
```

**Benefits:**
- ✅ Atomic read-modify-write
- ✅ Proper error handling
- ✅ No null pointer risk
- ✅ Ready for multi-threaded web API

---

### Example 2: Add Unique Constraints to Prevent Duplicates

**BookEntity.java:**
```java
@Entity
@Table(name = "books", uniqueConstraints = {
    @UniqueConstraint(name = "uk_book_title", columnNames = "title")
})
public class BookEntity {
    // ...
}
```

**AuthorEntity.java:**
```java
@Entity
@Table(name = "authors", uniqueConstraints = {
    @UniqueConstraint(name = "uk_author_name",
                     columnNames = {"first_name", "last_name"})
})
public class AuthorEntity {
    // ...
}
```

**Handle constraint violations:**
```java
@Transactional
public void createNewBook(BookRequestDTO bookRequestDTO) {
    try {
        // ... existing creation logic ...
    } catch (DataIntegrityViolationException e) {
        if (e.getMessage().contains("uk_book_title")) {
            throw new BookAlreadyExistsException(bookRequestDTO.title());
        } else if (e.getMessage().contains("uk_author_name")) {
            throw new AuthorAlreadyExistsException(
                bookRequestDTO.firstName(),
                bookRequestDTO.lastName()
            );
        }
        throw e;
    }
}
```

---

### Example 3: Move Transaction Boundary to Service

**BEFORE (in command class):**
```java
// BookCommands.java
public void addToShelf(){
    BookEntity bookEnt = bookService.findBookByTitle(title);
    if(bookEnt == null){
        System.out.println("Book Not Found");
    } else {
        bookEnt.setShelfId(shelfId);
        bookService.updateBook(bookEnt);  // ❌ No transaction wrapping both operations
    }
}
```

**AFTER (in service class):**
```java
// BookService.java
@Transactional
public void moveBookToShelf(String bookTitle, Long shelfId) {
    BookEntity book = bookRepository.findByTitleIgnoreCase(bookTitle)
        .orElseThrow(() -> new BookNotFoundException(bookTitle));

    // Validate shelf exists
    ShelfEntity shelf = shelfRepository.findById(shelfId)
        .orElseThrow(() -> new ShelfNotFoundException(shelfId));

    book.setShelfId(shelfId);
    bookRepository.save(book);
}

// BookCommands.java
public void addToShelf(){
    String title = promptService.promptText("What book?");
    Long shelfId = promptService.promptSelection("Which shelf?", shelfOptions());

    try {
        bookService.moveBookToShelf(title, shelfId);  // ✅ Transactional
        console.success("Added book to shelf!");
    } catch (BookNotFoundException e) {
        console.error("Book not found");
    } catch (ShelfNotFoundException e) {
        console.error("Shelf not found");
    }
}
```

---

## 🚨 Your Critical Issues & Solutions

### Issue Summary Table

| Issue | Severity | Impact | Fix Time |
|-------|----------|--------|----------|
| checkOutBook() missing @Transactional | 🟡 MEDIUM | Race condition in web API | 15 min |
| checkInBook() missing @Transactional | 🟡 MEDIUM | Inconsistent state possible | 15 min |
| updateBook() called without transaction | 🟡 MEDIUM | Lost updates possible | 30 min |
| createNewBook() race condition | 🟡 MEDIUM | Duplicate books/authors | 1 hour |
| Multi-step operations in commands | 🟢 LOW | Complexity in wrong layer | 1 hour |

---

## ✅ Your Action Items

### 🟡 **Priority 1: Add @Transactional to State-Changing Methods** (30 minutes)

**Action:**
1. Add `@Transactional` to `checkOutBook()`
2. Add `@Transactional` to `checkInBook()`
3. Refactor both to accept ID instead of entity
4. Add proper error handling

**Files:**
- Update: `library/book/BookService.java`

---

### 🟡 **Priority 2: Add Database Unique Constraints** (1 hour)

**Action:**
1. Add unique constraint on `books.title`
2. Add unique constraint on `authors.{first_name, last_name}`
3. Update `createNewBook()` to handle constraint violations
4. Create custom exceptions (BookAlreadyExistsException, etc.)

**Files:**
- Update: `library/book/BookEntity.java`
- Update: `library/author/AuthorEntity.java`
- Update: `library/book/BookService.java`
- Create: `library/book/exceptions/BookAlreadyExistsException.java`

---

### 🟡 **Priority 3: Move Transaction Boundaries to Service** (1 hour)

**Action:**
1. Create `moveBookToShelf()` method in BookService with `@Transactional`
2. Refactor BookCommands to call service method
3. Move business logic out of command classes

**Files:**
- Update: `library/book/BookService.java`
- Update: `cli/BookCommands.java`

---

### 🟢 **Priority 4: Document Concurrency Strategy** (15 minutes)

**Action:**
Create a `CONCURRENCY.md` file documenting:
- Current: Single-threaded CLI (no concurrency issues)
- Future: Web API will need thread safety
- Transaction usage patterns
- When to use `@Transactional`

**Files:**
- Create: `docs/CONCURRENCY.md`

---

## 📊 Concurrency Health Scorecard

| Area | Current Grade | Notes |
|------|---------------|-------|
| Thread Safety | ✅ A | Single-threaded CLI |
| Transaction Usage | ⚠️ C+ | Some methods missing @Transactional |
| Race Condition Prevention | ⚠️ C | No unique constraints |
| Isolation from Frameworks | ⚠️ B- | Business logic in commands |
| Future-Proofing | ⚠️ C+ | Not ready for web API |

**Overall Concurrency Grade: B-**

Good for current CLI app, but needs fixes before adding web API.

---

## 🎓 Key Takeaways

1. **Your CLI app is single-threaded, so concurrency isn't a current concern.** But plan ahead for web APIs.

2. **Transactions are concurrency** - even single-threaded apps need `@Transactional` for database consistency.

3. **All state-changing methods should be @Transactional** - Ensures read-modify-write is atomic.

4. **Unique constraints prevent race conditions** - Database-level enforcement is strongest.

5. **Business logic belongs in services, not commands** - Makes adding web API easier.

6. **Immutability prevents concurrency bugs** - Use records or final fields.

7. **Thread-safe collections exist for a reason** - Use ConcurrentHashMap, not HashMap.

8. **Keep synchronized sections small** - Don't lock entire classes.

9. **Let Spring manage transactions** - Don't manage EntityManager manually.

10. **Concurrency bugs are non-deterministic** - They're hard to reproduce and debug. Prevent them with good design.

---

## 📚 Further Study

### Books
- **"Java Concurrency in Practice"** - Brian Goetz
  *The bible of Java concurrency. Essential reading.*

- **"Seven Concurrency Models in Seven Weeks"** - Paul Butcher
  *Explore different approaches to concurrency.*

### Spring Resources
- **Spring Transaction Management**
  https://docs.spring.io/spring-framework/reference/data-access/transaction.html

- **Transaction Isolation Levels**
  https://www.baeldung.com/spring-transactional-propagation-isolation

### Videos
- **Brian Goetz: "From Concurrent to Parallel"**
  (JavaOne talks on concurrency)

---

## 💭 Mentor's Final Thoughts

Leo, here's the good news: **concurrency is mostly not a problem for you right now.** Your CLI app is single-threaded, so you don't have race conditions or deadlocks.

But you DO have some **transaction issues** that will bite you when you add a web API:
- `checkOutBook()` and `checkInBook()` aren't transactional
- No unique constraints on book titles or author names
- Business logic scattered in command classes instead of services

These are **easy fixes** (2-3 hours total) that future-proof your codebase.

**When you add the REST API** (which I recommend as your next major feature), you'll be glad you fixed these issues now. Concurrency bugs in production are nightmare fuel—they're rare, hard to reproduce, and catastrophic when they occur.

**Start with Priority 1** (add @Transactional to state-changing methods). That's 30 minutes and protects you from the most common transaction bugs.

Then add unique constraints (Priority 2). That's bulletproof protection against duplicate data.

You're **39% through the mentorship** and have completed all Clean Code fundamentals except 2 sections. Next up: Sections 13-14, then we dive into **Spring Framework Mastery**.

The foundation is solid. Let's keep building.

— Your Mentor

---

**Next:** Section 13 - Successive Refinement (case study in refactoring)
**Previous:** [Section 11 - Emergence](./11-emergence.md)
**Home:** [Master Index](./00-master-index.md)
