# Section 29: Transaction Management

**Learning objective:** Master `@Transactional` annotation, understand ACID properties, learn transaction propagation levels, handle rollback rules, and fix Bibby's missing and incomplete transaction boundaries.

**Why this matters:** Your `BookcaseService.createNewBookCase()` saves a bookcase, then loops to create multiple shelves. If shelf #3 fails, the bookcase and shelves #1-2 are already saved to the database. You now have **partial data** and an **inconsistent state**. Transactions prevent this by making multi-step operations atomic (all-or-nothing).

**Duration:** 55 min read

---

## The Critical Question

**Your current code:**

```java
// BookcaseService.java:25-40 (NO @Transactional)
public String createNewBookCase(String label, int capacity){
    bookcaseEntity = new BookcaseEntity(label, capacity);
    bookcaseRepository.save(bookcaseEntity);  // ✅ Saved to DB

    for(int i = 0; i < capacity; i++){
        addShelf(bookcaseEntity, i, i);  // What if this crashes at i=3?
    }
    return "Created bookcase";
}

public void addShelf(BookcaseEntity bookcase, int label, int position){
    ShelfEntity shelf = new ShelfEntity();
    // ... set fields ...
    shelfRepository.save(shelf);  // ✅ Saved to DB
}
```

**Question:** What happens if `addShelf()` crashes on the 3rd iteration (e.g., database constraint violation)?

**Answer:**
- **Bookcase:** ✅ Saved (persisted)
- **Shelf 1:** ✅ Saved
- **Shelf 2:** ✅ Saved
- **Shelf 3:** ❌ Failed
- **Shelves 4-10:** ❌ Never created

**Result:** Bookcase with capacity 10 has only 2 shelves. **Data inconsistency!**

**With `@Transactional`:** ALL changes rolled back. Nothing saved. Clean failure.

This section reveals how to use transactions properly.

---

## What You'll Learn From Your Code

**Transaction coverage in Bibby:**
- **1 method with `@Transactional`:** `BookService.createNewBook()` ✅
- **Missing transactions:**
  - `BookcaseService.createNewBookCase()` ❌ (CRITICAL BUG - multiple saves)
  - `BookService.checkOutBook()` ❌
  - `BookService.updateBook()` ❌
  - `BookService.checkInBook()` ❌

**Grade:** **D** (only 1 out of 5 write methods has transactions)

**After this section:** **Grade: A** (all write methods properly transactional)

---

## Part 1: What is a Transaction?

### Definition

**Transaction:** A unit of work that either **completely succeeds** or **completely fails**. No partial completion.

**ACID Properties:**

| Property | Meaning | Example |
|----------|---------|---------|
| **Atomicity** | All-or-nothing | Create bookcase + shelves: both succeed or both fail |
| **Consistency** | Database stays valid | Foreign key constraints always satisfied |
| **Isolation** | Concurrent transactions don't interfere | Two users creating books simultaneously don't see each other's uncommitted changes |
| **Durability** | Committed data survives crashes | After commit, data is permanent (written to disk) |

---

### Without Transactions (Your Current Code)

```java
// BookcaseService.createNewBookCase() - NO TRANSACTION
public String createNewBookCase(String label, int capacity){
    bookcaseRepository.save(bookcase);  // ← COMMIT #1 (immediate)

    for(int i = 0; i < capacity; i++){
        shelfRepository.save(shelf);    // ← COMMIT #2, #3, #4... (each iteration)
        // If this fails, previous shelves already committed!
    }
}
```

**Problems:**
1. ❌ **Not atomic** - Partial data if loop fails
2. ❌ **Inconsistent state** - Bookcase capacity doesn't match actual shelves
3. ❌ **Can't rollback** - Already committed to database
4. ❌ **Data corruption** - Orphaned bookcase with missing shelves

---

### With Transactions (What You Should Have)

```java
// BookcaseService.createNewBookCase() - WITH TRANSACTION
@Transactional
public String createNewBookCase(String label, int capacity){
    bookcaseRepository.save(bookcase);  // ← Not committed yet (in transaction)

    for(int i = 0; i < capacity; i++){
        shelfRepository.save(shelf);    // ← Not committed yet (in transaction)
        // If this fails, ENTIRE transaction rolls back
    }

    // Method completes successfully → COMMIT (all changes permanent)
    // Method throws exception → ROLLBACK (nothing saved)
}
```

