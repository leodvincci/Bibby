# Section 12: Memory Model Basics

**Estimated Time:** 45 minutes
**Prerequisites:** Sections 1-11
**Complexity:** ⭐⭐⭐ (Intermediate)

---

## Learning Objectives

By the end of this section, you will:

1. ✅ Understand stack vs heap memory allocation
2. ✅ Know how object references work (Java passes references, not objects)
3. ✅ Master String interning and the String pool
4. ✅ Grasp garbage collection basics
5. ✅ Avoid unnecessary object creation
6. ✅ Understand memory implications of collections
7. ✅ Apply immutability for memory efficiency

---

## The Java Memory Model: Stack vs Heap

**Java divides memory into two main areas:**

```
┌─────────────────────────────────────┐
│           STACK                     │  ← Method calls, local variables
│                                     │     primitives, object references
│  main()                             │
│    ├─ int count = 5                 │  ← Primitive value (5)
│    ├─ BookEntity book = ...         │  ← Reference (arrow to heap)
│    └─ String title = "Harry Potter" │  ← Reference (arrow to String pool)
│                                     │
│  createBook()                       │
│    ├─ String isbn = "123"           │
│    └─ int year = 1997               │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│           HEAP                      │  ← Objects, arrays
│                                     │
│  BookEntity@4a2                     │  ← Actual object
│    ├─ Long bookId = 1               │
│    ├─ String title → String pool    │
│    ├─ Set<Author> authors → [...]  │
│    └─ int publicationYear = 1997    │
│                                     │
│  ArrayList@7f3                      │  ← Collection object
│    └─ [ref1, ref2, ref3]            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│        STRING POOL (in Heap)        │  ← Interned Strings
│                                     │
│  "Harry Potter"                     │
│  "Shelf "                           │
│  "AVAILABLE"                        │
└─────────────────────────────────────┘
```

### Stack Memory

**Characteristics:**
- ✅ **Fast access** - LIFO (Last In, First Out)
- ✅ **Thread-specific** - Each thread has its own stack
- ✅ **Automatic cleanup** - When method returns, stack frame is popped

**What goes on the stack:**
- Primitive values (`int`, `long`, `boolean`, etc.)
- Object references (the arrow/pointer, not the object itself)
- Method call information

**Example from Bibby:**

```java
// BookcaseService.java:43-48
public void addShelf(BookcaseEntity bookcaseEntity, int label, int position){
    //                 ↑ Reference on stack       ↑ Primitive on stack

    ShelfEntity shelfEntity = new ShelfEntity();
    //          ↑ Reference on stack    ↑ Object created on heap

    shelfEntity.setBookcaseId(bookcaseEntity.getBookcaseId());
    shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());
    shelfEntity.setShelfPosition(position);
    shelfRepository.save(shelfEntity);
}
// ← Method returns, stack frame destroyed (but heap objects remain if referenced)
```

**Stack frame for this method:**

```
┌─────────────────────────────────┐
│ addShelf() Stack Frame          │
├─────────────────────────────────┤
│ bookcaseEntity → BookcaseEntity │  ← Reference to heap object
│ label = 5                       │  ← Primitive int
│ position = 5                    │  ← Primitive int
│ shelfEntity → ShelfEntity       │  ← Reference to heap object
└─────────────────────────────────┘
```

### Heap Memory

**Characteristics:**
- ✅ **Shared across threads** - All threads access same heap
- ✅ **Slower than stack** - More complex allocation
- ✅ **Garbage collected** - Objects removed when no longer referenced

**What goes on the heap:**
- All objects (`BookEntity`, `ArrayList`, `String`, etc.)
- Arrays
- Static variables (in a special area called "Method Area")

### Example: Object Creation

```java
// In BookService:
public void createBook(String title) {
    BookEntity book = new BookEntity();  // ← Object created on HEAP
    //         ↑                  ↑
    //    Stack reference    Heap object

    book.setTitle(title);  // ← Modifies heap object
    bookRepository.save(book);
}
```

**Memory layout:**

```
STACK:                  HEAP:
┌──────────────┐        ┌─────────────────────┐
│ createBook() │        │ BookEntity@a1b2     │
│  book → ──────────────>│  bookId: null       │
└──────────────┘        │  title: "..." ────→ String pool
                        │  authors: [] ────→  HashSet@c3d4
                        └─────────────────────┘
```

---

## Object References: Java Passes by Value (of the Reference)

**A common misconception: "Java passes objects by reference"**

