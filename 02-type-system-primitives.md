# Section 2: Java Type System & Primitives

Welcome back! Now that you understand your Bibby codebase's architecture, let's dive into one of Java's most fundamental (and often misunderstood) concepts: **the type system** and the difference between primitives and objects.

This might seem basic, but I've seen production bugs that cost companies thousands of dollars because developers didn't fully understand when to use `int` vs `Integer`. Your code has some of these patterns - both good and problematic - so let's learn from them.

---

## What Are Primitives vs Objects?

Java has **two parallel type systems** that coexist:

**Primitives (8 types):**
- `byte`, `short`, `int`, `long` (integers)
- `float`, `double` (floating-point)
- `char` (character)
- `boolean` (true/false)

**Object Wrappers (Reference Types):**
- `Byte`, `Short`, `Integer`, `Long`
- `Float`, `Double`
- `Character`
- `Boolean`

**Key Differences:**

| Aspect | Primitive (int) | Wrapper (Integer) |
|--------|----------------|-------------------|
| **Can be null?** | ❌ No | ✅ Yes |
| **Default value** | 0 | null |
| **Memory** | 4 bytes (stack) | 16+ bytes (heap + reference) |
| **Performance** | Faster | Slower (object overhead) |
| **Use in Collections** | ❌ No (List<int> illegal) | ✅ Yes (List<Integer>) |
| **Methods** | None | equals(), hashCode(), parseInt(), etc. |
| **Identity** | By value | By reference (gotcha!) |

---

## Why This Matters: The Real-World Impact

**Scenario 1: The NullPointerException That Crashed Checkout**

Imagine your `checkoutCount` was `int` instead of `Integer`. A book that's never been checked out has `checkoutCount = 0`. But what if you need to distinguish between:
- "Never checked out" (null or not set)
- "Checked out zero times after being returned" (0)

With `int`, you can't represent "unknown" - it's always a number. This is why you (correctly!) used `Integer checkoutCount` in your BookEntity.

**Scenario 2: The Performance Hit from Boxing**

Every time you do `Integer count = 5`, Java creates an object on the heap. In a tight loop processing millions of records, this creates garbage collection pressure and slows your application.

**Scenario 3: The Equality Bug**

```java
Integer a = 200;
Integer b = 200;
System.out.println(a == b); // false! They're different objects!

Integer x = 100;
Integer y = 100;
System.out.println(x == y); // true! (cached by JVM)
```

Let's see how these issues appear in YOUR code.

---

## In Your Code: What You're Doing Well

### ✅ Using Integer for Nullable Database Fields

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 25

```java
private Integer checkoutCount;
```

**Why This Is Correct:**

`checkoutCount` represents how many times a book has been checked out. Consider these scenarios:

1. **Brand new book** - Never checked out yet → `null` makes sense
2. **Checked out once, returned** → `1`
3. **Never checked out** → Could be `0` or `null`

By using `Integer` (wrapper), you can distinguish between:
- `null` = "We don't know / not tracked"
- `0` = "We know it's never been checked out"

This maps perfectly to database columns that can be `NULL`. JPA will map:
- `NULL` in database → `null` in Java
- `0` in database → `Integer` object with value 0

**What You Learned:** When a field can legitimately be "unknown" or "not applicable," use the wrapper type.

---

### ✅ Using Long for JPA Entity IDs

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 14-16

```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long bookId;
```

📁 File: `src/main/java/com/penrose/bibby/library/shelf/ShelfEntity.java`
📍 Lines: 9-11

```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long shelfId;
```

**Why This Is The Standard:**

All your entity IDs use `Long` (not `long`). This is the **JPA best practice** because:

1. **Before Persistence:** A new entity has `id = null` (not yet assigned)
2. **After Persistence:** Database generates ID → `id = 1L, 2L, 3L...`
3. **Null Check:** You can test `if (entity.getId() == null)` to see if it's new