**Benefits:**
1. ✅ **Atomic** - All shelves created or none
2. ✅ **Consistent** - Capacity always matches actual shelf count
3. ✅ **Rollback on failure** - Clean state on error
4. ✅ **Data integrity** - Never orphaned bookcases

---

## Part 2: How Spring Transactions Work

### The Proxy Pattern

**When you write:**
```java
@Service
public class BookService {
    @Transactional
    public void createNewBook(...) {
        // Your code
    }
}
```

**Spring creates a proxy:**

```
Client calls bookService.createNewBook()
       ↓
Spring Transaction Proxy intercepts call
       ↓
1. BEGIN TRANSACTION (EntityManager.getTransaction().begin())
       ↓
2. Call actual method (your code runs)
       ↓
3. If SUCCESS → COMMIT (persist all changes)
   If EXCEPTION → ROLLBACK (undo all changes)
```

**Proxy wraps your method:**
```java
// What Spring generates (simplified):
public void createNewBook(...) {
    EntityManager em = ...;
    EntityTransaction tx = em.getTransaction();

    try {
        tx.begin();              // ← Start transaction

        // Your actual method code runs here
        actualCreateNewBook();

        tx.commit();             // ← Success: commit
    } catch (RuntimeException e) {
        tx.rollback();           // ← Failure: rollback
        throw e;
    }
}
```

**This is why `@Transactional` is "magical" - Spring does the boilerplate.**

---

## Part 3: When to Use @Transactional

### Rule of Thumb

Use `@Transactional` when method:
1. **Writes to database** (INSERT, UPDATE, DELETE)
2. **Multiple database operations** that must be atomic
3. **Reads that must be consistent** (prevent dirty reads)

---

### Your Methods - Transaction Analysis

#### ✅ CORRECT: BookService.createNewBook()

```java
// BookService.java:22-41 (HAS @Transactional)
@Transactional
public void createNewBook(BookRequestDTO dto) {
    // Operation 1: Find or create author
    AuthorEntity author = authorRepository.findByFirstNameAndLastName(...);
    if (author == null) {
        author = new AuthorEntity(...);
        authorRepository.save(author);  // ← Write #1
    }

    // Operation 2: Create book
    BookEntity book = new BookEntity();
    book.setTitle(title);
    book.setAuthors(author);
    bookRepository.save(book);          // ← Write #2
}
```

**Why transaction needed:**
- Two writes (author, book) must be atomic
- If book creation fails, author should rollback too
- **Grade: A** (correctly uses `@Transactional`)

---

#### ❌ CRITICAL BUG: BookcaseService.createNewBookCase()

```java
// BookcaseService.java:25-40 (MISSING @Transactional)
public String createNewBookCase(String label, int capacity){
    bookcaseRepository.save(bookcase);  // ← Write #1

    for(int i = 0; i < capacity; i++){
        addShelf(bookcaseEntity, i, i); // ← Write #2, #3, #4...
    }
}

public void addShelf(...){
    shelfRepository.save(shelf);        // ← Each iteration writes
}
```

**Problems:**
1. ❌ **No `@Transactional`** - Each save commits immediately
2. ❌ **Loop with writes** - Creates 1 bookcase + N shelves
3. ❌ **Partial failure possible** - Bookcase saved, some shelves fail
4. ❌ **Data inconsistency** - Capacity 10, but only 3 shelves exist

**Fix:**
```java
@Transactional  // ← Add this!
public String createNewBookCase(String label, int capacity){
    bookcaseRepository.save(bookcase);

    for(int i = 0; i < capacity; i++){
        addShelf(bookcaseEntity, i, i);
    }
    // All saves commit together (or all rollback)
}
```

**Grade after fix: A**

---

#### ❌ MISSING: BookService.checkOutBook()

```java
// BookService.java:56-62 (MISSING @Transactional)
public void checkOutBook(BookEntity bookEntity){
    if(!bookEntity.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        bookEntity.setBookStatus("CHECKED_OUT");
        bookRepository.save(bookEntity);  // ← Write
    }
}
```

**Why transaction needed:**
- Modifies book status (UPDATE)
- Should be atomic with potential future operations

**Fix:**
```java
@Transactional  // ← Add this!
public void checkOutBook(Long bookId){  // ← Also fix: take ID, not entity
    BookEntity book = bookRepository.findById(bookId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if(!book.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())){
        book.setBookStatus(BookStatus.CHECKED_OUT.toString());
        bookRepository.save(book);
    }
}
```

---

#### ❌ MISSING: BookService.updateBook()