**Truth: Java ALWAYS passes by value, but for objects, the value is a REFERENCE.**

### Example from Your Code

```java
// BookcaseService.java:25-40
public String createNewBookCase(String label, int capacity){
    //                            ↑ Reference     ↑ Primitive value

    BookcaseEntity bookcaseEntity = bookcaseRepository.findBookcaseEntityByBookcaseLabel(label);
    //             ↑ Reference (copy of reference from repository)

    if(bookcaseEntity != null){
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Already exists");
    }

    bookcaseEntity = new BookcaseEntity(label, capacity);
    //  ↑ NEW reference - doesn't affect caller

    bookcaseRepository.save(bookcaseEntity);
    // ...
}
```

**What happens in memory:**

```
Step 1: findBookcaseEntityByBookcaseLabel(label)
┌────────────┐           ┌──────────────────┐
│ Repository │ ─returns→ │ BookcaseEntity@1 │  (or null)
└────────────┘           └──────────────────┘

Step 2: if null, create new
┌────────────────────┐           ┌──────────────────┐
│ bookcaseEntity ref │ ─────────>│ BookcaseEntity@2 │  (NEW object)
└────────────────────┘           └──────────────────┘
```

**Key point:** Reassigning the reference (`bookcaseEntity = new...`) creates a NEW object but doesn't affect any references elsewhere.

### Modifying Object Fields (Pass by Reference Behavior)

```java
public void modifyBook(BookEntity book) {
    book.setTitle("New Title");  // ✅ Modifies the SAME object on heap
}

BookEntity myBook = new BookEntity();
myBook.setTitle("Original");
modifyBook(myBook);  // Passes COPY of reference (still points to same object)
System.out.println(myBook.getTitle());  // Output: "New Title" ✅
```

**But reassignment doesn't affect caller:**

```java
public void replaceBook(BookEntity book) {
    book = new BookEntity();  // ❌ Creates new object, local reference only
    book.setTitle("New Book");
}

BookEntity myBook = new BookEntity();
myBook.setTitle("Original");
replaceBook(myBook);  // Passes copy of reference
System.out.println(myBook.getTitle());  // Output: "Original" ❌ (unchanged!)
```

---

## The String Pool: Java's String Optimization

**Strings are special in Java - they're immutable and pooled.**

### String Pool Explained

```java
String s1 = "Hello";  // ← String literal - goes to String pool
String s2 = "Hello";  // ← Reuses SAME object from pool

s1 == s2  // ✅ true (same object reference!)

String s3 = new String("Hello");  // ← Creates NEW object on heap (not in pool)
s1 == s3  // ❌ false (different objects!)
s1.equals(s3)  // ✅ true (same content)
```

**Memory layout:**

```
HEAP - String Pool:
┌──────────────┐
│ "Hello"@abc  │ ← String literal (pooled)
└──────────────┘
     ↑      ↑
     │      └─────── s2 (reuses same object)
     └────────────── s1

HEAP - Regular Objects:
┌──────────────┐
│ "Hello"@def  │ ← new String() (NOT pooled)
└──────────────┘
     ↑
     └────────────── s3 (different object)
```

### String Concatenation and Memory

**Your code uses String concatenation in several places.**

#### Example 1: BookcaseService.java:46 (Memory Issue!)

```java
shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());
//                         ↑ String literal (pooled)
//                                  ↑ Creates Integer object (heap)
//                                            ↑ Creates String (heap)
//                        ↑ Concatenation creates ANOTHER String (heap)
```

**Memory impact:**

```
Objects created:
1. "Shelf " → String pool ✅
2. Integer.valueOf(label) → Integer object on heap ❌ (unnecessary!)
3. .toString() → String on heap
4. "Shelf " + ... → Final String on heap

Total: 3 heap objects for a simple label!
```

**The Fix:**

```java
// BEFORE (inefficient):
shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());

// AFTER (efficient):
shelfEntity.setShelfLabel("Shelf " + label);  // ✅ Java auto-converts int to String
// Only creates 1 String object instead of 3!
```

**Java automatically converts primitives to Strings during concatenation:**

```java
int n = 42;
String s = "Number: " + n;  // ✅ Java calls Integer.toString(n) internally
// More efficient than: "Number: " + Integer.valueOf(n).toString()
```

#### Example 2: String.format() - Better for Complex Formatting

**BookcaseCommands.java:41**