With primitive `long`, you couldn't distinguish "not yet persisted" from "ID is 0". Every JPA tutorial recommends wrapper types for IDs, and you followed this perfectly!

**Professional Pattern:** All major frameworks (Hibernate, Spring Data) expect `Long`/`Integer` for generated IDs.

---

## In Your Code: Where You Can Improve

### ⚠️ Mixing int and Integer Inconsistently

Let's look at your `BookEntity` field types:

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 16-28

```java
private Long bookId;           // Wrapper
private String title;          // Reference type
private String isbn;           // Reference type
private String publisher;      // Reference type
private int publicationYear;   // PRIMITIVE
private String genre;          // Reference type
private int edition;           // PRIMITIVE
private String description;    // Reference type
private Long shelfId;          // Wrapper
private Integer checkoutCount; // Wrapper
private String bookStatus;     // Reference type
private LocalDate createdAt;   // Reference type
private LocalDate updatedAt;   // Reference type
```

**The Problem:**

You have **three different patterns** for numeric fields:
1. `Long bookId` - Wrapper (correct for ID)
2. `Integer checkoutCount` - Wrapper (correct for nullable)
3. `int publicationYear` - **Primitive** (questionable!)
4. `int edition` - **Primitive** (questionable!)

**Why This Is Problematic:**

**Issue 1: Database NULL Handling**

Your database schema likely allows these to be NULL:
```sql
CREATE TABLE books (
    publication_year INT,  -- Can be NULL in database
    edition INT            -- Can be NULL in database
);
```

But your Java code uses `int`, which can't represent NULL:
- Database `NULL` → JPA maps to `0` in Java (silently!)
- You lose information about "unknown publication year"

**Issue 2: Required vs Optional Data**

Ask yourself these questions:
- **Publication Year:** Do ALL books in your library have a known publication year? What about ancient manuscripts or books where the year is unknown?
- **Edition:** Do ALL books have an edition number? What about first printings that don't specify "1st edition"?

If the answer is "not always," you should use `Integer` to allow `null`.

**Real-World Scenario:**

```java
// User tries to add a classic book with unknown publication year
BookEntity book = new BookEntity();
book.setTitle("Beowulf");
book.setPublicationYear(???); // What goes here?
// If you don't call setPublicationYear(), it defaults to 0
// Now your 8th-century epic has publicationYear = 0!
```

---

### ⚠️ The Refactored Version

```java
// BEFORE (Current Code)
private int publicationYear;
private int edition;

// AFTER (Improved)
private Integer publicationYear;  // Allows null for "unknown year"
private Integer edition;          // Allows null for "unspecified edition"
```

**Updated Constructor:**

```java
// BEFORE (Current Code)
public BookEntity() {
    // publicationYear defaults to 0 - misleading!
    // edition defaults to 0 - misleading!
}

// AFTER (Improved)
public BookEntity() {
    // publicationYear defaults to null - clear "not set"
    // edition defaults to null - clear "not specified"
}
```

**Updated Business Logic:**

```java
// BEFORE (Current Code)
public int getPublicationYear() {
    return publicationYear; // Always returns a number, even if unknown
}

// AFTER (Improved)
public Integer getPublicationYear() {
    return publicationYear; // Returns null if unknown
}

// Can add validation
public void setPublicationYear(Integer year) {
    if (year != null && (year < 0 || year > LocalDate.now().getYear())) {
        throw new IllegalArgumentException("Invalid publication year: " + year);
    }
    this.publicationYear = year;
}
```

**What Changed:**
- ✅ `null` now means "unknown" instead of fake value `0`
- ✅ Database NULL maps correctly to Java null
- ✅ Validation can distinguish between "not set" and "invalid"
- ✅ Consistent with `checkoutCount` pattern
- ⚠️ Slightly more memory per entity (negligible for most apps)

---

### ⚠️ Primitive in ShelfEntity

📁 File: `src/main/java/com/penrose/bibby/library/shelf/ShelfEntity.java`
📍 Lines: 14

```java
private int shelfPosition;
```