```java
// BookService.java:65-67 (MISSING @Transactional)
public void updateBook(BookEntity bookEntity){
    bookRepository.save(bookEntity);  // ← Write
}
```

**Fix:**
```java
@Transactional  // ← Add this!
public void updateBook(Long bookId, UpdateBookRequest dto){
    BookEntity book = bookRepository.findById(bookId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    // Map DTO to entity
    book.setTitle(dto.title());
    book.setIsbn(dto.isbn());
    // ...

    bookRepository.save(book);
}
```

---

#### ❌ MISSING: BookService.checkInBook()

```java
// BookService.java:73-77 (MISSING @Transactional)
public void checkInBook(String bookTitle) {
    BookEntity bookEntity = findBookByTitle(bookTitle);  // ← Read
    bookEntity.setBookStatus(BookStatus.AVAILABLE.toString());
    updateBook(bookEntity);                               // ← Write
}
```

**Fix:**
```java
@Transactional  // ← Add this!
public void checkInBook(String bookTitle) {
    BookEntity book = bookRepository.findByTitleIgnoreCase(bookTitle)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    book.setBookStatus(BookStatus.AVAILABLE.toString());
    bookRepository.save(book);
}
```

---

#### ✅ NO TRANSACTION NEEDED: Read-Only Methods

```java
// BookService.java:79-81 (read-only)
public List<BookEntity> findBooksByShelf(Long id) {
    return bookRepository.findByShelfId(id);
}
```

**Why no transaction needed:**
- Only reads (SELECT)
- No writes

**Optional optimization:**
```java
@Transactional(readOnly = true)  // ← Tells Hibernate to optimize for reads
public List<BookResponse> findBooksByShelf(Long id) {
    return bookRepository.findByShelfId(id).stream()
        .map(this::toBookResponse)
        .toList();
}
```

**Benefits of `readOnly = true`:**
- Hibernate skips dirty checking (faster)
- Some databases optimize read-only transactions
- Prevents accidental writes

---

## Part 4: Transaction Propagation

### What is Propagation?

**Propagation:** How nested `@Transactional` methods behave.

**Scenario:**
```java
@Transactional
public void outerMethod() {
    innerMethod();  // ← Also @Transactional - what happens?
}

@Transactional
public void innerMethod() {
    // ...
}
```

**Question:** Does `innerMethod()` create a new transaction or join the existing one?

**Answer:** Depends on **propagation level**.

---

### Propagation Levels

| Propagation | Behavior | Use Case |
|-------------|----------|----------|
| **REQUIRED** (default) | Join existing transaction or create new | Most methods |
| **REQUIRES_NEW** | Always create new transaction (suspend existing) | Logging, audit trails |
| **MANDATORY** | Must be called within transaction (error if none) | Internal helper methods |
| **SUPPORTS** | Join if exists, run without if not | Optional transactional behavior |
| **NOT_SUPPORTED** | Run without transaction (suspend existing) | Non-transactional operations |
| **NEVER** | Must NOT be called within transaction (error if exists) | External API calls |
| **NESTED** | Create savepoint (subtransaction) | Partial rollback scenarios |

---

### REQUIRED (Default - 99% of cases)

```java
@Transactional  // Defaults to REQUIRED
public void outerMethod() {
    innerMethod();  // Joins outer transaction
}

@Transactional  // Also REQUIRED
public void innerMethod() {
    // Runs in same transaction as outer
}
```

**Result:**
- **One transaction** for both methods
- **Both commit together** (or both rollback)

**Example:**
```java
@Transactional
public void createBookWithAuthor() {
    createAuthor();  // ← Joins this transaction
    createBook();    // ← Also joins this transaction
    // All commit together
}
```

---

### REQUIRES_NEW (Separate Transaction)

```java
@Transactional
public void outerMethod() {
    innerMethod();  // ← Creates NEW transaction (suspends outer)
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void innerMethod() {
    // Runs in separate transaction
    // Commits independently of outer
}
```

**Use case: Audit logging**
```java
@Transactional
public void deleteBook(Long bookId) {
    bookRepository.deleteById(bookId);

    auditLog("Book deleted: " + bookId);  // ← REQUIRES_NEW
    // Audit log commits even if book deletion rolls back
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void auditLog(String message) {
    auditRepository.save(new AuditEntry(message));
    // Commits immediately, independent of outer transaction
}
```

**Why:** You always want audit logs persisted, even if main operation fails.

---

### MANDATORY (Must Have Transaction)