```java
return String.format(" %-12s \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mShelves    \u001B[1m\u001B[38;5;63m%-2d\u001B[22m\u001B[38;5;15mBooks ",
    bookcaseEntity.getBookcaseLabel().toUpperCase(),
    bookcaseEntity.getShelfCapacity(),
    bookCount);
```

**This is GOOD for complex formatting:**
- ✅ Readable format string
- ✅ Type-safe placeholders (`%s`, `%d`)
- ✅ Single String object created

**But for simple concatenation, `+` is faster:**

```java
// Simple concatenation - use +:
String label = "Shelf " + position;  // ✅ Faster

// Complex formatting - use String.format():
String message = String.format("Book %s (ID: %d) on shelf %s", title, id, shelfLabel);  // ✅ Readable
```

### String Interning

**Make a String go into the pool after creation:**

```java
String s1 = new String("Hello");  // Creates heap object (not pooled)
String s2 = s1.intern();  // ← Returns reference to pooled String

s2 == "Hello"  // ✅ true (same pooled object)
```

**Use case:** If you have many duplicate Strings from external sources (files, database), intern them to save memory.

**Caution:** Interning has overhead, only use for frequently repeated strings.

---

## Garbage Collection Basics

**Java automatically reclaims memory from unreachable objects.**

### When is an Object Eligible for GC?

**An object can be garbage collected when there are NO references to it.**

```java
public void example() {
    BookEntity book1 = new BookEntity();  // Object created
    BookEntity book2 = book1;  // TWO references to same object

    book1 = null;  // One reference removed (but book2 still references it)
    // ← Object NOT eligible for GC (book2 still references it)

    book2 = null;  // Last reference removed
    // ← Object NOW eligible for GC
}
```

### Example: Temporary Objects

```java
// BookcaseService.java:35-36
for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
    addShelf(bookcaseEntity, i, i);
}

public void addShelf(BookcaseEntity bookcaseEntity, int label, int position){
    ShelfEntity shelfEntity = new ShelfEntity();  // ← Created
    shelfEntity.setBookcaseId(bookcaseEntity.getBookcaseId());
    shelfEntity.setShelfLabel("Shelf " + label);
    shelfEntity.setShelfPosition(position);
    shelfRepository.save(shelfEntity);  // ← Persisted to DB
}  // ← Method ends, local `shelfEntity` reference destroyed
// BUT: Object is still reachable (managed by JPA/Hibernate)
```

**The `ShelfEntity` object is NOT garbage collected because:**
1. It's saved to the database (Hibernate keeps reference)
2. It's in the persistence context

### GC Process (Simplified)

```
1. Mark Phase:
   JVM walks through all reachable objects starting from "GC roots"
   (static variables, local variables on stack, active threads)

2. Sweep Phase:
   Unreachable objects are collected (memory reclaimed)

3. Compact Phase (optional):
   Rearrange objects to reduce fragmentation
```

**You don't need to manually trigger GC:**
- ❌ `System.gc()` - Just a suggestion, JVM may ignore
- ✅ Trust the JVM - It's very good at GC

---

## Memory-Efficient Patterns

### 1. Reuse Immutable Objects

**String literals are reused:**

```java
String status1 = "AVAILABLE";
String status2 = "AVAILABLE";
// Same object! (from String pool)

// Better than:
String status = new String("AVAILABLE");  // ❌ Creates unnecessary heap object
```

**Your `BookStatus` enum is perfect:**

```java
public enum BookStatus {
    AVAILABLE,     // ← Only ONE object per status
    CHECKED_OUT,
    RESERVED,
    LOST,
    ARCHIVED
}

BookEntity book = new BookEntity();
book.setBookStatus(BookStatus.AVAILABLE.name());  // ✅ Efficient
```

### 2. Avoid Unnecessary Boxing

**Current Issue in BookcaseService.java:46:**

```java
// BEFORE (creates unnecessary Integer object):
"Shelf " + Integer.valueOf(label).toString()
//         ↑ Boxes int → Integer (heap allocation)

// AFTER (more efficient):
"Shelf " + label  // ✅ Java handles conversion internally
```

**When boxing DOES make sense:**

```java
// JPA entities - nullable fields need wrapper types:
private Integer publicationYear;  // ✅ Can be null

// Collections - can't use primitives:
List<Integer> ids = new ArrayList<>();  // ✅ Must use Integer
```

### 3. Use StringBuilder for Loops

**Concatenation in loops creates many temporary Strings:**