**Analysis:**

This one is actually **debatable** - it could be correct as-is or should be changed. Let's think through it:

**Keep as `int` if:**
- Every shelf MUST have a position (required field)
- Position 0 is a valid shelf position (bottom shelf)
- You never need to represent "position not assigned"

**Change to `Integer` if:**
- Shelves can be created without assigning position initially
- Position NULL in database means "not yet placed"
- You need to distinguish "not assigned" from "position 0"

**My Recommendation:** Change to `Integer` because:

In your `BookcaseService.addShelf()` method:

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java`
📍 Lines: 43-49

```java
public void addShelf(BookcaseEntity bookcaseEntity, int label, int position){
    ShelfEntity shelfEntity = new ShelfEntity();
    shelfEntity.setBookcaseId(bookcaseEntity.getBookcaseId());
    shelfEntity.setShelfLabel("Shelf " + Integer.valueOf(label).toString());
    shelfEntity.setShelfPosition(position); // Always set immediately
    shelfRepository.save(shelfEntity);
}
```

You're always setting position immediately, so `int` works here. But what if later you want to:
- Create a shelf first, assign position later
- Reorder shelves and temporarily unassign positions
- Have floating shelves with no fixed position

For **future flexibility**, `Integer` is safer.

**Refactored:**

```java
// ShelfEntity.java
private Integer shelfPosition; // Allows null for "position not assigned"

// BookcaseService.java - stays the same, works with Integer
shelfEntity.setShelfPosition(position); // Auto-boxing: int → Integer
```

---

### ⚠️ Primitive in BookcaseEntity

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseEntity.java`
📍 Lines: 12

```java
private int shelfCapacity;
```

**Analysis:**

Similar question: Can a bookcase have "unknown capacity"?

**Current Usage:**

📁 File: `src/main/java/com/penrose/bibby/library/bookcase/BookcaseService.java`
📍 Lines: 32, 35

```java
bookcaseEntity = new BookcaseEntity(label, capacity); // Capacity always provided
for(int i = 0; i < bookcaseEntity.getShelfCapacity(); i++){
    // Always has a value
}
```

Your constructor **requires** capacity, so `int` makes sense here. This is a good example of when primitive is appropriate:

- **Always Required:** You can't create a bookcase without specifying capacity
- **Never NULL:** No scenario where "unknown capacity" is valid
- **Used in Loops:** Primitive `int` is perfect for loop counters

**Verdict:** Keep as `int` - this is correct!

**Why It's Different From publicationYear:**
- Creating a bookcase without capacity → Doesn't make sense
- Creating a book without publication year → Could happen (antique books)

---

## The Autoboxing Trap

Java automatically converts between primitives and wrappers:

```java
int primitive = 5;
Integer wrapper = primitive;     // Auto-boxing: int → Integer
int backToPrimitive = wrapper;   // Auto-unboxing: Integer → int
```

This seems convenient, but creates subtle bugs. Let's find them in your code:

### 🐛 Bug Waiting to Happen: Unboxing Null

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 110-115

```java
public Integer getCheckoutCount() {
    return checkoutCount;
}
public void setCheckoutCount(Integer checkoutCount) {
    this.checkoutCount = checkoutCount;
}
```

**Scenario: The Hidden NullPointerException**

```java
BookEntity book = new BookEntity();
// checkoutCount is null (not set)

// Somewhere in your code...
int count = book.getCheckoutCount(); // BOOM! NullPointerException
// Trying to auto-unbox null → primitive fails
```

**Where This Could Happen in Your Code:**

📁 File: `src/main/java/com/penrose/bibby/library/book/BookMapper.java`
📍 Lines: 24

```java
book.setCheckoutCount(e.getCheckoutCount()); // What if this is null?
```

**Later Usage:**

```java
// In some hypothetical analytics code
int totalCheckouts = 0;
for (BookEntity book : books) {
    totalCheckouts += book.getCheckoutCount(); // NPE if any book has null!
}
```