```java
@Transactional(propagation = Propagation.MANDATORY)
public void helperMethod() {
    // Must be called from within a transaction
    // Throws exception if called without transaction
}

@Transactional
public void outerMethod() {
    helperMethod();  // ✅ OK (outerMethod has transaction)
}

public void noTransactionMethod() {
    helperMethod();  // ❌ Throws IllegalTransactionStateException
}
```

**Use case:** Internal methods that should never be called directly.

---

## Part 5: Rollback Rules

### Default Rollback Behavior

**By default, Spring rolls back on:**
- ✅ `RuntimeException` (unchecked exceptions)
- ✅ `Error`
- ❌ `Exception` (checked exceptions) - **NO rollback!**

---

### The Checked Exception Trap

```java
@Transactional
public void createBook() throws IOException {  // Checked exception
    bookRepository.save(book);
    throw new IOException("File not found");   // ❌ NO ROLLBACK!
    // Book is STILL SAVED despite exception!
}
```

**Why no rollback?** Spring assumes checked exceptions are recoverable (e.g., retry logic).

**Fix: Specify rollback for all exceptions:**
```java
@Transactional(rollbackFor = Exception.class)  // ← Rollback on ANY exception
public void createBook() throws IOException {
    bookRepository.save(book);
    throw new IOException("File not found");   // ✅ ROLLBACK
}
```

---

### Custom Rollback Rules

```java
// Rollback ONLY on specific exceptions:
@Transactional(rollbackFor = {IOException.class, SQLException.class})
public void createBook() {
    // Rolls back on IOException or SQLException
}

// DON'T rollback on specific exceptions:
@Transactional(noRollbackFor = IllegalArgumentException.class)
public void createBook() {
    // Validation errors don't trigger rollback
}
```

---

### Programmatic Rollback

```java
@Transactional
public void createBook() {
    try {
        bookRepository.save(book);
        externalApiCall();  // Might fail
    } catch (ExternalApiException e) {
        // Mark transaction for rollback
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        // Continue with error handling
    }
}
```

---

## Part 6: Common Transaction Mistakes

### Mistake 1: Calling @Transactional Method from Same Class

```java
@Service
public class BookService {

    @Transactional
    public void methodA() {
        methodB();  // ❌ Calls methodB directly (NO proxy, NO transaction!)
    }

    @Transactional
    public void methodB() {
        // This transaction NEVER starts!
    }
}
```

**Why it fails:** Spring proxy only intercepts **external** calls. Internal calls bypass the proxy.

**Fix: Inject self-reference:**
```java
@Service
public class BookService {

    private final BookService self;  // ← Inject self

    public BookService(BookService self) {
        this.self = self;
    }

    @Transactional
    public void methodA() {
        self.methodB();  // ✅ Calls through proxy (transaction works)
    }

    @Transactional
    public void methodB() {
        // Transaction starts correctly
    }
}
```

**Or better: Refactor to separate classes.**

---

### Mistake 2: Modifying Entity After Transaction Ends

```java
@Transactional
public BookEntity createBook() {
    BookEntity book = new BookEntity();
    bookRepository.save(book);
    return book;  // ← Transaction ends here
}

// In controller:
BookEntity book = bookService.createBook();  // Transaction committed
book.setTitle("New Title");  // ❌ Change not persisted!
```

**Why:** Entity is **detached** after transaction ends.

**Fix:**
```java
// Option 1: Don't return entities from services (use DTOs)
public BookResponse createBook() {
    BookEntity book = new BookEntity();
    bookRepository.save(book);
    return toBookResponse(book);  // ← DTO, not entity
}

// Option 2: Make changes within transaction
@Transactional
public void updateBookTitle(Long bookId, String newTitle) {
    BookEntity book = bookRepository.findById(bookId).orElseThrow();
    book.setTitle(newTitle);
    // Auto-saved when transaction commits (dirty checking)
}
```

---

### Mistake 3: Transaction Too Large

```java
@Transactional
public void processAllBooks() {
    List<BookEntity> books = bookRepository.findAll();  // 10,000 books

    for (BookEntity book : books) {
        book.setCheckoutCount(0);
        bookRepository.save(book);  // ← Locks database for entire loop!
    }
    // Transaction holds locks on 10,000 rows until method completes
}
```

**Problems:**
- Locks database rows for long time
- Other users blocked
- Transaction timeout risk