```java
// ❌ BAD: Creates N intermediate String objects
String result = "";
for (BookEntity book : books) {
    result += book.getTitle() + ", ";  // Each += creates NEW String!
}

// ✅ GOOD: Single StringBuilder, grows as needed
StringBuilder sb = new StringBuilder();
for (BookEntity book : books) {
    sb.append(book.getTitle()).append(", ");
}
String result = sb.toString();
```

**Note:** For simple concatenation (not in a loop), `+` is fine - Java compiler optimizes it to `StringBuilder` automatically.

### 4. Immutability Reduces Memory Churn

**Records are immutable and efficient:**

```java
// Your BookSummary record:
public record BookSummary(Long bookId, String title) {}

BookSummary summary = new BookSummary(1L, "Harry Potter");
// ✅ Cannot be modified - safe to share
// ✅ No defensive copying needed
// ✅ Can be cached/pooled safely
```

**Immutable objects benefits:**
- ✅ Thread-safe (no synchronization needed)
- ✅ Safe to cache
- ✅ No defensive copies
- ✅ Easier to reason about

---

## Collections and Memory

### ArrayList Internal Array

```java
List<BookEntity> books = new ArrayList<>();  // Initial capacity: 10

books.add(book1);  // [book1, null, null, ..., null]  ← Wastes memory if you only have 1 book

// Better: Specify initial capacity if known
List<BookEntity> books = new ArrayList<>(100);  // Pre-allocates for 100
```

**When ArrayList grows:**

```java
List<BookEntity> books = new ArrayList<>(2);  // Initial capacity: 2
books.add(book1);  // [book1, null]
books.add(book2);  // [book1, book2] ← Full!
books.add(book3);  // ← Grows! Creates NEW array of size 3 (capacity * 1.5)
                   //   Copies [book1, book2] to new array
                   //   Old array eligible for GC
```

**Growth is expensive - specify capacity if you know the size:**

```java
int bookCount = bookRepository.count();
List<BookEntity> books = new ArrayList<>(bookCount);  // ✅ No resizing needed
```

### HashSet Memory

```java
Set<AuthorEntity> authors = new HashSet<>();
// Internal: HashMap with default capacity 16, load factor 0.75
// Grows when size > capacity * loadFactor
```

**Memory overhead:**
- Each entry in HashSet uses a `HashMap.Node` object
- Overhead ≈ 32 bytes per entry (on 64-bit JVM)

**Your `Set<AuthorEntity> authors` in `BookEntity`:**

```java
private Set<AuthorEntity> authors = new HashSet<>();
```

**For a book with 2 authors:**
- HashSet object: ~48 bytes
- 2 HashMap.Node objects: ~64 bytes
- 2 AuthorEntity references: (already counted in AuthorEntity objects)
- **Total overhead: ~112 bytes**

**This is fine!** Sets enforce uniqueness, which is more important than the memory overhead.

---

## Common Memory Issues to Avoid

### 1. Holding References to Large Objects

```java
// ❌ BAD: Holds reference to ENTIRE list forever
public class ReportService {
    private List<BookEntity> allBooks;  // ← Prevents GC even after report is done!

    public void generateReport() {
        allBooks = bookRepository.findAll();  // Loads ALL books
        // ... generate report ...
    }
}

// ✅ GOOD: Use local variable (eligible for GC after method returns)
public class ReportService {

    public void generateReport() {
        List<BookEntity> books = bookRepository.findAll();  // ← Local, GC'd after method
        // ... generate report ...
    }  // ← books reference destroyed
}
```

### 2. Not Closing Resources (Memory Leak!)

```java
// ❌ BAD: BufferedReader not closed (memory leak!)
public void importBooks(String filePath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filePath));
    String line = reader.readLine();
    // ... if exception thrown, reader is NEVER closed!
}

// ✅ GOOD: try-with-resources auto-closes
public void importBooks(String filePath) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
        String line = reader.readLine();
        // ...
    }  // ← Automatically closed, even if exception thrown
}
```

### 3. Creating Too Many Temporary Objects

**Your code is generally good, but could optimize:**

```java
// BookCommands.java - Multiple String concatenations in loop
for (BookEntity book : books) {
    System.out.println("Title: " + book.getTitle() + ", Author: " + book.getAuthors());
    // ↑ Creates multiple temporary String objects per iteration
}

// Better (if performance critical):
StringBuilder sb = new StringBuilder();
for (BookEntity book : books) {
    sb.setLength(0);  // Reset
    sb.append("Title: ").append(book.getTitle())
      .append(", Author: ").append(book.getAuthors());
    System.out.println(sb);
}
```