**The Fix: Null-Safe Operations**

```java
// Option 1: Use wrapper type throughout
Integer totalCheckouts = 0;
for (BookEntity book : books) {
    if (book.getCheckoutCount() != null) {
        totalCheckouts += book.getCheckoutCount(); // Safe
    }
}

// Option 2: Default to zero
int totalCheckouts = 0;
for (BookEntity book : books) {
    totalCheckouts += (book.getCheckoutCount() != null ? book.getCheckoutCount() : 0);
}

// Option 3: Use Java 8+ (best!)
int totalCheckouts = books.stream()
    .map(BookEntity::getCheckoutCount)
    .filter(Objects::nonNull)  // Skip nulls
    .mapToInt(Integer::intValue)  // Safe unboxing
    .sum();
```

---

## Type Consistency Across Your Domain

Let's look at ID fields across all your entities:

**Consistent Pattern (Good!):**

```java
// BookEntity.java
private Long bookId;

// AuthorEntity.java
private Long authorId;

// ShelfEntity.java
private Long shelfId;

// BookcaseEntity.java
private Long bookcaseId;

// All use Long - perfect consistency!
```

**Inconsistent Pattern (Needs Attention):**

```java
// BookEntity.java - Foreign Key
private Long shelfId;  // Wrapper (can be null - good!)

// BookcaseEntity.java - Capacity
private int shelfCapacity;  // Primitive (can't be null - inconsistent?)

// ShelfEntity.java - Position
private int shelfPosition;  // Primitive (can't be null - inconsistent?)
```

**Question to Ask:** Should these be nullable?

**Foreign Keys (`shelfId` in BookEntity):** ✅ Correctly using `Long`
- Books might not be on a shelf yet → `null` is valid
- Consistent with JPA relationship patterns

**Capacities and Positions:** Mixed
- `shelfCapacity` - Required, keep as `int` ✅
- `shelfPosition` - Consider `Integer` for flexibility ⚠️

---

## Integer Caching: A Surprising Gotcha

Java caches `Integer` objects for values -128 to 127:

```java
Integer a = 100;
Integer b = 100;
System.out.println(a == b); // true (same cached object)

Integer x = 200;
Integer y = 200;
System.out.println(x == y); // false (different objects)
```

**Never use `==` with wrapper types - always use `.equals()`:**

```java
// WRONG
if (book.getCheckoutCount() == 5) { ... }

// CORRECT
if (book.getCheckoutCount() != null && book.getCheckoutCount().equals(5)) { ... }

// BETTER (Java 7+)
if (Objects.equals(book.getCheckoutCount(), 5)) { ... }
```

**Check Your Code:**

Search for patterns like:
```bash
grep -r "== [0-9]" src/  # Find == comparisons with numbers
```

I didn't find this anti-pattern in your code - good job! But now you know to avoid it.

---

## Performance Considerations

**When Primitives Matter:**

Imagine processing 1 million books:

```java
// Primitive: 4 MB (1M books × 4 bytes per int)
private int publicationYear;

// Wrapper: 16-24 MB (1M books × 16+ bytes per Integer object)
private Integer publicationYear;
```

**When To Use Each:**

**Use Primitive (`int`) when:**
- ✅ Field is ALWAYS required (never null)
- ✅ Performance critical (tight loops, millions of objects)
- ✅ Working with calculations/counters locally
- ✅ Used as array indices or loop variables

**Use Wrapper (`Integer`) when:**
- ✅ Field can be NULL in database
- ✅ Used in Collections (`List<Integer>`)
- ✅ Need to call methods (`parseInt()`, `compareTo()`)
- ✅ JPA entity fields that might be null
- ✅ Need to distinguish "not set" from "zero"

**Your Current Code Analysis:**