**Fix: Batch processing**
```java
public void processAllBooks() {
    Pageable pageable = PageRequest.of(0, 100);  // 100 books per batch
    Page<BookEntity> page;

    do {
        page = bookRepository.findAll(pageable);
        processBatch(page.getContent());  // ← Separate transaction per batch
        pageable = page.nextPageable();
    } while (page.hasNext());
}

@Transactional
public void processBatch(List<BookEntity> books) {
    books.forEach(book -> book.setCheckoutCount(0));
    bookRepository.saveAll(books);
}
```

---

## Part 7: Testing Transactions

### Test Rollback Behavior

```java
@SpringBootTest
@Transactional  // ← Test runs in transaction (auto-rollback after test)
class BookServiceTransactionTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldRollbackWhenAuthorCreationFails() {
        // Given
        CreateBookRequest request = new CreateBookRequest(
            "1984",
            null,  // ← Will fail validation
            "Orwell"
        );

        // When & Then
        assertThrows(ConstraintViolationException.class, () -> {
            bookService.createNewBook(request);
        });

        // Verify rollback: no book or author saved
        assertThat(bookRepository.findAll()).isEmpty();
    }

    @Test
    @Commit  // ← Override default rollback (actually persist)
    void shouldCommitWhenSuccessful() {
        // Given
        CreateBookRequest request = new CreateBookRequest("1984", "George", "Orwell");

        // When
        bookService.createNewBook(request);

        // Then
        assertThat(bookRepository.findAll()).hasSize(1);
    }
}
```

---

## Part 8: Complete Transaction Strategy for Bibby

### Service Methods - Transaction Analysis

| Method | Current | Should Be | Reason |
|--------|---------|-----------|--------|
| `BookService.createNewBook()` | `@Transactional` ✅ | Keep | Multiple writes (author, book) |
| `BookService.checkOutBook()` | None ❌ | `@Transactional` | Updates book status |
| `BookService.updateBook()` | None ❌ | `@Transactional` | Updates book |
| `BookService.checkInBook()` | None ❌ | `@Transactional` | Updates book status |
| `BookService.findBookByTitle()` | None | `@Transactional(readOnly=true)` | Optional optimization |
| `BookcaseService.createNewBookCase()` | None ❌ | `@Transactional` | **CRITICAL: Creates bookcase + N shelves** |
| `BookcaseService.addShelf()` | None | Remove `@Transactional` | Called from within transaction |

---

### Fixed Services

**BookService.java:**
```java
@Service
public class BookService {

    // KEEP (already has @Transactional)
    @Transactional
    public BookResponse createNewBook(CreateBookRequest request) {
        // ...
    }

    // ADD @Transactional
    @Transactional
    public void checkOutBook(Long bookId) {
        BookEntity book = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!book.getBookStatus().equals(BookStatus.CHECKED_OUT.toString())) {
            book.setBookStatus(BookStatus.CHECKED_OUT.toString());
            bookRepository.save(book);
        }
    }

    // ADD @Transactional
    @Transactional
    public void updateBook(Long bookId, UpdateBookRequest request) {
        BookEntity book = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        book.setTitle(request.title());
        book.setIsbn(request.isbn());
        // ... map other fields
        bookRepository.save(book);
    }

    // ADD @Transactional
    @Transactional
    public void checkInBook(String bookTitle) {
        BookEntity book = bookRepository.findByTitleIgnoreCase(bookTitle)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        book.setBookStatus(BookStatus.AVAILABLE.toString());
        bookRepository.save(book);
    }

    // OPTIONAL: Add readOnly for optimization
    @Transactional(readOnly = true)
    public List<BookResponse> findBooksByShelf(Long shelfId) {
        return bookRepository.findByShelfId(shelfId).stream()
            .map(this::toBookResponse)
            .toList();
    }
}
```

---

**BookcaseService.java:**
```java
@Service
public class BookcaseService {

    // ADD @Transactional (CRITICAL FIX)
    @Transactional
    public String createNewBookCase(String label, int capacity){
        BookcaseEntity bookcase = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);

        if(bookcase != null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bookcase already exists");
        }

        bookcase = new BookcaseEntity(label, capacity);
        bookcaseRepository.save(bookcase);  // ← Part of transaction

        for(int i = 0; i < capacity; i++){
            addShelf(bookcase, i, i);        // ← Part of same transaction
        }
        // All saves commit together (or all rollback)

        log.info("Created bookcase: {}", label);
        return "Created bookcase " + label;
    }

    // NO @Transactional (called from within transaction)
    public void addShelf(BookcaseEntity bookcase, int label, int position){
        ShelfEntity shelf = new ShelfEntity();
        shelf.setBookcaseId(bookcase.getBookcaseId());
        shelf.setShelfLabel("Shelf " + label);
        shelf.setShelfPosition(position);
        shelfRepository.save(shelf);
    }

    // Read-only (optional optimization)
    @Transactional(readOnly = true)
    public List<BookcaseEntity> getAllBookcases(){
        return bookcaseRepository.findAll();
    }
}
```