**Note:** For small loops, the difference is negligible. Only optimize if profiling shows it's a bottleneck.

---

## Testing Your Understanding

### Question 1: Stack vs Heap

Where are these stored?

```java
public void createBook() {
    int year = 1997;
    String title = "Harry Potter";
    BookEntity book = new BookEntity();
    book.setPublicationYear(year);
}
```

**Answer:**
- `year` (value 1997): **Stack** (primitive)
- `title` (reference): **Stack**, but points to String in **String pool (heap)**
- `book` (reference): **Stack**, but points to BookEntity object on **Heap**
- BookEntity object: **Heap**

### Question 2: Memory Efficiency

Which is more memory efficient?

```java
// Option A:
String label = "Shelf " + Integer.valueOf(position).toString();

// Option B:
String label = "Shelf " + position;
```

**Answer:** **Option B**
- Option A creates 3 objects (Integer, String from toString, final String)
- Option B creates 1 object (Java auto-converts int to String)

### Question 3: When is GC Eligible?

```java
BookEntity book1 = new BookEntity();
BookEntity book2 = book1;
book1 = null;
// Is the BookEntity object eligible for GC now?
```

**Answer:** **NO**
- `book2` still references the object
- Object is only GC-eligible when ALL references are removed

---

## Action Items for Your Codebase

### Priority 1: Fix Unnecessary Boxing

- [ ] **Fix inefficient String creation** (BookcaseService.java:46)
  ```java
  // Change:
  shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());

  // To:
  shelfEntity.setShelfLabel("Shelf " + label);
  ```

### Priority 2: Best Practices (Awareness)

- [ ] **Review String concatenation in loops** - Use StringBuilder if performance-critical
- [ ] **Use try-with-resources** for any future file I/O operations
- [ ] **Specify ArrayList capacity** when size is known

### Priority 3: Understanding (No Changes Needed)

- [ ] **Understand stack vs heap** - Primitives and references on stack, objects on heap
- [ ] **Know String pool** - Literals are pooled, `new String()` is not
- [ ] **Trust GC** - Don't call `System.gc()`, JVM handles it

---

## Key Concepts Recap

| Concept | What It Means | Example in Bibby |
|---------|---------------|------------------|
| **Stack** | Fast, thread-local memory for method calls | Local variables, parameters |
| **Heap** | Shared memory for objects | BookEntity, ArrayList, String |
| **Object Reference** | Arrow/pointer to heap object | `BookEntity book = ...` |
| **String Pool** | Special heap area for string literals | `"AVAILABLE"`, `"Shelf "` |
| **Garbage Collection** | Automatic memory reclamation | No manual memory management needed |
| **Immutability** | Object cannot be modified | Records, Strings, Enums |
| **Boxing** | Converting primitive to wrapper | `Integer.valueOf(5)` |
| **String Interning** | Adding String to pool | `s.intern()` |

---

## Summary

### What You Learned

1. ✅ **Stack vs Heap** - Where different data lives
2. ✅ **Object references** - Java passes reference values, not objects
3. ✅ **String pool** - Optimization for string literals
4. ✅ **Garbage collection** - Automatic, trust the JVM
5. ✅ **Unnecessary boxing** - Avoid `Integer.valueOf().toString()`
6. ✅ **Immutability** - Records and Strings are memory-efficient
7. ✅ **Collections memory** - ArrayList grows, HashSet has overhead
8. ✅ **Memory leaks** - Holding references, not closing resources

### Memory Issues in Bibby

❌ **Found:**
- Unnecessary boxing: `Integer.valueOf(label).toString()` (BookcaseService:46)

✅ **Good:**
- Uses records (immutable, efficient)
- Uses enums for constants (only one instance per value)
- No obvious memory leaks
- Generally efficient String usage

---

## What's Next?

In **Section 13: Lambda Expressions & Functional Interfaces**, we'll examine:
- Lambda syntax and when to use it
- Functional interfaces (Function, Predicate, Consumer)
- Method references (`::`operator)
- Refactoring loops to lambdas
- Replacing anonymous classes

**Your Progress:**
- ✅ Sections 1-12 Complete (36% of mentorship guide)
- ⏳ 21 sections remaining

---

**Section 12 Complete! 🎉**

**Key takeaway:** Java's automatic memory management is excellent - you just need to avoid unnecessary object creation and understand references vs objects.

**Ready for Section 13?** Reply **"yes"** or **"continue"**.