| Field | Current Type | Recommendation | Reasoning |
|-------|-------------|----------------|-----------|
| `bookId` | `Long` | ✅ Keep | Generated ID, must be nullable |
| `publicationYear` | `int` | ⚠️ Change to `Integer` | Unknown years should be null |
| `edition` | `int` | ⚠️ Change to `Integer` | Not all books specify edition |
| `shelfId` | `Long` | ✅ Keep | Foreign key, can be null |
| `checkoutCount` | `Integer` | ✅ Keep | Can be null for new books |
| `shelfPosition` | `int` | ⚠️ Consider `Integer` | Future flexibility |
| `shelfCapacity` | `int` | ✅ Keep | Always required |

---

## Common Pitfalls in Your Code

### Pitfall 1: Returning Primitive from Method That Could Be Null

📁 File: `src/main/java/com/penrose/bibby/library/book/BookEntity.java`
📍 Lines: 79-83

```java
public int getPublicationYear() {
    return publicationYear;
}
public void setPublicationYear(int publicationYear) {
    this.publicationYear = publicationYear;
}
```

**Problem:** If you change field to `Integer` but keep return type as `int`:

```java
private Integer publicationYear; // Now nullable

public int getPublicationYear() {
    return publicationYear; // NPE if publicationYear is null!
}
```

**Fix:** Match getter return type to field type:

```java
private Integer publicationYear;

public Integer getPublicationYear() {
    return publicationYear; // Returns null safely
}

public void setPublicationYear(Integer publicationYear) {
    this.publicationYear = publicationYear;
}
```

---

### Pitfall 2: Comparison Logic Breaks

If you have any code comparing years:

```java
// With primitive int - simple
if (book.getPublicationYear() > 2000) { ... }

// With Integer - needs null check
if (book.getPublicationYear() != null && book.getPublicationYear() > 2000) { ... }

// Better with Objects utility
if (Objects.requireNonNullElse(book.getPublicationYear(), 0) > 2000) { ... }
```

---

## Key Takeaways

**The Golden Rules:**

1. **JPA Entity IDs:** Always use `Long` or `Integer` (never `long`/`int`)
2. **Nullable Fields:** Use wrapper types (`Integer`, not `int`)
3. **Required Fields:** Primitives are acceptable if truly always present
4. **Comparisons:** Never use `==` with wrappers, always `.equals()`
5. **Null Checks:** Always check wrapper types before unboxing
6. **Collections:** Must use wrappers (`List<Integer>`, not `List<int>`)

**Decision Tree:**

```
Is this a JPA entity field?
├─ Yes: Is it an @Id or @GeneratedValue?
│   └─ Yes: Use Long/Integer (wrapper)
│   └─ No: Can it be NULL in the database?
│       ├─ Yes: Use wrapper (Integer)
│       └─ No: Is it always required in your business logic?
│           ├─ Yes: Primitive is OK (int)
│           └─ No: Use wrapper for flexibility (Integer)
└─ No (local variable): Is it used in collections or might be null?
    ├─ Yes: Use wrapper
    └─ No: Primitive is fine (performance)
```

---

## Practice Exercise: Refactor BookEntity

**Your Task:**

Refactor `BookEntity` to use appropriate types for all numeric fields.

**Step 1:** Analyze each field
```java
private int publicationYear;  // Should this be Integer?
private int edition;          // Should this be Integer?
private Integer checkoutCount; // Already correct!
```

**Step 2:** Decide using this checklist:
- [ ] Can this field be NULL in the database?
- [ ] Is "unknown" a valid state for this field?
- [ ] Do I need to distinguish between "not set" and "zero"?
- [ ] Is this field always required when creating a Book?

**Step 3:** Refactor the code
- Change field declarations
- Update getters/setters
- Add null safety checks where needed
- Update any comparison logic

**Step 4:** Test with different scenarios:
- Create a book without publication year
- Create a book with publication year
- Save to database and retrieve
- Verify NULL handling

**Solution:**