---

## Summary: Your Transaction Situation - Before & After

### Before (Current State):

| Issue | Grade | Problem |
|-------|-------|---------|
| Only 1 of 5 write methods has `@Transactional` | D | Incomplete transaction coverage |
| `BookcaseService.createNewBookCase()` missing | F | **CRITICAL: Partial data possible** |
| No propagation configuration | C | Default works, but not explicit |
| No rollback rules | C | Checked exceptions don't rollback |
| No read-only transactions | C | Missed optimization |

**Overall Grade: D** (critical bug in bookcase creation)

---

### After (All Fixes Applied):

| Improvement | Grade | Benefit |
|-------------|-------|---------|
| All write methods have `@Transactional` | A | Complete transaction coverage |
| `BookcaseService.createNewBookCase()` fixed | A | Atomic bookcase + shelves creation |
| Explicit propagation when needed | A | Clear intent |
| Rollback rules for checked exceptions | A | Consistent rollback behavior |
| Read-only optimization | A | Better performance |
| Transaction tests | A | Verified rollback behavior |

**Overall Grade: A** (production-ready transaction management)

---

## Action Items

### Priority 1: Fix Critical Bug

```java
// BookcaseService.java - ADD @Transactional
@Transactional  // ← ADD THIS!
public String createNewBookCase(String label, int capacity){
    bookcaseRepository.save(bookcase);
    for(int i = 0; i < capacity; i++){
        addShelf(bookcase, i, i);
    }
    return "Created bookcase";
}
```

**Impact:** Prevents partial data if shelf creation fails.

---

### Priority 2: Add @Transactional to Write Methods

```java
// BookService.java
@Transactional  // ← ADD
public void checkOutBook(Long bookId) { /* ... */ }

@Transactional  // ← ADD
public void updateBook(Long bookId, UpdateBookRequest request) { /* ... */ }

@Transactional  // ← ADD
public void checkInBook(String bookTitle) { /* ... */ }
```

---

### Priority 3: Add Read-Only Optimization (Optional)

```java
@Transactional(readOnly = true)  // ← ADD
public List<BookResponse> findBooksByShelf(Long shelfId) {
    return bookRepository.findByShelfId(shelfId).stream()
        .map(this::toBookResponse)
        .toList();
}
```

---

### Priority 4: Add Rollback Rules (For Future)

```java
@Transactional(rollbackFor = Exception.class)  // ← Rollback on ANY exception
public void riskyOperation() throws IOException {
    // ...
}
```

---

## What You Learned

1. **ACID properties:**
   - Atomicity (all-or-nothing)
   - Consistency (valid state)
   - Isolation (concurrent safety)
   - Durability (permanent)

2. **When to use `@Transactional`:**
   - Write operations (INSERT, UPDATE, DELETE)
   - Multiple operations that must be atomic
   - Optional for read-only (performance optimization)

3. **Bibby's critical bug:**
   - `BookcaseService.createNewBookCase()` has NO transaction
   - Creates bookcase + shelves in loop
   - If shelf #3 fails, bookcase and shelves #1-2 already saved
   - Result: Partial data, inconsistent state

4. **Propagation levels:**
   - REQUIRED (default) - Join or create
   - REQUIRES_NEW - Always new transaction
   - MANDATORY - Must have transaction

5. **Rollback rules:**
   - Default: RuntimeException rolls back
   - Checked exceptions do NOT rollback
   - Use `rollbackFor = Exception.class` for all exceptions

6. **Common mistakes:**
   - Calling `@Transactional` method from same class (bypasses proxy)
   - Modifying entity after transaction ends (detached)
   - Transaction too large (locks database)

---

**Next:** Section 30 - Spring Shell Commands will explore Spring Shell framework, command structure, parameter binding, and improving Bibby's CLI commands.

**Your transaction management is currently broken.** The `BookcaseService` bug is CRITICAL - it can leave partial data in your database. These fixes transform Bibby from **Grade D to Grade A**, ensuring data consistency and integrity.

You're almost done with the guide! Keep going!

---

*Section 29 complete. 29 of 33 sections finished (88%).*
*Next up: Section 30 - Spring Shell Commands*