```java
// REFACTORED BookEntity
@Entity
@Table(name = "books")
public class BookEntity {
    // ... other fields ...

    // Changed from int to Integer
    private Integer publicationYear;  // Allows null for unknown years
    private Integer edition;          // Allows null for unspecified editions
    private Integer checkoutCount;    // Already correct!

    // ... constructors ...

    // Updated getters - now return Integer
    public Integer getPublicationYear() {
        return publicationYear;
    }

    public Integer getEdition() {
        return edition;
    }

    public Integer getCheckoutCount() {
        return checkoutCount;
    }

    // Setters - now accept Integer
    public void setPublicationYear(Integer year) {
        // Optional: Add validation
        if (year != null && year < 0) {
            throw new IllegalArgumentException("Year cannot be negative");
        }
        this.publicationYear = year;
    }

    public void setEdition(Integer edition) {
        if (edition != null && edition < 1) {
            throw new IllegalArgumentException("Edition must be at least 1");
        }
        this.edition = edition;
    }

    public void setCheckoutCount(Integer count) {
        if (count != null && count < 0) {
            throw new IllegalArgumentException("Checkout count cannot be negative");
        }
        this.checkoutCount = count;
    }
}
```

---

## Action Items for This Week

Based on Section 2 insights, complete these three tasks:

**1. Refactor BookEntity Numeric Fields**
**Priority:** HIGH
**Estimated Time:** 20 minutes
**File:** `src/main/java/com/penrose/bibby/library/book/BookEntity.java`

- Change `int publicationYear` → `Integer publicationYear`
- Change `int edition` → `Integer edition`
- Update all getters to return `Integer`
- Update all setters to accept `Integer`
- Add validation in setters (year > 0, edition > 0)

**2. Audit All Primitive vs Wrapper Usage**
**Priority:** MEDIUM
**Estimated Time:** 30 minutes

Create a document listing:
- All entity numeric fields
- Current type (int or Integer)
- Recommendation (keep or change)
- Reasoning

Files to check:
- `BookEntity.java`
- `AuthorEntity.java`
- `ShelfEntity.java`
- `BookcaseEntity.java`
- `CatalogEntity.java` (if it exists)

**3. Add Null Safety Checks**
**Priority:** HIGH
**Estimated Time:** 15 minutes
**Files:** `BookService.java`, `BookMapper.java`

Find any code that:
- Calls getters returning `Integer` and uses the value directly
- Auto-unboxes without null checks
- Compares wrapper types with `==`

Add appropriate null checks or use `Objects.requireNonNullElse()`.

---

## Further Study

**Deep Dives:**
- Java Language Specification: Primitive Types and Values (§4.2)
- Effective Java (3rd Ed) by Joshua Bloch: Item 6 "Avoid creating unnecessary objects"
- Effective Java: Item 61 "Prefer primitive types to boxed primitives"

**Articles:**
- Baeldung: "Java Autoboxing and Unboxing" - https://www.baeldung.com/java-autoboxing-unboxing
- Oracle: "Autoboxing and Unboxing" - Java Tutorials

**Performance Analysis:**
- "Java Performance: The Definitive Guide" by Scott Oaks - Chapter on Object Creation

---

## Summary

You've learned:
- ✅ The 8 primitive types and their wrapper classes
- ✅ When to use `int` vs `Integer` in JPA entities
- ✅ Why your `Long bookId` is correct
- ✅ Why your `Integer checkoutCount` is correct
- ⚠️ Why `int publicationYear` should be `Integer`
- ⚠️ The autoboxing trap and NullPointerException risks
- ✅ How to make null-safe comparisons
- ✅ The performance implications of each choice
- ✅ A decision tree for future type choices

**Your code showed:**
- **Strengths:** Correct ID types, appropriate nullable checkoutCount
- **Opportunities:** Inconsistent primitive usage, missing null safety

**Next Up:** Section 3 - Control Flow & Logic (examining if/else patterns, loops, and conditional logic in your BookService and BookCommands)

---

*Section created: 2025-11-17*
*Files analyzed: BookEntity, ShelfEntity, BookcaseEntity, BookService, BookMapper*
*Type issues identified: 4*
*Recommendations provided: 7*
